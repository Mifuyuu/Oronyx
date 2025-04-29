package me.khaithomx.oronyx.bazaar;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.khaithomx.oronyx.Oronyx; // Needed for static config fields and Logger
import me.khaithomx.oronyx.util.ChatUtils;
import me.khaithomx.oronyx.util.NumberUtils; // For formatting numbers
import me.khaithomx.oronyx.util.ScoreboardReader;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.util.*;

/**
 * Processes bazaar data to find profitable items based on user configuration.
 * Reads configuration values directly from static fields in the Oronyx class.
 * Includes special handling for generating enchantment names and uses PriorityQueue for optimization.
 */
public class BazaarProcessor {

    // --- Static map for Roman numeral conversion ---
    private static final Map<Integer, String> ROMAN_MAP = new HashMap<>();
    static {
        ROMAN_MAP.put(1, "I"); ROMAN_MAP.put(2, "II"); ROMAN_MAP.put(3, "III");
        ROMAN_MAP.put(4, "IV"); ROMAN_MAP.put(5, "V"); ROMAN_MAP.put(6, "VI");
        ROMAN_MAP.put(7, "VII"); ROMAN_MAP.put(8, "VIII"); ROMAN_MAP.put(9, "IX");
        ROMAN_MAP.put(10, "X");
    }
    // --- End Roman numeral map ---


    // --- Base Comparator (Natural Order) for PriorityQueue (Min-Heap) ---
    // This comparator defines which element is "smaller" (lower profit/percentage)
    private static Comparator<ProfitableItem> getBaseComparator(String sortBy) {
        if (sortBy == null) sortBy = "profit";
        switch (sortBy.trim().toLowerCase()) {
            case "profitpercentage":
                // Lower percentage comes first. Handle non-positive buy price.
                return Comparator.<ProfitableItem, Double>comparing(
                        item -> item.getBuyPrice() <= 0 ? Double.NEGATIVE_INFINITY : item.getProfitPercentage()
                );
            case "profit":
            default:
                // Lower profit comes first.
                return Comparator.comparingDouble(ProfitableItem::getProfitPerItem);
        }
    }
    // --- ---

