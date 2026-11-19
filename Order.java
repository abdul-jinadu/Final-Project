import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    public enum Status {
        PENDING, PREPARING, COMPLETED
    }

    private final List<LineItem> items = new ArrayList<>();
    private Status status = Status.PENDING;
    private final int restaurantId;

    public Order() {
        this(0);
    }

    public Order(int restaurantId) {
        this.restaurantId = restaurantId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public void addItem(MenuItems item, int quantity) {
        if (item == null || quantity < 1) {
            throw new IllegalArgumentException(
                "Choose a menu item and a quantity of at least 1."
            );
        }
        items.add(new LineItem(item,quantity));
    }

    public List<LineItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // How many of one menu item are already in this order (used for stock checks).
    public int getQuantityOf(MenuItems item) {
        int total = 0;
        for (LineItem line : items) {
            if (line.getItem() == item || (item.getItemId() > 0
                    && line.getItem().getItemId() == item.getItemId()
                    && line.getItem().getRestaurantId() == item.getRestaurantId())) {
                total = Math.addExact(total, line.getQuantity());
            }
        }
        return total;
    }

    public double getSubtotal() {
        double subtotal = 0.0;
        for (LineItem line : items) {
            subtotal += line.getLineTotal();
        }
        if (!Double.isFinite(subtotal) || subtotal > Long.MAX_VALUE / 100.0) {
            throw new IllegalArgumentException("The order total is too large.");
        }
        return Math.round(subtotal * 100) / 100.0;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Choose an order status.");
        }
        this.status = status;
    }

    public static class LineItem {
        private final MenuItems item;
        private final int quantity;

        private LineItem(MenuItems item, int quantity) {
            this.item = item;
            this.quantity = quantity;
        }

        public MenuItems getItem() {
            return item;
        }

        public int getQuantity() {
            return quantity;
        }

        public double getLineTotal() {
            return item.getPrice() * quantity;
        }
    }
}
