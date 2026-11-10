public class Main {
    public static void main(String[] args) {

        MenuItems burger = new MenuItems("Burger", 8.99, "Food");
        MenuItems soda = new MenuItems("Soda", 2.50, "Drink");
        MenuItems fries = new MenuItems("Fries", 3.99, "Side");

        burger.displayItem();
        soda.displayItem();
        fries.displayItem();
    }
}