    /**
     * Finds profitable items using static config values from Oronyx class, optimized with early exits and PriorityQueue.
     *
     * @param bazaarData    The JsonObject containing bazaar product data from the API.
     * @param itemNamesMap  A map of item IDs to their display names.
     * @param modName       The name of the mod (used for chat message prefix).
     * @return A List of ProfitableItem objects meeting criteria, sorted (best first) and limited.
     */
    public List<ProfitableItem> findProfitableItemsStatic(JsonObject bazaarData, Map<String, String> itemNamesMap, String modName) {

        // --- Initial Checks & Setup ---
        if (!Oronyx.modEnabled) return Collections.emptyList(); // Use Collections.emptyList() for empty returns
        if (bazaarData == null || !bazaarData.has("products") || !bazaarData.get("products").isJsonObject()) return Collections.emptyList();
        if (itemNamesMap == null) return Collections.emptyList();

        JsonObject products = bazaarData.getAsJsonObject("products");
        long currentPurse = ScoreboardReader.getPlayerPurse();
        Oronyx.LOGGER.debug("Starting Bazaar Scan. Current Purse: {}. FilterByMaxPurse: {}. DisplayLimit: {}",
                (currentPurse == -1 ? "Not Found" : String.format("%,d", currentPurse)),
                Oronyx.filterByMaxPlayerPurse,
                Oronyx.displayLimit);

        // --- Overall Purse Check ---
        if (Oronyx.minPurse > 0 && currentPurse != -1 && currentPurse < Oronyx.minPurse) {
            /* Log + return */ return Collections.emptyList();}

        int itemsProcessed = 0;
        int itemsPassedInitialFilters = 0;
        int itemsPassedAllFilters = 0;

        // --- Setup Result Collection (PriorityQueue or List) ---
        PriorityQueue<ProfitableItem> topItemsQueue = null; // For limited results
        List<ProfitableItem> unlimitedResults = null;      // For unlimited results
        Comparator<ProfitableItem> baseComparator = getBaseComparator(Oronyx.sortBy); // Get comparator for queue/sorting

        if (Oronyx.displayLimit > 0) {
            // Use PriorityQueue (Min-Heap) with the base comparator
            topItemsQueue = new PriorityQueue<>(Oronyx.displayLimit + 1, baseComparator);
        } else {
            // No limit, use a standard list
            unlimitedResults = new ArrayList<>();
        }
        // --- ---

        // --- Process Each Product ---
        for (Map.Entry<String, JsonElement> entry : products.entrySet()) {
            itemsProcessed++;
            String productId = entry.getKey();
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject productData = entry.getValue().getAsJsonObject();
            if (!productData.has("buy_summary") || !productData.has("sell_summary") ||
                    !productData.get("buy_summary").isJsonArray() || !productData.get("sell_summary").isJsonArray()) continue;

            List<OrderSummary> buySummary = parseOrderSummary(productData.getAsJsonArray("buy_summary"));
            List<OrderSummary> sellSummary = parseOrderSummary(productData.getAsJsonArray("sell_summary"));
            if (buySummary.isEmpty() || sellSummary.isEmpty()) continue;

            // --- Determine Prices & Volume ---
            double ourBuyPrice = sellSummary.get(0).getPricePerUnit();
            double ourSellPrice = buySummary.get(0).getPricePerUnit();
            long totalBuyVolume = getTotalVolume(buySummary);
            long totalSellVolume = getTotalVolume(sellSummary);

            // --- **Early Filtering Stage 1 (Before Profit Calc)** ---
            if (ourSellPrice <= ourBuyPrice) continue; // Basic profitability
            if ((Oronyx.minBuyVolume > 0 && totalBuyVolume < Oronyx.minBuyVolume) || (Oronyx.minSellVolume > 0 && totalSellVolume < Oronyx.minSellVolume)) continue; // Volume
            if ((Oronyx.minPricePerUnitBuy > 0 && ourBuyPrice < Oronyx.minPricePerUnitBuy) || (Oronyx.maxPricePerUnitBuy > 0 && ourBuyPrice > Oronyx.maxPricePerUnitBuy)) continue; // Buy Price Range
            if (Oronyx.maxPricePerUnitSell > 0 && ourSellPrice > Oronyx.maxPricePerUnitSell) continue; // Sell Price Max
            if (currentPurse != -1) {
                if (Oronyx.filterByMaxPlayerPurse && ourBuyPrice > currentPurse) continue; // Max Player Purse
                if (Oronyx.maxSpentPerOrder > 0 && ourBuyPrice > Oronyx.maxSpentPerOrder) continue; // Basic Max Spend (single item cost)
            }
            // --- End Early Filtering Stage 1 ---

            itemsPassedInitialFilters++;

            // --- Calculate Profit & Percentage ---
            double taxRate = Math.max(0.0, Oronyx.tax - 1.0);
            double receivedSellPrice = ourSellPrice * (1.0 - taxRate);
            double potentialProfitPerItem = receivedSellPrice - ourBuyPrice;
            if (potentialProfitPerItem <= 0) continue; // Check profit after tax
            double potentialProfitPercentage = (ourBuyPrice <= 0) ? Double.POSITIVE_INFINITY : (potentialProfitPerItem / ourBuyPrice) * 100.0;

            // --- **Early Filtering Stage 2 (Profit Based)** ---
            if ((Oronyx.minProfit > 0 && potentialProfitPerItem < Oronyx.minProfit) || (Oronyx.maxProfit > 0 && potentialProfitPerItem > Oronyx.maxProfit)) continue; // Profit Range
            if (Oronyx.minProfitPercentage > 0 && potentialProfitPercentage < Oronyx.minProfitPercentage) continue; // Min Profit Percentage
            // Advanced Max Spend Check (can we afford min volume within max spend?)
            if (currentPurse != -1 && Oronyx.maxSpentPerOrder > 0 && ourBuyPrice > 0) {
                long affordableQuantity = Math.min(
                        (long)(Oronyx.maxSpentPerOrder / ourBuyPrice), // Max by spend limit
                        (long)(currentPurse / ourBuyPrice)           // Max by current purse
                );
                if (affordableQuantity < 1) continue; // Should have been caught earlier, but double check
                // Optional: if (Oronyx.minBuyVolume > 0 && affordableQuantity < Oronyx.minBuyVolume) continue;
            }
            // --- End Early Filtering Stage 2 ---


            // --- Item passed ALL filters ---
            itemsPassedAllFilters++;

            // --- Determine Item Name (Only for passed items) ---
            String itemName;
            String generatedEnchantName = generateEnchantmentName(productId);
            itemName = (generatedEnchantName != null) ? generatedEnchantName : itemNamesMap.getOrDefault(productId, productId + " (Unknown Name)");

            Oronyx.LOGGER.trace("Item '{}' passed all filters.", itemName);

            // --- Create ProfitableItem Object ---
            ProfitableItem item = new ProfitableItem(
                    productId, itemName, ourBuyPrice, ourSellPrice,
                    potentialProfitPerItem, potentialProfitPercentage,
                    totalBuyVolume, totalSellVolume
            );

            // --- Add to appropriate collection (Queue or List) ---
            if (topItemsQueue != null) { // Using PriorityQueue (displayLimit > 0)
                topItemsQueue.offer(item); // Add item to the min-heap
                // If the queue size is now greater than the limit, remove the smallest element
                if (topItemsQueue.size() > Oronyx.displayLimit) {
                    topItemsQueue.poll(); // Removes the head (element with lowest profit/percentage)
                }
            } else { // Using ArrayList (displayLimit <= 0)
                unlimitedResults.add(item);
            }
            // --- No chat message sending inside the loop ---

        } // End loop through products

        Oronyx.LOGGER.debug("Processed {} product entries. {} items passed initial filters, {} items passed all filters.", itemsProcessed, itemsPassedInitialFilters, itemsPassedAllFilters);

        // --- Finalize results list ---
        List<ProfitableItem> finalItemList;
        if (topItemsQueue != null) { // Results came from PriorityQueue
            // Convert queue to a list. Order will be arbitrary at this point.
            finalItemList = new LinkedList<>(topItemsQueue);
            // Sort the final list based on the desired order (best first)
            // Use reversed() on the base comparator used for the min-heap.
            finalItemList.sort(baseComparator.reversed());
            Oronyx.LOGGER.info("Final list size (from PriorityQueue, Limit: {}): {}", Oronyx.displayLimit, finalItemList.size());
        } else { // Results came from the unlimited list
            // Sort the full list based on the desired order (best first)
            unlimitedResults.sort(baseComparator.reversed());
            finalItemList = unlimitedResults;
            Oronyx.LOGGER.info("Final list size (unlimited): {}", finalItemList.size());
        }

        // --- Send Chat Messages (Iterate over the final sorted and limited list) ---
        if (Oronyx.showEvaluatingMessages) {
            Oronyx.LOGGER.debug("Sending {} evaluating messages to chat.", finalItemList.size());
            for (ProfitableItem profitableItem : finalItemList) {
                // Get data from the final item object
                String itemName = profitableItem.getItemName();
                double ourBuyPrice = profitableItem.getBuyPrice();
                double ourSellPrice = profitableItem.getSellPrice();
                long totalBuyVolume = profitableItem.getBuyVolume();
                long totalSellVolume = profitableItem.getSellVolume();
                double potentialProfitPerItem = profitableItem.getProfitPerItem();
                double potentialProfitPercentage = profitableItem.getProfitPercentage();

                // Format numbers
                String formattedBuyPrice = NumberUtils.formatNumberShort(ourBuyPrice);
                String formattedSellPrice = NumberUtils.formatNumberShort(ourSellPrice);
                String formattedBuyVol = NumberUtils.formatNumberShort(totalBuyVolume);
                String formattedSellVol = NumberUtils.formatNumberShort(totalSellVolume);
                String formattedProfit = NumberUtils.formatNumberShort(potentialProfitPerItem);
                String formattedProfitPercent = String.format("%.1f%%", potentialProfitPercentage);

                // Build message string with the colors from user's code
                String evalMsgString = String.format("%s[%s]%s Evaluating: %s%s %s| %sBuy: %s%s %s| %sSell: %s%s %s| %sVol(B/S): %s%s%s/%s%s %s| %sProfit: %s%s %s(%s)",
                        EnumChatFormatting.GOLD, modName, EnumChatFormatting.GRAY,                  // Prefix §6[Oronyx]§7 Evaluating:
                        EnumChatFormatting.YELLOW, itemName,                                        // Item Name §e{ItemName}
                        EnumChatFormatting.DARK_GRAY, EnumChatFormatting.GRAY, EnumChatFormatting.WHITE, formattedBuyPrice, // Separator §8| §7Buy:§f{BuyP} <-- Changed Buy color to White
                        EnumChatFormatting.DARK_GRAY, EnumChatFormatting.GRAY, EnumChatFormatting.RED, formattedSellPrice, // Separator §8| §7Sell:§c{SellP}
                        EnumChatFormatting.DARK_GRAY, EnumChatFormatting.GRAY, EnumChatFormatting.WHITE, formattedBuyVol, EnumChatFormatting.GRAY, EnumChatFormatting.RED, formattedSellVol, // Separator §8| §7Vol(B/S):§f{BuyV}§7/§c{SellV} <-- Changed SellVol color to Red
                        EnumChatFormatting.DARK_GRAY, EnumChatFormatting.GRAY, EnumChatFormatting.YELLOW, formattedProfit, // Separator §8| §7Profit:§e{Profit}
                        EnumChatFormatting.GOLD, formattedProfitPercent                               // Percentage §6({Profit%})
                );

                // Create components and events
                IChatComponent chatComponent = getIChatComponent(evalMsgString, itemName);

                ChatUtils.sendMessage(chatComponent);
            }
        }
        // --- End Send Chat Messages ---

        return finalItemList; // Return the final, sorted, and potentially limited list
    }

