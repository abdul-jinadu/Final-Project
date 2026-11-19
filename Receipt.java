import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Receipt {
    private static final String LINE = "========================================";
    private static final String DASHES = "----------------------------------------";

    private final int orderId;
    private final Customer customer;
    private final Order order;
    private final Payment payment;
    private final String orderDate;
    private final String restaurantName;
    private final boolean savedReceipt;
    private final boolean receiptDetails;

    public Receipt(int orderId, Customer customer, Order order, Payment payment) {
        this(orderId, customer, order, payment, "Restaurant", LocalDateTime.now().toString());
    }

    public Receipt(int orderId, Customer customer, Order order, Payment payment,
                   String restaurantName, String orderDate) {
        if (customer == null || order == null || payment == null) {
            throw new IllegalArgumentException("A receipt needs a customer, an order and a payment.");
        }
        this.orderId = orderId;
        this.customer = customer;
        this.order = order;
        this.payment = payment;
        this.orderDate = orderDate;
        this.restaurantName = restaurantName;
        this.savedReceipt = false;
        this.receiptDetails = true;
    }

    public Receipt(DatabaseManager.SavedOrder saved) {
        this.orderId = saved.getOrderId();
        this.customer = saved.getCustomer();
        this.order = saved.getOrder();
        this.payment = saved.getPayment();
        this.orderDate = saved.getOrderDate();
        this.restaurantName = saved.getRestaurantName();
        this.savedReceipt = true;
        this.receiptDetails = saved.hasReceiptDetails();
    }

    public void printReceipt() {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");

        System.out.println("\n" + LINE);
        System.out.println("              RECEIPT");
        System.out.println(LINE);
        System.out.println("Restaurant: " + restaurantName);
        System.out.println("Order #: " + orderId);
        System.out.println("Customer: " + customer.getName());
        System.out.println("Date: " + (receiptDetails ? LocalDateTime.parse(orderDate).format(format) : orderDate));
        System.out.println(DASHES);
        System.out.printf("%-4s %-20s %12s%n", "Qty", "Item", "Price");
        System.out.println(DASHES);

        // One line per item the customer ordered.
        for (Order.LineItem line : order.getItems()) {
            System.out.printf("%-4d %-20s %12s%n", line.getQuantity(),
                    line.getItem().getName(), money(line.getLineTotal()));
            if (line.getQuantity() > 1) {
                System.out.printf("     @ %s each%n", money(line.getItem().getPrice()));
            }
        }

        System.out.println(DASHES);
        printRow("Subtotal:", payment.getSubtotal());
        if (receiptDetails) {
            printRow("Tax (6%):", payment.getTax());
            printRow("Tip:", payment.getTip());
            printRow("TOTAL:", payment.getTotal());
        } else {
            System.out.println("Tax, tip, final paid total and payment");
            System.out.println("method were not recorded for this order.");
        }
        System.out.println(DASHES);

        if (!receiptDetails) {
            System.out.println("Historical item details are shown above.");
        } else if (savedReceipt) {
            System.out.println("Payment Method: " + payment.getPaymentMethod());
        } else if (payment.getPaymentMethod() == Payment.Method.CASH) {
            printRow("Paid (Cash):", payment.getAmountTendered());
            printRow("Change:", payment.getChange());
        } else {
            System.out.printf("%-26s %12s%n", "Paid (Card):", "**** " + payment.getCardLastFour());
        }

        System.out.println("Status: " + order.getStatus());
        System.out.println(LINE);
        System.out.println("     Thank you for dining with us!");
        System.out.println(LINE);
    }

    private void printRow(String label, double amount) {
        System.out.printf("%-26s %12s%n", label, money(amount));
    }

    private static String money(double amount) {
        return String.format("$%.2f", amount);
    }
}
