public class Payment {
    private double tax;
    private double tip;
    private double total;
    private String paymentMethod;

    public Payment(Order order, double tipPercentage, String paymentMethod) {
        double subtotal = order.getSubtotal();
        this.paymentMethod = paymentMethod;

        tax = subtotal * 0.06;
        tip = subtotal * (tipPercentage / 100);
        total = subtotal + tax + tip;
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

    public void displayPayment() {
        System.out.printf("Tax: $%.2f%n", tax);
        System.out.printf("Tip: $%.2f%n", tip);
        System.out.printf("Total: $%.2f%n", total);
        System.out.println("Payment Method: " + paymentMethod);
    }
    
}
