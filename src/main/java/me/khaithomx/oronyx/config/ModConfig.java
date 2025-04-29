package me.khaithomx.oronyx.config;

import me.khaithomx.oronyx.Oronyx;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;
import java.util.Arrays;

/**
 * Handles loading, saving, and syncing configuration values using Forge's system.
 */
public class ModConfig {

    public static Configuration config;
    // Updated category names
    public static final String CATEGORY_GENERAL = "general";
    public static final String CATEGORY_PROFIT = "profit_criteria";
    public static final String CATEGORY_PRICE = "price_filters";
    public static final String CATEGORY_ORDERS = "order_settings"; // Renamed
    public static final String CATEGORY_VOLUME = "volume_filters";
    public static final String CATEGORY_PURSE = "purse_settings";
    public static final String CATEGORY_API = "api_settings";
    // Removed CATEGORY_BLACKLIST

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
            syncConfigValues();
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
     * Also defines the configuration options (comments, defaults, ranges).
     */
    public static void syncConfigValues() {
        // General Settings
        config.addCustomCategoryComment(CATEGORY_GENERAL, "General settings for the mod");
        Oronyx.modEnabled = config.getBoolean("modEnabled", CATEGORY_GENERAL, true, "Enable or disable all core functionality of the mod.");
        Oronyx.displayLimit = config.getInt("displayLimit", CATEGORY_GENERAL, 0, 0, 1000, "Maximum number of profitable items to list (0 means no limit).");
        Oronyx.showEvaluatingMessages = config.getBoolean("showEvaluatingMessages", CATEGORY_GENERAL, true, "Show the 'Evaluating: <Item> | ...' messages in chat during checks.");
        Oronyx.delay = config.getString("delay", CATEGORY_GENERAL, "60s", "Frequency of Bazaar checks (e.g., 30s, 1m, 5m). Minimum allowed is 5 seconds.");

        // Profit Criteria
        config.addCustomCategoryComment(CATEGORY_PROFIT, "Criteria for identifying profitable items");
        Oronyx.minProfit = config.getInt("minProfit", CATEGORY_PROFIT, 100, 0, Integer.MAX_VALUE, "Minimum required profit per item (after tax).");
        Oronyx.maxProfit = config.getInt("maxProfit", CATEGORY_PROFIT, 0, 0, Integer.MAX_VALUE, "Maximum allowed profit per item (0 means no limit).");
        Oronyx.minProfitPercentage = config.getInt("minProfitPercentage", CATEGORY_PROFIT, 10, 0, 10000, "Minimum required profit percentage relative to the buy price.");
        Oronyx.maxProfitPercentage = config.getInt("maxProfitPercentage", CATEGORY_PROFIT, 0, 0, 10000, "Maximum allowed profit percentage (0 means no limit)."); // Added

        // Price Filters
        config.addCustomCategoryComment(CATEGORY_PRICE, "Filters based on item buy and sell prices");
        Oronyx.minPricePerUnitBuy = config.getInt("minPricePerUnitBuy", CATEGORY_PRICE, 0, 0, Integer.MAX_VALUE, "Minimum buy price per unit allowed (0 means no limit).");
        Oronyx.maxPricePerUnitBuy = config.getInt("maxPricePerUnitBuy", CATEGORY_PRICE, 0, 0, Integer.MAX_VALUE, "Maximum buy price per unit allowed (0 means no limit).");
        Oronyx.maxPricePerUnitSell = config.getInt("maxPricePerUnitSell", CATEGORY_PRICE, 0, 0, Integer.MAX_VALUE, "Maximum sell price per unit allowed (0 means no limit).");

        // Volume Filters
        config.addCustomCategoryComment(CATEGORY_VOLUME, "Filters based on the total volume of buy/sell orders");
        Oronyx.minBuyVolume = config.getInt("minBuyVolume", CATEGORY_VOLUME, 50, 0, Integer.MAX_VALUE, "Minimum required total buy order volume for an item.");
        Oronyx.minSellVolume = config.getInt("minSellVolume", CATEGORY_VOLUME, 50, 0, Integer.MAX_VALUE, "Minimum required total sell offer volume for an item.");

        // Purse Settings
        config.addCustomCategoryComment(CATEGORY_PURSE, "Settings related to the player's purse");
        Oronyx.minPurse = config.getInt("minPurse", CATEGORY_PURSE, 0, 0, Integer.MAX_VALUE, "Minimum purse balance required for the mod to perform checks (0 disables this check).");
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

        // Order & Filtering Settings
        config.addCustomCategoryComment(CATEGORY_ORDERS, "Settings for sorting results and additional item filtering");
        Oronyx.sortBy = config.getString("sortBy", CATEGORY_ORDERS, "profit", "Sort results by: 'profit' or 'profitPercentage'.", new String[]{"profit", "profitPercentage"});
        Oronyx.blockUltimateEnchants = config.getBoolean("blockUltimateEnchants", CATEGORY_ORDERS, false, "Block Ultimate Enchantments from appearing in results."); // Added
        Oronyx.blockNormalEnchants = config.getBoolean("blockNormalEnchants", CATEGORY_ORDERS, false, "Block Normal (non-Ultimate) Enchantments from appearing in results."); // Added
        Oronyx.enableBlacklistFilter = config.getBoolean("enableBlacklistFilter", CATEGORY_ORDERS, true, "Enable filtering using the Item Blacklist (managed via /oronyx command)."); // Added and Moved

        // API Settings
        config.addCustomCategoryComment(CATEGORY_API, "Settings for Hypixel API interaction");
        Oronyx.bazaarUrl = config.getString("bazaarUrl", CATEGORY_API, "https://api.hypixel.net/v2/skyblock/bazaar", "Hypixel API URL for Bazaar data.");
        Oronyx.itemListUrl = config.getString("itemListUrl", CATEGORY_API, "https://api.hypixel.net/v2/resources/skyblock/items", "Hypixel API URL for Item List.");
        Oronyx.tax = config.get(CATEGORY_API, "tax", 1.01125, "Bazaar tax multiplier. Example: 1.1 = 10% tax, 1.01125 = 1.125% tax. Value must be >= 1.0.", 1.0, 2.0).getDouble();

        // Item Blacklist (Data only, moved category for storage)
        // Removed the dedicated category comment block
        Oronyx.blacklistItems = config.getStringList("blacklistItems", Configuration.CATEGORY_GENERAL, new String[0], "Internal list of blacklisted items. Use /oronyx blacklist commands to manage."); // Stored in General

        // Final save check
        if (config.hasChanged()) {
            config.save();
            Oronyx.LOGGER.info("Configuration saved due to changes or defaults applied.");
        }
    }

