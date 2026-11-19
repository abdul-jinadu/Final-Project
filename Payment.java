public class Payment {
    public enum Method {
        CASH, CARD
    }

    private static final double TAX_RATE = 0.06;

    private final double subtotal;
    private final double tax;
    private final double tip;
    private final double total;
    private final Method paymentMethod;

    private double amountTendered;
    private double change;
    private String cardLastFour;
    private boolean paid;

    public Payment(Order order, double tipPercentage, Method paymentMethod) {
        if (order == null || paymentMethod == null) {
            throw new IllegalArgumentException("An order and a payment method are required.");
        }
        if (!Double.isFinite(tipPercentage) || tipPercentage < 0) {
            throw new IllegalArgumentException("The tip cannot be negative.");
        }
        this.paymentMethod = paymentMethod;

        subtotal = roundToCents(order.getSubtotal());
        if (!Double.isFinite(subtotal) || !Double.isFinite(subtotal * (tipPercentage / 100))
                || subtotal * (tipPercentage / 100) > Long.MAX_VALUE / 100.0) {
            throw new IllegalArgumentException("The payment amount is too large.");
        }
        tax = roundToCents(subtotal * TAX_RATE);
        tip = roundToCents(subtotal * (tipPercentage / 100));
        total = roundToCents(subtotal + tax + tip);
    }

    // Saved receipts use the original amounts, without recalculating today's tax or tip.
    public Payment(double subtotal, double tax, double tip, double total, Method method) {
        this.subtotal = subtotal;
        this.tax = tax;
        this.tip = tip;
        this.total = total;
        this.paymentMethod = method;
        this.paid = true;
    }

    // Cash: returns true if the amount covers the total, and works out the change.
    public boolean payWithCash(double amount) {
        if (paymentMethod != Method.CASH) {
            throw new IllegalStateException("This payment is not set up for cash.");
        }
        if (!Double.isFinite(amount) || amount < 0 || amount > Long.MAX_VALUE / 100.0
                || roundToCents(amount) < total) {
            return false;
        }
        amountTendered = roundToCents(amount);
        change = roundToCents(amountTendered - total);
        paid = true;
        return true;
    }

    // Card: only the last 4 digits are kept, for the receipt.
    public boolean payWithCard(String lastFourDigits) {
        if (paymentMethod != Method.CARD) {
            throw new IllegalStateException("This payment is not set up for card.");
        }
        if (lastFourDigits == null || !lastFourDigits.trim().matches("\\d{4}")) {
            return false;
        }
        cardLastFour = lastFourDigits.trim();
        amountTendered = total;
        change = 0.0;
        paid = true;
        return true;
    }

    private static double roundToCents(double amount) {
        if (!Double.isFinite(amount) || amount > Long.MAX_VALUE / 100.0) {
            throw new IllegalArgumentException("The payment amount is too large.");
        }
        return Math.round(amount * 100) / 100.0;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getTax() {
        return tax;
    }

    public double getTip() {
        return tip;
    }

    public double getTotal() {
        return total;
    }

    public Method getPaymentMethod() {
        return paymentMethod;
    }

    public double getAmountTendered() {
        return amountTendered;
    }

    public double getChange() {
        return change;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public boolean isPaid() {
        return paid;
    }

    public void displayPayment() {
        System.out.printf("Tax: $%.2f%n", tax);
        System.out.printf("Tip: $%.2f%n", tip);
        System.out.printf("Total: $%.2f%n", total);
        System.out.println("Payment Method: " + paymentMethod);
        if (paid && paymentMethod == Method.CASH) {
            System.out.printf("Cash given: $%.2f%n", amountTendered);
            System.out.printf("Change: $%.2f%n", change);
        } else if (paid && paymentMethod == Method.CARD) {
            System.out.println("Card: **** **** **** " + cardLastFour);
        }
    }
}
