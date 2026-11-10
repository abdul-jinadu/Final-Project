import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Order {
    private enum Status {
        PENDING, PREPARING, COMPLETED
    }

    private final List<LineItem> items = new ArrayList<>();
    private Status status = Status.PENDING;

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

    public double getSubtotal() {
        double subtotal = 0.0;
        for (LineItem line : items) {
            subtotal += line.getLineTotal();
        }
        return subtotal;
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