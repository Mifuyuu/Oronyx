package me.khaithomx.oronyx.bazaar;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Data class representing a potentially profitable item found in the Bazaar.
 */
public class ProfitableItem {

    private final String productId;
    private final String itemName;
    private final double buyPrice; // Highest price someone is buying for (our cost to buy instantly)
    private final double sellPrice; // Lowest price someone is selling for (our price to sell instantly)
    private final double profitPerItem; // Calculated profit after tax
    private final double profitPercentage; // Calculated profit percentage based on buy price
    private final long buyVolume; // Total volume available at buy orders (how many people are buying)
    private final long sellVolume; // Total volume available at sell offers (how many people are selling)
    // private double coinsPerHour; // Optional: Estimated CPH (requires complex calculation)

    // Number formatter for cleaner output
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    static {
        numberFormat.setMaximumFractionDigits(1);
    }


    public ProfitableItem(String productId, String itemName, double buyPrice, double sellPrice,
                          double profitPerItem, double profitPercentage, long buyVolume, long sellVolume) {
        this.productId = productId;
        this.itemName = itemName;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.profitPerItem = profitPerItem;
        this.profitPercentage = profitPercentage;
        this.buyVolume = buyVolume;
        this.sellVolume = sellVolume;
        // this.coinsPerHour = coinsPerHour;
    }

    // --- Getters ---

    public String getProductId() {
        return productId;
    }

    public String getItemName() {
        return itemName;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public double getProfitPerItem() {
        return profitPerItem;
    }

    public double getProfitPercentage() {
        // Handle potential NaN or Infinity if buyPrice is 0 or profit is extreme
        if (Double.isNaN(profitPercentage) || Double.isInfinite(profitPercentage)) {
            return 0.0; // Or some indicator value
        }
        return profitPercentage;
    }

    public long getBuyVolume() {
        return buyVolume;
    }

    public long getSellVolume() {
        return sellVolume;
    }

    /*
    public double getCoinsPerHour() {
        return coinsPerHour;
    }
    */

    @Override
    public String toString() {
        // Provides a simple string representation for debugging or simple lists
        return String.format("%s: Buy=%.1f, Sell=%.1f, Profit=%.1f (%.1f%%), Vol(B/S)=%d/%d",
                itemName, buyPrice, sellPrice, profitPerItem, getProfitPercentage(), buyVolume, sellVolume);
    }

    // Example method for formatted display string (can be used in GUI)
    public String getFormattedProfitString() {
        return String.format("%,.1f (%s%.1f%%%s)",
                profitPerItem,
                net.minecraft.util.EnumChatFormatting.GOLD, // Example color
                getProfitPercentage(),
                net.minecraft.util.EnumChatFormatting.RESET);
    }
    public String getFormattedPriceString() {
        return String.format("Buy: %s%,.1f%s Sell: %s%,.1f%s",
                net.minecraft.util.EnumChatFormatting.GREEN, buyPrice, net.minecraft.util.EnumChatFormatting.RESET,
                net.minecraft.util.EnumChatFormatting.RED, sellPrice, net.minecraft.util.EnumChatFormatting.RESET);
    }
    public String getFormattedVolumeString() {
        return String.format("B: %,d / S: %,d", buyVolume, sellVolume);
    }
}