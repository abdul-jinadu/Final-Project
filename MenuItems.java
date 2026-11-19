public class MenuItems {
    private int itemId;
    private int restaurantId;
    private int stock;
    private boolean available = true;
    private String name;
    private double price;
    private String category;

    // Constructor
    public MenuItems(String name, double price, String category) {
        if (name == null || name.trim().isEmpty() || category == null || category.trim().isEmpty()
                || !Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("An item needs a name, category and nonnegative price.");
        }
        this.name = name;
        this.price = price;
        this.category = category;
    }

    // Keep the original constructor for existing code and historical order items.
    public MenuItems(int itemId, int restaurantId, String name, double price,
                     String category, int stock, boolean available) {
        this(name, price, category);
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }
        this.itemId = itemId;
        this.restaurantId = restaurantId;
        this.stock = stock;
        this.available = available;
    }

    public int getItemId() {
        return itemId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public int getStock() {
        return stock;
    }

    public boolean isAvailable() {
        return available;
    }

    // Here is where we get the name of food or drink
    public String getName() {
        return name;
    }

    // Here is where we get the price
    public double getPrice() {
        return price;
    }

    // Here is where we get the category of the item
    public String getCategory() {
        return category;
    }

    // Displays the menu items
    public void displayItem() {
        System.out.println(name + " - $" + price + " (" + category + ")");
    }
}
