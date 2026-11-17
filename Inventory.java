import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Inventory {
    // Keeps menu items in the order they were added, each with how many are in stock.
    private final Map<MenuItems, Integer> stock = new LinkedHashMap<>();
    private final List<MenuItems> items = new ArrayList<>();

    // Adds a new item to the inventory with a starting stock amount.
    public void addItem(MenuItems item, int quantity) {
        if (item == null || quantity < 0) {
            throw new IllegalArgumentException("Choose a menu item and a stock amount of 0 or more.");
        }
        if (!stock.containsKey(item)) {
            items.add(item);
        }
        stock.put(item, quantity);
    }

    public List<MenuItems> getItems() {
        return Collections.unmodifiableList(items);
    }

    // Menu numbers start at 1, even though the list starts at index 0.
    public MenuItems getItem(int number) {
        if (number >= 1 && number <= items.size()) {
            return items.get(number - 1);
        }
        return null;
    }

    public int getStock(MenuItems item) {
        return stock.getOrDefault(item, 0);
    }

    public boolean isAvailable(MenuItems item, int quantity) {
        return getStock(item) >= quantity;
    }

    // Takes items out of stock after an order is paid for and saved.
    public void removeStock(MenuItems item, int quantity) {
        if (!isAvailable(item, quantity)) {
            throw new IllegalArgumentException("Not enough " + item.getName() + " in stock.");
        }
        stock.put(item, getStock(item) - quantity);
    }

    // Removes every line of an order from stock.
    public void removeStock(Order order) {
        // Check everything first so stock is never left half-updated.
        for (Order.LineItem line : order.getItems()) {
            if (!isAvailable(line.getItem(), order.getQuantityOf(line.getItem()))) {
                throw new IllegalArgumentException("Not enough " + line.getItem().getName() + " in stock.");
            }
        }
        for (Order.LineItem line : order.getItems()) {
            removeStock(line.getItem(), line.getQuantity());
        }
    }

    public void restock(MenuItems item, int quantity) {
        if (!stock.containsKey(item) || quantity < 1) {
            throw new IllegalArgumentException("Choose a menu item and a quantity of at least 1.");
        }
        stock.put(item, getStock(item) + quantity);
    }

    public void displayInventory() {
        System.out.println("\n========== INVENTORY ==========");
        for (int i = 0; i < items.size(); i++) {
            MenuItems item = items.get(i);
            int count = getStock(item);
            String note = count == 0 ? "  SOLD OUT" : (count <= 5 ? "  (low)" : "");
            System.out.printf("%2d. %-20s %-8s %3d%s%n", i + 1, item.getName(),
                    item.getCategory(), count, note);
        }
        System.out.println("===============================");
    }
}
