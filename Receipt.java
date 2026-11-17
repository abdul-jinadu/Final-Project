import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Receipt {
    private static final String LINE = "========================================";
    private static final String DASHES = "----------------------------------------";

    private final int orderId;
    private final Customer customer;
    private final Order order;
    private final Payment payment;
    private final LocalDateTime time;

    public Receipt(int orderId, Customer customer, Order order, Payment payment) {
        if (customer == null || order == null || payment == null) {
            throw new IllegalArgumentException("A receipt needs a customer, an order and a payment.");
        }
        this.orderId = orderId;
        this.customer = customer;
        this.order = order;
        this.payment = payment;
        this.time = LocalDateTime.now();
    }

    public void printReceipt() {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");

        System.out.println("\n" + LINE);
        System.out.println("              RECEIPT");
        System.out.println(LINE);
        System.out.println("Order #: " + orderId);
        System.out.println("Customer: " + customer.getName());
        System.out.println("Date: " + time.format(format));
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
        printRow("Tax (6%):", payment.getTax());
        printRow("Tip:", payment.getTip());
        printRow("TOTAL:", payment.getTotal());
        System.out.println(DASHES);

        if (payment.getPaymentMethod() == Payment.Method.CASH) {
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
