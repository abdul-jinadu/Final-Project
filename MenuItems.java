
public class MenuItems {
    private String name;
    private double price;
    private String category;

    // Constructor
    public MenuItems(String name, double price, String category) {
        this.name = name;
        this.price = price;
        this.category = category;
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
