import java.util.ArrayList;

public class OrderManager {
    private final ArrayList<Order> orders = new ArrayList<>();

    // Order numbers starts at 1, even though the list starts at index 0.
    public int addOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("An order is required.");
        }
        orders.add(order);
        return orders.size();
    }

    public Order getOrder(int orderNumber) {
        if (orderNumber < 1 || orderNumber > orders.size()) {
            throw new IllegalArgumentException("Invalid order number.");
        }
        return orders.get(orderNumber -1 );
    }
    
    public void updateStatus(int orderNumber, Order.Status status) {
        getOrder(orderNumber).setStatus(status);
    }

    public void displayOrders() {
        if (orders.isEmpty()) {
            System.out.println("No orders yet.");
            return;
        }

        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            System.out.println("Order #" + (i + 1) + ": " + order.getStatus());
        }
    }
}