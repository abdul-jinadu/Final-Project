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
        // Keep saved orders when the program starts again.
        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS orders (" +
                    "order_id INTEGER PRIMARY KEY, " +
                    "customer_name TEXT NOT NULL, " +
                    "total_price REAL NOT NULL, " +
                    "status TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS order_items (" +
                    "item_id INTEGER PRIMARY KEY, " +
                    "order_id INTEGER NOT NULL, " +
                    "item_name TEXT NOT NULL, " +
                    "category TEXT NOT NULL, " +
                    "quantity INTEGER NOT NULL CHECK (quantity > 0), " +
                    "item_price REAL NOT NULL, " +
                    "FOREIGN KEY (order_id) REFERENCES orders(order_id))");
        }
    }

    public int saveOrder(Order order, Customer customer) throws SQLException {
        if (order == null || order.getItems().isEmpty() || customer == null ||
                customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("An order needs items and a customer name.");
        }

        String orderSql = "INSERT INTO orders (customer_name, total_price, status) VALUES (?, ?, ?)";
        String itemSql = "INSERT INTO order_items " +
                "(order_id, item_name, category, quantity, item_price) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            // Commit only after the order and every item have been inserted.
            connection.setAutoCommit(false);
            try {
                int orderId;
                try (PreparedStatement orderStatement = connection.prepareStatement(orderSql);
                     PreparedStatement itemStatement = connection.prepareStatement(itemSql)) {
                    orderStatement.setString(1, customer.getName());
                    orderStatement.setDouble(2, order.getSubtotal());
                    orderStatement.setString(3, order.getStatus().name());
                    orderStatement.executeUpdate();

                    // Use this connection's new order ID for all of its items.
                    try (ResultSet results = statement.executeQuery("SELECT last_insert_rowid() AS order_id")) {
                        if (!results.next()) {
                            throw new SQLException("The new order ID could not be read.");
                        }
                        orderId = results.getInt("order_id");
                    }

                    for (Order.LineItem line : order.getItems()) {
                        MenuItems item = line.getItem();
                        itemStatement.setInt(1, orderId);
                        itemStatement.setString(2, item.getName());
                        itemStatement.setString(3, item.getCategory());
                        itemStatement.setInt(4, line.getQuantity());
                        itemStatement.setDouble(5, item.getPrice());
                        itemStatement.executeUpdate();
                    }
                }
                connection.commit();
                return orderId;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public ArrayList<SavedOrder> getOrders() throws SQLException {
        ArrayList<SavedOrder> savedOrders = new ArrayList<>();
        String orderSql = "SELECT order_id, customer_name, total_price, status FROM orders ORDER BY order_id";
        String itemSql = "SELECT item_name, category, quantity, item_price FROM order_items " +
                "WHERE order_id = ? ORDER BY item_id";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement orderStatement = connection.prepareStatement(orderSql);
             PreparedStatement itemStatement = connection.prepareStatement(itemSql);
             ResultSet orderResults = orderStatement.executeQuery()) {
            while (orderResults.next()) {
                int orderId = orderResults.getInt("order_id");
                Customer customer = new Customer(orderResults.getString("customer_name"));
                Order order = new Order();
                order.setStatus(Order.Status.valueOf(orderResults.getString("status")));

                // Rebuild each order using the existing menu item and line item classes.
                itemStatement.setInt(1, orderId);
                try (ResultSet itemResults = itemStatement.executeQuery()) {
                    while (itemResults.next()) {
                        MenuItems item = new MenuItems(itemResults.getString("item_name"),
                                itemResults.getDouble("item_price"), itemResults.getString("category"));
                        order.addItem(item, itemResults.getInt("quantity"));
                    }
                }
                savedOrders.add(new SavedOrder(orderId, customer, order,
                        orderResults.getDouble("total_price")));
            }
        }
        return savedOrders;
    }

    public void displayOrders(List<SavedOrder> savedOrders) {
        if (savedOrders.isEmpty()) {
            System.out.println("No saved orders yet.");
            return;
        }
        for (SavedOrder saved : savedOrders) {
            System.out.println("\n=== Order #" + saved.getOrderId() + " ===");
            saved.getCustomer().displayCustomer();
            System.out.println("Status: " + saved.getOrder().getStatus());
            for (Order.LineItem line : saved.getOrder().getItems()) {
                System.out.printf("%s | Quantity: %d | Unit price: $%.2f | Line total: $%.2f%n",
                        line.getItem().getName(), line.getQuantity(),
                        line.getItem().getPrice(), line.getLineTotal());
            }
            System.out.printf("Total: $%.2f%n", saved.getTotalPrice());
        }
    }

    // Holds database information alongside the existing Customer and Order objects.
    public static class SavedOrder {
        private final int orderId;
        private final Customer customer;
        private final Order order;
        private final double totalPrice;

        public SavedOrder(int orderId, Customer customer, Order order, double totalPrice) {
            this.orderId = orderId;
            this.customer = customer;
            this.order = order;
            this.totalPrice = totalPrice;
        }

        public int getOrderId() {
            return orderId;
        }

        public Customer getCustomer() {
            return customer;
        }

        public Order getOrder() {
            return order;
        }

        public double getTotalPrice() {
            return totalPrice;
        }
    }
}