    /**
     * Populates the in-memory blacklist set from the loaded config array.
     * Converts names to lowercase for case-insensitive matching.
     */
    public static void populateBlacklistSet() {
        Oronyx.blacklistSet.clear(); // Clear existing set before populating
        if (Oronyx.blacklistItems != null) {
            for (String item : Oronyx.blacklistItems) {
                if (item != null && !item.trim().isEmpty()) {
                    Oronyx.blacklistSet.add(item.trim().toLowerCase()); // Add lowercase version
                }
            }
        }
        Oronyx.LOGGER.info("Loaded {} items into the blacklist set.", Oronyx.blacklistSet.size());
    }

    /**
     * Saves the current in-memory blacklist set back to the Configuration object
     * and saves the configuration file. Should be called after modifying the blacklist via commands.
     */
    public static void saveBlacklist() {
        if (config == null) {
            Oronyx.LOGGER.error("Cannot save blacklist, Configuration object is null!");
            return;
        }
        // Convert the Set back to a String array
        String[] blacklistArray = Oronyx.blacklistSet.toArray(new String[0]);
        Arrays.sort(blacklistArray); // Keep the list sorted in the config file

        // Get the property from the correct category (now General) and update its value
        Property prop = config.get(Configuration.CATEGORY_GENERAL, "blacklistItems", new String[0]);
        prop.set(blacklistArray); // Set the String array value

        // Save the entire configuration file if changes were made
        if (config.hasChanged()) {
            config.save();
            Oronyx.LOGGER.info("Blacklist saved to configuration file.");
        }
    }


    /**
     * Event handler class to reload config changes made via the GUI.
     */
    public static class ConfigEventHandler {
        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.modID.equalsIgnoreCase(Oronyx.MODID)) {
                Oronyx.LOGGER.info("Oronyx configuration changed via GUI, reloading...");
                syncConfigValues(); // Reload all values
                populateBlacklistSet(); // Repopulate blacklist set from potentially changed list in config
                if(Oronyx.instance != null && Oronyx.instance.itemListCache != null) {
                    Oronyx.instance.itemListCache.updateApiUrl(Oronyx.itemListUrl);
                }
            }
        }
    }
}