    private static IChatComponent getIChatComponent(String evalMsgString, String itemName) {
        IChatComponent chatComponent = new ChatComponentText(evalMsgString);
        ChatStyle style = new ChatStyle();
        String commandToRun = "/bz " + itemName;
        // !! WARNING: Using RUN_COMMAND !!
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandToRun);
        style.setChatClickEvent(clickEvent);
        IChatComponent hoverText = new ChatComponentText(EnumChatFormatting.YELLOW + "Click to " + EnumChatFormatting.RED + "Run" + EnumChatFormatting.YELLOW + " Command:\n" + EnumChatFormatting.GRAY + commandToRun);
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText);
        style.setChatHoverEvent(hoverEvent);
        chatComponent.setChatStyle(style);
        return chatComponent;
    }

    // --- Helper Methods for Enchantment Name Generation ---
    /** Generates formatted enchantment name from ID or returns null. */
    private static String generateEnchantmentName(String enchantmentId) {
        if (enchantmentId == null || !enchantmentId.startsWith("ENCHANTMENT_")) return null;
        String coreAndLevel = enchantmentId.substring("ENCHANTMENT_".length());
        int lastUnderscoreIndex = coreAndLevel.lastIndexOf('_');
        if (lastUnderscoreIndex == -1 || lastUnderscoreIndex >= coreAndLevel.length() - 1) return null;
        String coreNameRaw = coreAndLevel.substring(0, lastUnderscoreIndex);
        String levelString = coreAndLevel.substring(lastUnderscoreIndex + 1);
        int level;
        try {
            level = Integer.parseInt(levelString);
            if (level <= 0) return null;
        } catch (NumberFormatException e) { return null; }
        String formattedName;
        if (coreNameRaw.startsWith("ULTIMATE_")) {
            formattedName = coreNameRaw.equals("ULTIMATE_WISE") ? "Ultimate Wise" : formatNamePart(coreNameRaw.substring("ULTIMATE_".length()));
        } else if (coreNameRaw.startsWith("TURBO_")) {
            formattedName = "Turbo-" + formatNamePart(coreNameRaw.substring("TURBO_".length()));
        } else {
            formattedName = formatNamePart(coreNameRaw);
        }
        String romanLevel = toRoman(level);
        return formattedName + " " + (romanLevel != null ? romanLevel : levelString);
    }

    /** Formats raw name part ("SOME_NAME") to Title Case ("Some Name"). */
    private static String formatNamePart(String rawNamePart) {
        if (rawNamePart == null || rawNamePart.isEmpty()) return "";
        String[] words = rawNamePart.split("_+");
        StringJoiner joiner = new StringJoiner(" ");
        for (String word : words) {
            if (!word.isEmpty()) {
                joiner.add(word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase());
            }
        }
        return joiner.toString();
    }

    /** Converts integer 1-10 to Roman numeral. */
    private static String toRoman(int number) {
        return ROMAN_MAP.get(number);
    }

    // --- Helper Methods for Order Summaries ---
    /** Parses buy/sell summary JsonArray into a List of OrderSummary. */
    private static List<OrderSummary> parseOrderSummary(JsonArray summaryArray) {
        List<OrderSummary> summaryList = new ArrayList<>();
        if (summaryArray == null) return summaryList;
        for (JsonElement element : summaryArray) {
            if (!element.isJsonObject()) continue;
            JsonObject order = element.getAsJsonObject();
            if (order.has("amount") && order.get("amount").isJsonPrimitive() && order.get("amount").getAsJsonPrimitive().isNumber() &&
                    order.has("pricePerUnit") && order.get("pricePerUnit").isJsonPrimitive() && order.get("pricePerUnit").getAsJsonPrimitive().isNumber() &&
                    order.has("orders") && order.get("orders").isJsonPrimitive() && order.get("orders").getAsJsonPrimitive().isNumber())
            {
                try {
                    summaryList.add(new OrderSummary(
                            order.get("amount").getAsInt(),
                            order.get("pricePerUnit").getAsDouble(),
                            order.get("orders").getAsInt()
                    ));
                } catch (Exception e) {
                    Oronyx.LOGGER.warn("Failed to parse order summary entry: {} - Error: {}", order.toString().substring(0, Math.min(100, order.toString().length())), e.getMessage());
                }
            } else {
                Oronyx.LOGGER.warn("Skipping invalid order summary entry (missing/wrong type fields): {}", order.toString().substring(0, Math.min(100, order.toString().length())));
            }
        }
        return summaryList;
    }

    /** Calculates total volume from a list of OrderSummary. */
    private static long getTotalVolume(List<OrderSummary> summaryList) {
        long total = 0;
        if (summaryList == null) return 0;
        for (OrderSummary order : summaryList) {
            if (order != null) {
                total += order.getAmount();
            }
        }
        return total;
    }

    // --- Helper Method for Sorting ---
    /** Gets the appropriate Comparator for sorting (natural order for queue, reversed for final list). */
    private static Comparator<ProfitableItem> getSortComparator(String sortBy) {
        // This now returns the comparator for the FINAL list (best first)
        return getBaseComparator(sortBy).reversed();
    }

} // End of BazaarProcessor class