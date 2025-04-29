package me.khaithomx.oronyx.config;

import me.khaithomx.oronyx.Oronyx;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

/**
 * Handles loading, saving, and syncing configuration values using Forge's system.
 */
public class ModConfig {

    public static Configuration config; // Make static for easy access via ModConfig.config
    public static final String CATEGORY_GENERAL = "General";
    public static final String CATEGORY_PROFIT = "Profit Criteria";
    public static final String CATEGORY_PRICE = "Price Filters";
    public static final String CATEGORY_ORDERS = "Order Settings";
    public static final String CATEGORY_VOLUME = "Volume Filters";
    public static final String CATEGORY_PURSE = "Purse Settings";
    public static final String CATEGORY_API = "API Settings";

    /**
     * Initializes the configuration object. Called during PreInit.
     * @param configFile The suggested configuration file from Forge.
     */
    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            loadConfig();
        }
    }

    /**
     * Loads the configuration from the file or creates it with defaults.
     * Also syncs the loaded values to the static fields.
     */
    public static void loadConfig() {
        try {
            config.load();
            syncConfigValues(); // Load values from config file into static fields in Oronyx class
        } catch (Exception e) {
            Oronyx.LOGGER.error("Error loading configuration file!", e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    /**
     * Reads values from the Configuration object and updates the corresponding static fields in Oronyx.
     * This method synchronizes the loaded config values with the mod's runtime values.
     * It also defines the configuration options (comments, defaults, ranges).
     */
    public static void syncConfigValues() {
        // General Settings
        config.addCustomCategoryComment(CATEGORY_GENERAL, "General settings for the mod");
        Oronyx.modEnabled = config.getBoolean("modEnabled", CATEGORY_GENERAL, true,
                "Enable or disable all core functionality of the mod.");
        Oronyx.displayLimit = config.getInt("displayLimit", CATEGORY_GENERAL, 0, 0, 1000,
                "Maximum number of profitable items to list (0 means no limit).");
        Oronyx.showEvaluatingMessages = config.getBoolean("showEvaluatingMessages", CATEGORY_GENERAL, true,
                "Show the 'Evaluating: <Item> | ...' messages in chat during checks.");
        Oronyx.delay = config.getString("delay", CATEGORY_GENERAL, "60s",
                "Frequency of Bazaar checks (e.g., 30s, 1m, 5m). Minimum allowed is 5 seconds.");

        // Profit Criteria
        config.addCustomCategoryComment(CATEGORY_PROFIT, "Criteria for identifying profitable items");
        Oronyx.minProfit = config.getInt("minProfit", CATEGORY_PROFIT, 100, 0, Integer.MAX_VALUE,
                "Minimum required profit per item (after tax).");
        Oronyx.maxProfit = config.getInt("maxProfit", CATEGORY_PROFIT, 0, 0, Integer.MAX_VALUE,
                "Maximum allowed profit per item (0 means no limit). Use this to filter out potentially manipulated items.");
        Oronyx.minProfitPercentage = config.getInt("minProfitPercentage", CATEGORY_PROFIT, 10, 0, 10000,
                "Minimum required profit percentage relative to the buy price.");

        // Price Filters
        config.addCustomCategoryComment(CATEGORY_PRICE, "Filters based on item buy and sell prices");
        Oronyx.minPricePerUnitBuy = config.getInt("minPricePerUnitBuy", CATEGORY_PRICE, 0, 0, Integer.MAX_VALUE,
                "Minimum buy price per unit allowed (0 means no limit). Filters out extremely cheap items.");
        Oronyx.maxPricePerUnitBuy = config.getInt("maxPricePerUnitBuy", CATEGORY_PRICE, 0, 0, Integer.MAX_VALUE,
                "Maximum buy price per unit allowed (0 means no limit). Filters out extremely expensive items.");
        Oronyx.maxPricePerUnitSell = config.getInt("maxPricePerUnitSell", CATEGORY_PRICE, 0, 0, Integer.MAX_VALUE,
                "Maximum sell price per unit allowed (0 means no limit). Filters out potentially manipulated items.");

        // Volume Filters
        config.addCustomCategoryComment(CATEGORY_VOLUME, "Filters based on the total volume of buy/sell orders");
        Oronyx.minBuyVolume = config.getInt("minBuyVolume", CATEGORY_VOLUME, 50, 0, Integer.MAX_VALUE,
                "Minimum required total buy order volume for an item.");
        Oronyx.minSellVolume = config.getInt("minSellVolume", CATEGORY_VOLUME, 50, 0, Integer.MAX_VALUE,
                "Minimum required total sell offer volume for an item.");

        // Purse Settings
        config.addCustomCategoryComment(CATEGORY_PURSE, "Settings related to the player's purse");
        Oronyx.minPurse = config.getInt("minPurse", CATEGORY_PURSE, 0, 0, Integer.MAX_VALUE,
                "Minimum purse balance required for the mod to perform checks (0 disables this check).");
        // Handle 'long' for maxSpentPerOrder
        String maxSpentDefaultString = "6000000";
        Property propMaxSpent = config.get(CATEGORY_PURSE, "maxSpentPerOrder", maxSpentDefaultString,
                "Maximum estimated coins to spend on a single flip attempt (0 means no limit).");

        try {
            Oronyx.maxSpentPerOrder = Long.parseLong(propMaxSpent.getString());
        } catch (NumberFormatException e) {
            Oronyx.LOGGER.warn("Invalid format for 'maxSpentPerOrder' in config file: '{}'. Using default value: {}", propMaxSpent.getString(), maxSpentDefaultString);
            Oronyx.maxSpentPerOrder = Long.parseLong(maxSpentDefaultString); // Parse default string
            propMaxSpent.set(maxSpentDefaultString); // Set property back to default string
        }

        // Optional: Validate the parsed value (e.g., ensure non-negative)
        if (Oronyx.maxSpentPerOrder < 0L) {
            Oronyx.LOGGER.warn("Configuration value for 'maxSpentPerOrder' ({}) was negative. Setting to 0.", Oronyx.maxSpentPerOrder);
            Oronyx.maxSpentPerOrder = 0L;
            // Update the property object with the corrected value as a String
            propMaxSpent.set(String.valueOf(Oronyx.maxSpentPerOrder));
        }
        // <-- Sync new config -->
        Oronyx.filterByMaxPlayerPurse = config.getBoolean("filterByMaxPlayerPurse", CATEGORY_PURSE, true,
                "Enable filtering out items that cost more than the player's current purse.");
        // <-- End sync -->

        // Order Settings
        config.addCustomCategoryComment(CATEGORY_ORDERS, "Settings related to sorting and order behavior (some are placeholders)");
        Oronyx.sortBy = config.getString("sortBy", CATEGORY_ORDERS, "profit",
                "Sort results by: 'profit' or 'profitPercentage'.", new String[]{"profit", "profitPercentage"});

        // API Settings
        config.addCustomCategoryComment(CATEGORY_API, "Settings for Hypixel API interaction");
        Oronyx.bazaarUrl = config.getString("bazaarUrl", CATEGORY_API, "https://api.hypixel.net/v2/skyblock/bazaar",
                "Hypixel API URL for Bazaar data. Do not change unless you know what you are doing.");
        Oronyx.itemListUrl = config.getString("itemListUrl", CATEGORY_API, "https://api.hypixel.net/v2/resources/skyblock/items",
                "Hypixel API URL for Item List. Do not change unless you know what you are doing.");
        Oronyx.tax = config.get(CATEGORY_API, "tax", 1.01125,
                "Bazaar tax multiplier. Example: 1.1 = 10% tax, 1.01125 = 1.125% tax. Value must be >= 1.0.", 1.0, 2.0).getDouble();


        // Final save check after all properties have been processed
        if (config.hasChanged()) {
            config.save();
            Oronyx.LOGGER.info("Configuration saved due to changes or defaults applied.");
        }
    }

    /**
     * Event handler class to reload config changes made via the GUI.
     */
    public static class ConfigEventHandler {
        /**
         * Listens for config change events and re-syncs the mod's static values.
         * @param event The config changed event.
         */
        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.modID.equalsIgnoreCase(Oronyx.MODID)) {
                Oronyx.LOGGER.info("Oronyx configuration changed via GUI, reloading...");
                syncConfigValues();
                if(Oronyx.instance != null && Oronyx.instance.itemListCache != null) {
                    Oronyx.instance.itemListCache.updateApiUrl(Oronyx.itemListUrl);
                }
            }
        }
    }
}