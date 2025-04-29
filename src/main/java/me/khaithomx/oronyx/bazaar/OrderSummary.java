package me.khaithomx.oronyx.bazaar;

/**
 * Data class representing a summary of orders at a specific price level
 * from the Bazaar API (buy_summary or sell_summary).
 */
public class OrderSummary {

    private final int amount;         // Total number of items at this price level
    private final double pricePerUnit; // The price per unit for this level
    private final int orders;         // The number of orders making up this price level

    public OrderSummary(int amount, double pricePerUnit, int orders) {
        this.amount = amount;
        this.pricePerUnit = pricePerUnit;
        this.orders = orders;
    }

    // --- Getters ---

    public int getAmount() {
        return amount;
    }

    public double getPricePerUnit() {
        return pricePerUnit;
    }

    public int getOrders() {
        return orders;
    }

    @Override
    public String toString() {
        return "OrderSummary{" +
                "amount=" + amount +
                ", pricePerUnit=" + pricePerUnit +
                ", orders=" + orders +
                '}';
    }
}