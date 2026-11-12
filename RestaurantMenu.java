import java.util.Scanner;

public class RestaurantMenu {
    private final MenuItems[] items = {
        new MenuItems("Burger", 8.99, "Food"),
        new MenuItems("Pizza", 12.99, "Food"),
        new MenuItems("Chicken Sandwich", 9.99, "Food"),
        new MenuItems("Soda", 2.50, "Drink"),
        new MenuItems("Water", 1.00, "Drink"),
        new MenuItems("Juice", 2.50, "Drink"),
        new MenuItems("Fries", 4.99, "Side"),
        new MenuItems("Tater Tots", 3.99, "Side"),
        new MenuItems("Cheesecake", 6.99, "Dessert"),
        new MenuItems("Ice Cream", 3.99, "Dessert")

    };

    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\n=== Restaurant Ordering System ===");
                System.out.println("1. View Menu");
                System.out.println("2. Exit");
                System.out.print("Enter your choice: ");

                // Stop if there is no more input.
                if (!scanner.hasNextLine()) {
                    return;
                }

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        displayFoodMenu();
                        break;
                    case "2":
                        System.out.println("Thank you. Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter 1 or 2.");
                }
            }
        }
    }

    private void displayFoodMenu() {
        System.out.println("\n=== Food and Drinks ===");
        for (MenuItems item : items) {
            item.displayItem();
        }
    }
}
