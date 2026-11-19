import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Inventory {
    private final DatabaseManager database;
    private final int restaurantId;
    private List<MenuItems> items = new ArrayList<>();

    public Inventory(DatabaseManager database, int restaurantId) throws SQLException {
        this.database = database;
        this.restaurantId = restaurantId;
        items = database.getMenuItems(restaurantId);
    }

    // Reload before displaying or editing so the database stays the source of truth.
    public void refresh() throws SQLException {
        items = database.getMenuItems(restaurantId);
    }

    public void addItem(MenuItems item, int quantity) throws SQLException {
        if (item == null) {
            throw new IllegalArgumentException("Choose a menu item.");
        }
        database.addMenuItem(restaurantId, item.getName(), item.getCategory(), item.getPrice(),
                quantity, item.isAvailable());
        refresh();
    }

    public List<MenuItems> getItems() {
        return Collections.unmodifiableList(items);
    }

    // Menu numbers start at 1; database IDs do not have to match these numbers.
    public MenuItems getItem(int number) {
        if (number >= 1 && number <= items.size()) {
            return items.get(number - 1);
        }
        return null;
    }

    public int getStock(MenuItems item) {
        if (item != null && item.getRestaurantId() == restaurantId) {
            for (MenuItems current : items) {
                if (current.getItemId() == item.getItemId()) {
                    return current.getStock();
                }
            }
        }
        return 0;
    }

    public boolean isAvailable(MenuItems item, int quantity) {
        if (item == null || quantity < 1 || item.getRestaurantId() != restaurantId) {
            return false;
        }
        for (MenuItems current : items) {
            if (current.getItemId() == item.getItemId()) {
                return current.isAvailable() && current.getStock() >= quantity;
            }
        }
        return false;
    }

    // Checkout no longer calls removeStock separately: DatabaseManager.saveOrder
    // reduces stock within the SAME transaction as saving the order and receipt.
    public void restock(MenuItems item, int quantity) throws SQLException {
        if (item == null || item.getRestaurantId() != restaurantId || quantity < 1) {
            throw new IllegalArgumentException("Choose a menu item and a quantity of at least 1.");
        }
        if (!database.restockItem(restaurantId, item.getItemId(), quantity)) {
            throw new SQLException("Item not found in this restaurant, or the resulting stock is too large.");
        }
        refresh();
    }

    public void displayInventory() throws SQLException {
        refresh();
        System.out.println("\n========== INVENTORY ==========");
        for (int i = 0; i < items.size(); i++) {
            MenuItems item = items.get(i);
            int count = item.getStock();
            String note = !item.isAvailable() ? "  UNAVAILABLE"
                    : count == 0 ? "  SOLD OUT" : count <= 5 ? "  (low)" : "";
            System.out.printf("%2d. %-20s %-8s %3d%s%n", i + 1, item.getName(),
                    item.getCategory(), count, note);
        }
        System.out.println("===============================");
    }
}
