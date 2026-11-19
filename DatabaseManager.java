import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:sqlite:restaurant.db";

    public DatabaseManager() throws SQLException {
        initializeDatabase();
    }

    // SQLite requires this setting on EVERY connection, not just during setup.
    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(DATABASE_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
        return connection;
    }

    private void executeSql(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private boolean tableExists(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, name);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    // These table names are constants in this class, never terminal input.
    private ArrayList<String> getColumns(Connection connection, String table) throws SQLException {
        ArrayList<String> columns = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (results.next()) {
                columns.add(results.getString("name"));
            }
        }
        return columns;
    }

    private boolean hasColumns(ArrayList<String> columns, String... required) {
        for (String column : required) {
            if (!columns.contains(column)) {
                return false;
            }
        }
        return true;
    }

    private void initializeDatabase() throws SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                boolean ordersExist = tableExists(connection, "orders");
                boolean itemsExist = tableExists(connection, "order_items");
                if (ordersExist != itemsExist) {
                    throw new SQLException("Only one order table exists. No migration was performed; review the schema.");
                }
                boolean legacy = false;
                if (ordersExist) {
                    ArrayList<String> orders = getColumns(connection, "orders");
                    ArrayList<String> items = getColumns(connection, "order_items");
                    legacy = orders.size() == 4 && hasColumns(orders,
                            "order_id", "customer_name", "total_price", "status")
                            && items.size() == 6 && hasColumns(items,
                            "item_id", "order_id", "item_name", "category", "quantity", "item_price");
                    boolean current = hasColumns(orders, "order_id", "restaurant_id", "customer_name",
                            "subtotal", "tax", "tip", "final_total", "payment_method", "status",
                            "order_date", "receipt_details_available") && hasColumns(items,
                            "order_item_id", "order_id", "item_id", "item_name", "category", "quantity", "item_price");
                    if (!legacy && !current) {
                        throw new SQLException("Unrecognized order schema. Saved data was left unchanged; review it before migrating.");
                    }
                    if (legacy && (tableExists(connection, "legacy_orders")
                            || tableExists(connection, "legacy_order_items"))) {
                        throw new SQLException("Legacy archive tables already exist. Migration stopped to preserve them.");
                    }
                }
                executeSql(connection, "CREATE TABLE IF NOT EXISTS restaurants ("
                        + "restaurant_id INTEGER PRIMARY KEY, name TEXT NOT NULL UNIQUE)");
                executeSql(connection, "CREATE TABLE IF NOT EXISTS menu_items ("
                        + "item_id INTEGER PRIMARY KEY, restaurant_id INTEGER NOT NULL, "
                        + "name TEXT NOT NULL, category TEXT NOT NULL, "
                        + "price REAL NOT NULL CHECK (price >= 0), "
                        + "stock INTEGER NOT NULL CHECK (stock >= 0 AND stock <= 2147483647), "
                        + "available INTEGER NOT NULL DEFAULT 1 CHECK (available IN (0, 1)), "
                        + "UNIQUE (restaurant_id, name), "
                        + "FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id))");
                seedSamples(connection);
                if (legacy) {
                    // Keep the original tables and values as an archive; never drop them.
                    // SQLite updates the archived items' foreign key to legacy_orders.
                    executeSql(connection, "ALTER TABLE orders RENAME TO legacy_orders");
                    executeSql(connection, "ALTER TABLE order_items RENAME TO legacy_order_items");
                }
                createOrderTables(connection);
                if (legacy) {
                    migrateOrders(connection);
                }
                try (Statement statement = connection.createStatement();
                     ResultSet results = statement.executeQuery("PRAGMA foreign_key_check")) {
                    if (results.next()) {
                        throw new SQLException("A foreign key is invalid. Initialization was rolled back; review existing records.");
                    }
                }
                connection.commit();
                if (legacy) {
                    System.out.println("Existing orders were copied to Burger Palace; original tables are kept as legacy_orders and legacy_order_items.");
                    System.out.println("Older orders did not record payment details or dates. Their receipts will say so.");
                }
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private void createOrderTables(Connection connection) throws SQLException {
        executeSql(connection, "CREATE TABLE IF NOT EXISTS orders ("
                + "order_id INTEGER PRIMARY KEY, restaurant_id INTEGER NOT NULL, "
                + "customer_name TEXT NOT NULL, subtotal REAL NOT NULL CHECK (subtotal >= 0), "
                + "tax REAL NOT NULL CHECK (tax >= 0), tip REAL NOT NULL CHECK (tip >= 0), "
                + "final_total REAL NOT NULL CHECK (final_total >= 0), payment_method TEXT, "
                + "status TEXT NOT NULL CHECK (status IN ('PENDING', 'PREPARING', 'COMPLETED')), "
                + "order_date TEXT NOT NULL, "
                + "receipt_details_available INTEGER NOT NULL DEFAULT 1 CHECK (receipt_details_available IN (0, 1)), "
                + "FOREIGN KEY (restaurant_id) REFERENCES restaurants(restaurant_id))");
        executeSql(connection, "CREATE TABLE IF NOT EXISTS order_items ("
                + "order_item_id INTEGER PRIMARY KEY, order_id INTEGER NOT NULL, item_id INTEGER, "
                + "item_name TEXT NOT NULL, category TEXT NOT NULL, "
                + "quantity INTEGER NOT NULL CHECK (quantity > 0 AND quantity <= 2147483647), "
                + "item_price REAL NOT NULL CHECK (item_price >= 0), "
                + "FOREIGN KEY (order_id) REFERENCES orders(order_id), "
                + "FOREIGN KEY (item_id) REFERENCES menu_items(item_id))");
    }

    private void migrateOrders(Connection connection) throws SQLException {
        // Old total_price was a subtotal, not a recorded tax-inclusive payment.
        // Zero placeholders are marked incomplete, so receipts do not present them as known facts.
        String sql = "INSERT INTO orders (order_id, restaurant_id, customer_name, subtotal, tax, tip, "
                + "final_total, payment_method, status, order_date, receipt_details_available) "
                + "SELECT order_id, ?, customer_name, total_price, 0, 0, total_price, NULL, status, "
                + "'Unknown (legacy order)', 0 FROM legacy_orders";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, findRestaurantId(connection, "Burger Palace"));
            statement.executeUpdate();
        }
        // Old item_id identified a receipt line, not a menu item. Do not guess a menu link.
        executeSql(connection, "INSERT INTO order_items (order_item_id, order_id, item_id, "
                + "item_name, category, quantity, item_price) "
                + "SELECT item_id, order_id, NULL, item_name, category, quantity, item_price FROM legacy_order_items");
    }

    private void seedSamples(Connection connection) throws SQLException {
        seedRestaurant(connection, "Burger Palace");
        seedRestaurant(connection, "Pizza Corner");
        seedRestaurant(connection, "Island Grill");
        int burger = findRestaurantId(connection, "Burger Palace");
        int pizza = findRestaurantId(connection, "Pizza Corner");
        int island = findRestaurantId(connection, "Island Grill");
        seedItem(connection, burger, "Burger", "Food", 8.99, 20);
        seedItem(connection, burger, "Chicken Sandwich", "Food", 9.99, 15);
        seedItem(connection, burger, "Fries", "Side", 4.99, 30);
        seedItem(connection, burger, "Soda", "Drink", 2.50, 40);
        seedItem(connection, burger, "Water", "Drink", 1.00, 50);
        seedItem(connection, burger, "Cheesecake", "Dessert", 6.99, 10);
        seedItem(connection, pizza, "Cheese Pizza", "Food", 12.99, 15);
        seedItem(connection, pizza, "Pepperoni Pizza", "Food", 14.99, 12);
        seedItem(connection, pizza, "Garlic Bread", "Side", 4.50, 25);
        seedItem(connection, pizza, "Soda", "Drink", 2.25, 35);
        seedItem(connection, pizza, "Tiramisu", "Dessert", 5.99, 10);
        seedItem(connection, island, "Jerk Chicken", "Food", 13.50, 18);
        seedItem(connection, island, "Grilled Fish", "Food", 16.00, 12);
        seedItem(connection, island, "Rice and Peas", "Side", 4.00, 30);
        seedItem(connection, island, "Plantains", "Side", 3.50, 25);
        seedItem(connection, island, "Mango Juice", "Drink", 3.00, 35);
    }

    private void seedRestaurant(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO restaurants (name) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM restaurants WHERE name = ?)")) {
            statement.setString(1, name);
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    private int findRestaurantId(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT restaurant_id FROM restaurants WHERE name = ?")) {
            statement.setString(1, name);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    throw new SQLException("Restaurant not found: " + name);
                }
                return results.getInt("restaurant_id");
            }
        }
    }

    private void seedItem(Connection connection, int restaurantId, String name, String category,
                          double price, int stock) throws SQLException {
        String sql = "INSERT INTO menu_items (restaurant_id, name, category, price, stock) "
                + "SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS "
                + "(SELECT 1 FROM menu_items WHERE restaurant_id = ? AND name = ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, restaurantId);
            statement.setString(2, name);
            statement.setString(3, category);
            statement.setDouble(4, price);
            statement.setInt(5, stock);
            statement.setInt(6, restaurantId);
            statement.setString(7, name);
            statement.executeUpdate();
        }
    }

    public ArrayList<Restaurant> getRestaurants() throws SQLException {
        ArrayList<Restaurant> restaurants = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT restaurant_id, name FROM restaurants ORDER BY restaurant_id");
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                restaurants.add(new Restaurant(results.getInt("restaurant_id"), results.getString("name")));
            }
        }
        return restaurants;
    }

    public ArrayList<MenuItems> getMenuItems(int restaurantId) throws SQLException {
        ArrayList<MenuItems> items = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM menu_items WHERE restaurant_id = ? ORDER BY item_id")) {
            statement.setInt(1, restaurantId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    items.add(new MenuItems(results.getInt("item_id"), restaurantId, results.getString("name"),
                            results.getDouble("price"), results.getString("category"),
                            results.getInt("stock"), results.getInt("available") == 1));
                }
            }
        }
        return items;
    }

    public int addMenuItem(int restaurantId, String name, String category, double price, int stock,
                           boolean available) throws SQLException {
        validateMenuItem(name, category, price, stock);
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO menu_items (restaurant_id, name, category, price, stock, available) VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setInt(1, restaurantId);
            statement.setString(2, name.trim());
            statement.setString(3, category.trim());
            statement.setDouble(4, roundToCents(price));
            statement.setInt(5, stock);
            statement.setInt(6, available ? 1 : 0);
            statement.executeUpdate();
            return lastId(connection);
        }
    }

    public boolean updateMenuItem(int restaurantId, int itemId, double price, int stock,
                                  boolean available) throws SQLException {
        validateMenuItem("Item", "Category", price, stock);
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE menu_items SET price = ?, stock = ?, available = ? WHERE item_id = ? AND restaurant_id = ?")) {
            statement.setDouble(1, roundToCents(price));
            statement.setInt(2, stock);
            statement.setInt(3, available ? 1 : 0);
            statement.setInt(4, itemId);
            statement.setInt(5, restaurantId);
            return statement.executeUpdate() == 1;
        }
    }

    private void validateMenuItem(String name, String category, double price, int stock) {
        if (name == null || name.trim().isEmpty() || category == null || category.trim().isEmpty()
                || !Double.isFinite(price) || price < 0 || price > 1000000 || stock < 0) {
            throw new IllegalArgumentException("Enter a name, category, price from 0 to 1000000, and stock of 0 or more.");
        }
    }

    public boolean restockItem(int restaurantId, int itemId, int quantity) throws SQLException {
        if (quantity < 1) {
            throw new IllegalArgumentException("Restock quantity must be at least 1.");
        }
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE menu_items SET stock = stock + ? WHERE restaurant_id = ? AND item_id = ? AND stock <= 2147483647 - ?")) {
            statement.setInt(1, quantity);
            statement.setInt(2, restaurantId);
            statement.setInt(3, itemId);
            statement.setInt(4, quantity);
            return statement.executeUpdate() == 1;
        }
    }

    // This is the only checkout path: stock, order and receipt lines commit together.
    public int saveOrder(Order order, Customer customer, Payment payment, String orderDate) throws SQLException {
        if (order == null || order.getRestaurantId() < 1 || order.getItems().isEmpty()
                || customer == null || customer.getName() == null || customer.getName().trim().isEmpty()
                || payment == null || !payment.isPaid() || payment.getPaymentMethod() == null
                || orderDate == null || orderDate.trim().isEmpty()
                || !Double.isFinite(order.getSubtotal())
                || Math.abs(order.getSubtotal() - payment.getSubtotal()) > 0.001) {
            throw new IllegalArgumentException("A paid order needs a restaurant, items, customer, date and matching payment.");
        }
        String stockSql = "UPDATE menu_items SET stock = stock - ? "
                + "WHERE item_id = ? AND restaurant_id = ? AND stock >= ? AND available = 1 "
                + "AND price = ? AND name = ? AND category = ?";
        String orderSql = "INSERT INTO orders (restaurant_id, customer_name, subtotal, tax, tip, "
                + "final_total, payment_method, status, order_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String itemSql = "INSERT INTO order_items (order_id, item_id, item_name, category, quantity, item_price) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                // The conditional UPDATE checks current stock and obtains SQLite's write lock.
                // Repeated lines are checked against stock left by earlier lines in this transaction.
                try (PreparedStatement statement = connection.prepareStatement(stockSql)) {
                    for (Order.LineItem line : order.getItems()) {
                        MenuItems item = line.getItem();
                        if (item.getItemId() < 1 || item.getRestaurantId() != order.getRestaurantId()) {
                            throw new SQLException("Every item must belong to the selected restaurant.");
                        }
                        statement.setInt(1, line.getQuantity());
                        statement.setInt(2, item.getItemId());
                        statement.setInt(3, order.getRestaurantId());
                        statement.setInt(4, line.getQuantity());
                        statement.setDouble(5, item.getPrice());
                        statement.setString(6, item.getName());
                        statement.setString(7, item.getCategory());
                        if (statement.executeUpdate() != 1) {
                            throw new SQLException("Stock, availability or menu details changed for " + item.getName()
                                    + ". Cancel this order and select items again from the current menu.");
                        }
                    }
                }
                int orderId;
                try (PreparedStatement statement = connection.prepareStatement(orderSql)) {
                    statement.setInt(1, order.getRestaurantId());
                    statement.setString(2, customer.getName().trim());
                    statement.setDouble(3, payment.getSubtotal());
                    statement.setDouble(4, payment.getTax());
                    statement.setDouble(5, payment.getTip());
                    statement.setDouble(6, payment.getTotal());
                    statement.setString(7, payment.getPaymentMethod().name());
                    statement.setString(8, order.getStatus().name());
                    statement.setString(9, orderDate);
                    statement.executeUpdate();
                    orderId = lastId(connection);
                }
                try (PreparedStatement statement = connection.prepareStatement(itemSql)) {
                    for (Order.LineItem line : order.getItems()) {
                        statement.setInt(1, orderId);
                        statement.setInt(2, line.getItem().getItemId());
                        statement.setString(3, line.getItem().getName());
                        statement.setString(4, line.getItem().getCategory());
                        statement.setInt(5, line.getQuantity());
                        statement.setDouble(6, line.getItem().getPrice());
                        statement.executeUpdate();
                    }
                }
                connection.commit();
                return orderId;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private int lastId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT last_insert_rowid()")) {
            if (!results.next()) {
                throw new SQLException("The new record ID could not be read.");
            }
            return results.getInt(1);
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException exception) {
            original.addSuppressed(exception);
        }
    }

    private double roundToCents(double amount) {
        return Math.round(amount * 100) / 100.0;
    }

    public ArrayList<SavedOrder> getOrders(int restaurantId) throws SQLException {
        ArrayList<SavedOrder> savedOrders = new ArrayList<>();
        String orderSql = "SELECT o.*, r.name AS restaurant_name FROM orders o "
                + "JOIN restaurants r ON o.restaurant_id = r.restaurant_id "
                + "WHERE o.restaurant_id = ? ORDER BY o.order_id";
        String itemSql = "SELECT * FROM order_items WHERE order_id = ? ORDER BY order_item_id";
        try (Connection connection = connect();
             PreparedStatement orderStatement = connection.prepareStatement(orderSql);
             PreparedStatement itemStatement = connection.prepareStatement(itemSql)) {
            orderStatement.setInt(1, restaurantId);
            try (ResultSet orders = orderStatement.executeQuery()) {
                while (orders.next()) {
                    int orderId = orders.getInt("order_id");
                    Order order = new Order(restaurantId);
                    order.setStatus(Order.Status.valueOf(orders.getString("status")));
                    itemStatement.setInt(1, orderId);
                    try (ResultSet items = itemStatement.executeQuery()) {
                        while (items.next()) {
                            // Use saved names and prices, never today's menu prices.
                            MenuItems item = new MenuItems(items.getInt("item_id"), restaurantId,
                                    items.getString("item_name"), items.getDouble("item_price"),
                                    items.getString("category"), 0, true);
                            order.addItem(item, items.getInt("quantity"));
                        }
                    }
                    String method = orders.getString("payment_method");
                    Payment payment = new Payment(orders.getDouble("subtotal"), orders.getDouble("tax"),
                            orders.getDouble("tip"), orders.getDouble("final_total"),
                            method == null ? null : Payment.Method.valueOf(method));
                    savedOrders.add(new SavedOrder(orderId, new Customer(orders.getString("customer_name")),
                            order, payment, orders.getString("restaurant_name"), orders.getString("order_date"),
                            orders.getInt("receipt_details_available") == 1));
                }
            }
        }
        return savedOrders;
    }

    public ArrayList<SavedOrder> getOrdersByStatus(int restaurantId, Order.Status... statuses) throws SQLException {
        ArrayList<SavedOrder> matching = new ArrayList<>();
        for (SavedOrder saved : getOrders(restaurantId)) {
            for (Order.Status status : statuses) {
                if (saved.getOrder().getStatus() == status) {
                    matching.add(saved);
                    break;
                }
            }
        }
        return matching;
    }

    public boolean updateStatus(int restaurantId, int orderId, Order.Status status) throws SQLException {
        if (status == null) {
            throw new IllegalArgumentException("Choose an order status.");
        }
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE orders SET status = ? WHERE order_id = ? AND restaurant_id = ?")) {
            statement.setString(1, status.name());
            statement.setInt(2, orderId);
            statement.setInt(3, restaurantId);
            return statement.executeUpdate() == 1;
        }
    }

    public void displayOrders(List<SavedOrder> savedOrders) {
        if (savedOrders.isEmpty()) {
            System.out.println("No saved orders yet.");
        }
        for (SavedOrder saved : savedOrders) {
            System.out.println("\n=== " + saved.getRestaurantName() + " - Order #" + saved.getOrderId() + " ===");
            saved.getCustomer().displayCustomer();
            System.out.println("Status: " + saved.getOrder().getStatus());
            System.out.println("Date: " + saved.getOrderDate());
            for (Order.LineItem line : saved.getOrder().getItems()) {
                System.out.printf("%s | Quantity: %d | Unit price: $%.2f | Line total: $%.2f%n",
                        line.getItem().getName(), line.getQuantity(), line.getItem().getPrice(), line.getLineTotal());
            }
            System.out.printf(saved.hasReceiptDetails() ? "Total: $%.2f%n" : "Recorded subtotal: $%.2f (payment details unavailable)%n",
                    saved.getTotalPrice());
        }
    }

    // Receipt columns alongside the existing Customer, Order and Payment classes.
    public static class SavedOrder {
        private final int orderId;
        private final Customer customer;
        private final Order order;
        private final Payment payment;
        private final String restaurantName;
        private final String orderDate;
        private final boolean receiptDetails;

        public SavedOrder(int orderId, Customer customer, Order order, Payment payment,
                          String restaurantName, String orderDate, boolean receiptDetails) {
            this.orderId = orderId;
            this.customer = customer;
            this.order = order;
            this.payment = payment;
            this.restaurantName = restaurantName;
            this.orderDate = orderDate;
            this.receiptDetails = receiptDetails;
        }

        public int getOrderId() { return orderId; }
        public Customer getCustomer() { return customer; }
        public Order getOrder() { return order; }
        public Payment getPayment() { return payment; }
        public double getTotalPrice() { return payment.getTotal(); }
        public String getRestaurantName() { return restaurantName; }
        public String getOrderDate() { return orderDate; }
        public boolean hasReceiptDetails() { return receiptDetails; }
    }
}