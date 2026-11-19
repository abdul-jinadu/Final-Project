import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class RestaurantMenu {
    private DatabaseManager database;
    private Inventory inventory;
    private Restaurant restaurant;

    public void start() {
        try {
            database = new DatabaseManager();
        } catch (SQLException | RuntimeException exception) {
            System.out.println("Could not initialize restaurant.db: " + exception.getMessage());
            return;
        }
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n=== MAIN MENU ===");
                System.out.println("1. Select Restaurant");
                System.out.println("2. Exit");
                System.out.print("Enter your choice: ");
                String choice = readLine(scanner);
                if (choice == null || choice.equals("2") || choice.equals("0")) {
                    System.out.println("Thank you. Goodbye!");
                    return;
                }
                if (choice.equals("1")) {
                    try {
                        selectRestaurant(scanner);
                    } catch (SQLException exception) {
                        System.out.println("Could not load restaurants: " + exception.getMessage());
                    }
                } else {
                    System.out.println("Please enter 1 or 2.");
                }
            }
        }
    }

    private void selectRestaurant(Scanner scanner) throws SQLException {
        while (true) {
            List<Restaurant> restaurants = database.getRestaurants();
            System.out.println("\n=== SELECT RESTAURANT ===");
            for (int i = 0; i < restaurants.size(); i++) {
                System.out.println((i + 1) + ". " + restaurants.get(i).getName());
            }
            System.out.println("0. Back");
            System.out.print("Enter your choice: ");
            String input = readLine(scanner);
            if (input == null || input.equals("0")) {
                return;
            }
            try {
                int choice = Integer.parseInt(input);
                if (choice < 1 || choice > restaurants.size()) {
                    System.out.println("Choose a listed restaurant.");
                    continue;
                }
                restaurant = restaurants.get(choice - 1);
                inventory = new Inventory(database, restaurant.getRestaurantId());
                restaurantMenu(scanner);
            } catch (NumberFormatException exception) {
                System.out.println("Enter a restaurant number.");
            }
        }
    }

    private void restaurantMenu(Scanner scanner) {
        while (true) {
            System.out.println("\n=== " + restaurant.getName() + " ===");
            System.out.println("1. View Menu");
            System.out.println("2. Place Order");
            System.out.println("3. View Active Orders (Pending / Preparing)");
            System.out.println("4. View Completed Orders");
            System.out.println("5. Update Order Status");
            System.out.println("6. View Inventory");
            System.out.println("7. Restock an Item");
            System.out.println("8. View All Saved Orders");
            System.out.println("9. View Previous Receipts");
            System.out.println("10. Add Menu Item");
            System.out.println("11. Update Menu Price / Stock / Availability");
            System.out.println("0. Back to Restaurants");
            System.out.print("Enter your choice: ");

            String choice = readLine(scanner);
            // Stop if there is no more input.
            if (choice == null) {
                return;
            }

            try {
                switch (choice) {
                case "1":
                    displayMenu();
                    break;
                case "2":
                    placeOrder(scanner);
                    break;
                case "3":
                    viewOrders("Active", Order.Status.PENDING, Order.Status.PREPARING);
                    break;
                case "4":
                    viewOrders("Completed", Order.Status.COMPLETED);
                    break;
                case "5":
                    updateOrderStatus(scanner);
                    break;
                case "6":
                    inventory.displayInventory();
                    break;
                case "7":
                    restockItem(scanner);
                    break;
                case "8":
                    viewOrders("All", Order.Status.values());
                    break;
                case "0":
                    return;
                case "9":
                    viewReceipts(scanner);
                    break;
                case "10":
                    addMenuItem(scanner);
                    break;
                case "11":
                    editMenuItem(scanner);
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number from 0 to 11.");
                }
            } catch (SQLException exception) {
                System.out.println("Database operation failed: " + exception.getMessage());
            } catch (IllegalArgumentException exception) {
                System.out.println(exception.getMessage());
            }
        }
    }

    public void displayMenu() throws SQLException {
        inventory.refresh();
        System.out.println("\n============ " + restaurant.getName() + " MENU ============");
        List<MenuItems> items = inventory.getItems();
        for (int i = 0; i < items.size(); i++) {
            MenuItems item = items.get(i);
            int stock = inventory.getStock(item);
            String availability = !item.isAvailable() ? "UNAVAILABLE" : stock == 0 ? "SOLD OUT" : stock + " left";
            System.out.printf("%2d. %-20s %-8s $%6.2f  %s%n", i + 1, item.getName(),
                    item.getCategory(), item.getPrice(), availability);
        }
        System.out.println("=========================================");
    }

    public MenuItems getItem(int number) {
        return inventory.getItem(number);
    }

    private void placeOrder(Scanner scanner) throws SQLException {
        System.out.print("Customer name: ");
        String name = readLine(scanner);
        if (name == null) {
            return;
        }
        if (name.isEmpty()) {
            System.out.println("Please enter a customer name.");
            return;
        }
        Customer customer = new Customer(name);
        Order order = new Order(restaurant.getRestaurantId());

        while (true) {
            displayMenu();
            System.out.println(" 0. Checkout and Pay");
            System.out.println("-1. Cancel Order");
            System.out.printf("Current subtotal: $%.2f%n", order.getSubtotal());
            System.out.print("Choose an item: ");
            String input = readLine(scanner);
            if (input == null) {
                return;
            }
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a menu number.");
                continue;
            }
            if (choice == -1) {
                System.out.println("Order cancelled.");
                return;
            }
            if (choice == 0) {
                if (order.getItems().isEmpty()) {
                    System.out.println("Add at least one item before checking out.");
                    continue;
                }
                if (checkout(scanner, customer, order)) {
                    return;
                }
                // Checkout was not finished, so let the customer keep editing the order.
                continue;
            }
            MenuItems item = getItem(choice);
            if (item == null) {
                System.out.println("Choose one of the listed menu numbers.");
                continue;
            }
            System.out.print("Quantity: ");
            String quantityInput = readLine(scanner);
            if (quantityInput == null) {
                return;
            }
            try {
                int quantity = Integer.parseInt(quantityInput);
                // Count what is already in this order so it can't go over the stock.
                int alreadyOrdered = order.getQuantityOf(item);
                int requested = Math.addExact(alreadyOrdered, quantity);
                if (quantity >= 1 && !inventory.isAvailable(item, requested)) {
                    int left = inventory.getStock(item) - alreadyOrdered;
                    System.out.printf("Item unavailable, or only %d %s left for this order.%n", Math.max(left, 0), item.getName());
                    continue;
                }
                order.addItem(item, quantity);
                System.out.println("Item added.");
            } catch (IllegalArgumentException | ArithmeticException exception) {
                System.out.println("Please enter a whole-number quantity of at least 1.");
            }
        }
    }

    // Takes payment, saves the order, updates stock and prints the receipt.
    // Returns true when the order is finished, or false to go back to the order.
    private boolean checkout(Scanner scanner, Customer customer, Order order) {
        System.out.println("\n--- Review your order at " + restaurant.getName() + " ---");
        for (Order.LineItem line : order.getItems()) {
            System.out.printf("%d x %s @ $%.2f = $%.2f%n", line.getQuantity(),
                    line.getItem().getName(), line.getItem().getPrice(), line.getLineTotal());
        }
        System.out.printf("%nSubtotal: $%.2f%n", order.getSubtotal());

        Double tipPercentage = readTip(scanner);
        if (tipPercentage == null) {
            return false;
        }

        Payment.Method method = readPaymentMethod(scanner);
        if (method == null) {
            return false;
        }

        Payment payment;
        try {
            payment = new Payment(order, tipPercentage, method);
        } catch (IllegalArgumentException exception) {
            System.out.println(exception.getMessage());
            return false;
        }
        System.out.printf("Amount due: $%.2f%n", payment.getTotal());

        boolean paid = method == Payment.Method.CASH
                ? collectCash(scanner, payment)
                : collectCard(scanner, payment);
        if (!paid) {
            System.out.println("Payment not completed. Returning to your order.");
            return false;
        }

        String orderDate = LocalDateTime.now().toString();
        int orderId;
        try {
            orderId = database.saveOrder(order, customer, payment, orderDate);
        } catch (SQLException | IllegalArgumentException exception) {
            System.out.println("Could not save the order: " + exception.getMessage());
            System.out.println("Your selections are still here. Choose 0 to retry or -1 to cancel.");
            return false;
        }
        System.out.printf("Order #%d saved. Status: %s%n", orderId, order.getStatus());
        new Receipt(orderId, customer, order, payment, restaurant.getName(), orderDate).printReceipt();
        return true;
    }

    private Double readTip(Scanner scanner) {
        while (true) {
            System.out.print("Tip percentage (for example 15, or press Enter for no tip): ");
            String input = readLine(scanner);
            if (input == null) {
                return null;
            }
            if (input.isEmpty()) {
                return 0.0;
            }
            try {
                double tip = Double.parseDouble(input.replace("%", ""));
                if (Double.isFinite(tip) && tip >= 0) {
                    return tip;
                }
            } catch (NumberFormatException exception) {
                // Falls through to the message below.
            }
            System.out.println("Please enter a tip of 0 or more.");
        }
    }

    private Payment.Method readPaymentMethod(Scanner scanner) {
        while (true) {
            System.out.println("Payment method:");
            System.out.println("1. Cash");
            System.out.println("2. Card");
            System.out.println("0. Back to order");
            System.out.print("Enter your choice: ");
            String input = readLine(scanner);
            if (input == null || input.equals("0")) {
                return null;
            }
            if (input.equals("1")) {
                return Payment.Method.CASH;
            }
            if (input.equals("2")) {
                return Payment.Method.CARD;
            }
            System.out.println("Please enter 1, 2, or 0.");
        }
    }

    private boolean collectCash(Scanner scanner, Payment payment) {
        while (true) {
            System.out.print("Cash given (or press Enter to go back): $");
            String input = readLine(scanner);
            if (input == null || input.isEmpty()) {
                return false;
            }
            try {
                double amount = Double.parseDouble(input.replace("$", ""));
                if (payment.payWithCash(amount)) {
                    System.out.printf("Change due: $%.2f%n", payment.getChange());
                    return true;
                }
                System.out.printf("That is not enough. The total is $%.2f.%n", payment.getTotal());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter an amount such as 20 or 20.00.");
            }
        }
    }

    private boolean collectCard(Scanner scanner, Payment payment) {
        while (true) {
            System.out.print("Last 4 digits of the card (or press Enter to go back): ");
            String input = readLine(scanner);
            if (input == null || input.isEmpty()) {
                return false;
            }
            if (payment.payWithCard(input)) {
                System.out.println("Card approved.");
                return true;
            }
            System.out.println("Please enter exactly 4 digits.");
        }
    }

    private void viewOrders(String label, Order.Status... statuses) {
        try {
            List<DatabaseManager.SavedOrder> orders = database.getOrdersByStatus(restaurant.getRestaurantId(), statuses);
            System.out.println("\n--- " + label + " Orders (" + orders.size() + ") ---");
            database.displayOrders(orders);
        } catch (SQLException exception) {
            System.out.println("Could not retrieve saved orders: " + exception.getMessage());
        }
    }

    private void updateOrderStatus(Scanner scanner) {
        viewOrders("Active", Order.Status.PENDING, Order.Status.PREPARING);
        System.out.print("\nOrder number to update (or press Enter to go back): ");
        String input = readLine(scanner);
        if (input == null || input.isEmpty()) {
            return;
        }
        int orderId;
        try {
            orderId = Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            System.out.println("Please enter an order number.");
            return;
        }

        Order.Status[] statuses = Order.Status.values();
        System.out.println("New status:");
        for (int i = 0; i < statuses.length; i++) {
            System.out.println((i + 1) + ". " + statuses[i]);
        }
        System.out.print("Enter your choice: ");
        String statusInput = readLine(scanner);
        if (statusInput == null) {
            return;
        }
        int statusChoice;
        try {
            statusChoice = Integer.parseInt(statusInput);
        } catch (NumberFormatException exception) {
            statusChoice = 0;
        }
        if (statusChoice < 1 || statusChoice > statuses.length) {
            System.out.println("Please choose one of the listed statuses.");
            return;
        }

        try {
            if (database.updateStatus(restaurant.getRestaurantId(), orderId, statuses[statusChoice - 1])) {
                System.out.println("Order #" + orderId + " is now " + statuses[statusChoice - 1] + ".");
            } else {
                System.out.println("No order #" + orderId + " was found.");
            }
        } catch (SQLException exception) {
            System.out.println("Could not update the order: " + exception.getMessage());
        }
    }

    private void restockItem(Scanner scanner) throws SQLException {
        inventory.displayInventory();
        System.out.print("Item number to restock (or press Enter to go back): ");
        String input = readLine(scanner);
        if (input == null || input.isEmpty()) {
            return;
        }
        try {
            MenuItems item = inventory.getItem(Integer.parseInt(input));
            if (item == null) {
                System.out.println("Choose one of the listed item numbers.");
                return;
            }
            System.out.print("How many to add: ");
            String amount = readLine(scanner);
            if (amount == null) {
                return;
            }
            inventory.restock(item, Integer.parseInt(amount));
            System.out.println(item.getName() + " now has " + inventory.getStock(item) + " in stock.");
        } catch (IllegalArgumentException exception) {
            System.out.println("Please enter a whole number of at least 1.");
        }
    }

    private void viewReceipts(Scanner scanner) throws SQLException {
        List<DatabaseManager.SavedOrder> orders = database.getOrders(restaurant.getRestaurantId());
        if (orders.isEmpty()) {
            System.out.println("No saved receipts for " + restaurant.getName() + ".");
            return;
        }
        for (DatabaseManager.SavedOrder saved : orders) {
            System.out.println("Order #" + saved.getOrderId() + " | " + saved.getCustomer().getName()
                    + " | " + saved.getOrderDate() + " | " + saved.getOrder().getStatus());
        }
        System.out.print("Order ID to print (or press Enter to go back): ");
        String input = readLine(scanner);
        if (input == null || input.isEmpty()) {
            return;
        }
        try {
            int orderId = Integer.parseInt(input);
            for (DatabaseManager.SavedOrder saved : orders) {
                if (saved.getOrderId() == orderId) {
                    new Receipt(saved).printReceipt();
                    return;
                }
            }
            System.out.println("That order does not belong to this restaurant.");
        } catch (NumberFormatException exception) {
            System.out.println("Please enter a listed order ID.");
        }
    }

    private void addMenuItem(Scanner scanner) throws SQLException {
        System.out.print("New item name (or press Enter to cancel): ");
        String name = readLine(scanner);
        if (name == null || name.isEmpty()) {
            return;
        }
        System.out.print("Category: ");
        String category = readLine(scanner);
        if (category == null) {
            return;
        }
        System.out.print("Price: $");
        String price = readLine(scanner);
        if (price == null) {
            return;
        }
        System.out.print("Starting stock: ");
        String stock = readLine(scanner);
        if (stock == null) {
            return;
        }
        Boolean available = readAvailability(scanner);
        if (available == null) {
            return;
        }
        try {
            int itemId = database.addMenuItem(restaurant.getRestaurantId(), name, category,
                    Double.parseDouble(price), Integer.parseInt(stock), available);
            System.out.println("Menu item #" + itemId + " saved for " + restaurant.getName() + ".");
        } catch (NumberFormatException exception) {
            System.out.println("Enter a numeric price and a whole-number stock quantity.");
        }
    }

    private void editMenuItem(Scanner scanner) throws SQLException {
        inventory.displayInventory();
        System.out.print("Item number to update (or press Enter to cancel): ");
        String input = readLine(scanner);
        if (input == null || input.isEmpty()) {
            return;
        }
        try {
            MenuItems item = inventory.getItem(Integer.parseInt(input));
            if (item == null) {
                System.out.println("Choose a listed item number.");
                return;
            }
            System.out.printf("New price (currently $%.2f): $", item.getPrice());
            String price = readLine(scanner);
            if (price == null) {
                return;
            }
            System.out.print("New total stock (replaces " + item.getStock() + "): ");
            String stock = readLine(scanner);
            if (stock == null) {
                return;
            }
            Boolean available = readAvailability(scanner);
            if (available == null) {
                return;
            }
            if (database.updateMenuItem(restaurant.getRestaurantId(), item.getItemId(),
                    Double.parseDouble(price), Integer.parseInt(stock), available)) {
                System.out.println("Menu item updated. Saved receipts keep their original prices.");
            } else {
                System.out.println("The item was not found in this restaurant.");
            }
        } catch (NumberFormatException exception) {
            System.out.println("Enter a numeric price and whole-number item/stock quantities.");
        }
    }

    private Boolean readAvailability(Scanner scanner) {
        while (true) {
            System.out.print("Available for ordering? (1 = yes, 0 = no, Enter = cancel): ");
            String input = readLine(scanner);
            if (input == null || input.isEmpty()) {
                return null;
            }
            if (input.equals("1")) {
                return true;
            }
            if (input.equals("0")) {
                return false;
            }
            System.out.println("Please enter 1 or 0.");
        }
    }

    // Reads one trimmed line, or returns null if there is no more input.
    private String readLine(Scanner scanner) {
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }
}
