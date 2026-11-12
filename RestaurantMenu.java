import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class RestaurantMenu {
    private final ArrayList<MenuItems> menuItems = new ArrayList<>();

    public RestaurantMenu() {
        menuItems.add(new MenuItems("Burger", 8.99, "Food"));
        menuItems.add(new MenuItems("Pizza", 12.99, "Food"));
        menuItems.add(new MenuItems("Chicken Sandwich", 9.99, "Food"));
        menuItems.add(new MenuItems("Soda", 2.50, "Drink"));
        menuItems.add(new MenuItems("Water", 1.00, "Drink"));
        menuItems.add(new MenuItems("Juice", 2.50, "Drink"));
        menuItems.add(new MenuItems("Fries", 4.99, "Side"));
        menuItems.add(new MenuItems("Tater Tots", 3.99, "Side"));
        menuItems.add(new MenuItems("Cheesecake", 6.99, "Dessert"));
        menuItems.add(new MenuItems("Ice Cream", 3.99, "Dessert"));
        menuItems.add(new MenuItems("Lemonade", 2.99, "Drink"));
    }

    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n=== Restaurant Ordering System ===");
                System.out.println("1. View Menu");
                System.out.println("2. Exit");
                System.out.println("3. Place Order");
                System.out.println("4. View Saved Orders");
                System.out.print("Enter your choice: ");

                // Stop if there is no more input.
                if (!scanner.hasNextLine()) {
                    return;
                }

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        displayMenu();
                        break;
                    case "2":
                        System.out.println("Thank you. Goodbye!");
                        return;
                    case "3":
                        placeOrder(scanner);
                        break;
                    case "4":
                        viewSavedOrders();
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter 1, 2, 3, or 4.");
                }
            }
        }
    }

    public void displayMenu() {
        System.out.println("\n===== RESTAURANT MENU =====");
        for (int i = 0; i < menuItems.size(); i++) {
            MenuItems item = menuItems.get(i);
            System.out.printf("%d. %-20s $%.2f%n", i + 1, item.getName(), item.getPrice());
        }
        System.out.println("===========================");
    }

    public MenuItems getItem(int number) {
        if (number >= 1 && number <= menuItems.size()) {
            return menuItems.get(number - 1);
        }
        return null;
    }

    private void placeOrder(Scanner scanner) {
        System.out.print("Customer name: ");
        if (!scanner.hasNextLine()) {
            return;
        }
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Please enter a customer name.");
            return;
        }
        Customer customer = new Customer(name);
        Order order = new Order();

        while (true) {
            displayMenu();
            System.out.println("0. Save Order");
            System.out.println("-1. Cancel Order");
            System.out.printf("Current total: $%.2f%n", order.getSubtotal());
            System.out.print("Choose an item: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            int choice;
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
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
                    System.out.println("Add at least one item before saving.");
                    continue;
                }
                try {
                    DatabaseManager database = new DatabaseManager();
                    int orderId = database.saveOrder(order, customer);
                    System.out.printf("Order #%d saved. Total: $%.2f. Status: %s%n",
                            orderId, order.getSubtotal(), order.getStatus());
                    return;
                } catch (SQLException exception) {
                    System.out.println("Could not save the order: " + exception.getMessage());
                    System.out.println("Your selections are still here. Choose 0 to retry or -1 to cancel.");
                }
                continue;
            }
            MenuItems item = getItem(choice);
            if (item == null) {
                System.out.println("Choose one of the listed menu numbers.");
                continue;
            }
            System.out.print("Quantity: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            try {
                int quantity = Integer.parseInt(scanner.nextLine().trim());
                order.addItem(item, quantity);
                System.out.println("Item added.");
            } catch (IllegalArgumentException exception) {
                System.out.println("Please enter a whole-number quantity of at least 1.");
            }
        }
    }

    private void viewSavedOrders() {
        try {
            DatabaseManager database = new DatabaseManager();
            database.displayOrders(database.getOrders());
        } catch (SQLException exception) {
            System.out.println("Could not retrieve saved orders: " + exception.getMessage());
        }
    }
}
