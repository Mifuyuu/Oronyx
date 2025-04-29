package me.khaithomx.oronyx;

import me.khaithomx.oronyx.api.ItemListCache;
import me.khaithomx.oronyx.bazaar.BazaarProcessor;
import me.khaithomx.oronyx.bazaar.ProfitableItem;
import me.khaithomx.oronyx.config.ModConfig;
import me.khaithomx.oronyx.handler.ClientTickHandler;
import me.khaithomx.oronyx.handler.KeyInputHandler;
import me.khaithomx.oronyx.command.OronyxCommand; // <-- Import Command Class
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.ClientCommandHandler; // <-- Import Command Handler Registry
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.util.Collections;
import java.util.List;
import java.util.Set; // <-- Import Set
import java.util.concurrent.ConcurrentHashMap; // <-- Import ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList;

@Mod(modid = Oronyx.MODID, name = Oronyx.NAME, version = Oronyx.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]",
        guiFactory = "me.khaithomx.oronyx.gui.OronyxGuiFactory")
public class Oronyx {
    public static final String MODID = "oronyx";
    public static final String NAME = "Oronyx";
    public static final String VERSION = "1.1.5-Beta";

    @Mod.Instance(MODID)
    public static Oronyx instance;
    public static final Logger LOGGER = LogManager.getLogger(NAME);

    // Instances of core components
    public ItemListCache itemListCache;
    public BazaarProcessor bazaarProcessor;
    public ClientTickHandler clientTickHandler;

    // Keybinding
    public static KeyBinding openConfigKeybind;

    // --- Static config fields (populated by ModConfig.syncConfigValues) ---
    // General
    public static boolean modEnabled;
    public static int displayLimit;
    public static boolean showEvaluatingMessages;
    public static String delay;
    // Profit Criteria
    public static int minProfit;
    public static int maxProfit;
    public static int minProfitPercentage;
    public static int maxProfitPercentage; // Added
    // Price Filters
    public static int minPricePerUnitBuy;
    public static int maxPricePerUnitBuy;
    public static int maxPricePerUnitSell;
    // Volume Filters
    public static int minBuyVolume;
    public static int minSellVolume;
    // Purse Settings
    public static int minPurse;
    public static long maxSpentPerOrder;
    public static boolean filterByMaxPlayerPurse;
    // Order & Filtering Settings
    public static String sortBy;
    public static boolean blockUltimateEnchants; // Added
    public static boolean blockNormalEnchants;   // Added
    public static boolean enableBlacklistFilter; // Added (replaces useBlacklistFilter)
    public static String[] blacklistItems;      // Added (data source for blacklistSet)
    // API Settings
    public static String bazaarUrl;
    public static String itemListUrl;
    public static double tax;
    // --- End of static config fields ---

    // --- Blacklist Data (In-memory Set for fast lookup) ---
    // Use Set for efficient contains checks, ConcurrentHashMap based for thread safety (though likely overkill here)
    public static Set<String> blacklistSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // --- ---

    // Results list (thread-safe for updates/reads)
    private List<ProfitableItem> lastProfitableItems = new CopyOnWriteArrayList<>();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;
        Oronyx.LOGGER.info("Starting Oronyx v{} Pre-Initialization...", VERSION);
        // Initialize configuration
        ModConfig.init(event.getSuggestedConfigurationFile());
        // Register config change handler
        MinecraftForge.EVENT_BUS.register(new ModConfig.ConfigEventHandler());
        Oronyx.LOGGER.info("Configuration system initialized.");
        // Populate initial blacklist set from config after first sync
        ModConfig.populateBlacklistSet();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        Oronyx.LOGGER.info("Starting Oronyx Initialization...");
        // Initialize core components (use static config values now)
        itemListCache = new ItemListCache(Oronyx.itemListUrl);
        bazaarProcessor = new BazaarProcessor();
        clientTickHandler = new ClientTickHandler();

        // Register Event Handlers
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
        MinecraftForge.EVENT_BUS.register(clientTickHandler);

        // Register Keybinds
        openConfigKeybind = new KeyBinding("Open Oronyx Config", Keyboard.KEY_O, "Oronyx"); // Example key
        ClientRegistry.registerKeyBinding(openConfigKeybind);

        // Register Commands
        ClientCommandHandler.instance.registerCommand(new OronyxCommand()); // <-- Register command handler
        Oronyx.LOGGER.info("Commands registered.");

        Oronyx.LOGGER.info("Event handlers and keybinds registered.");
        // Initial fetch for item list
        itemListCache.initialItemListFetch();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        Oronyx.LOGGER.info("Oronyx Initialization complete.");
    }

    /**
     * Updates the list of profitable items found. Called by ClientTickHandler.
     * @param items The new list of profitable items.
     */
    public void updateLastProfitableItems(List<ProfitableItem> items) {
        this.lastProfitableItems.clear();
        if (items != null) {
            this.lastProfitableItems.addAll(items);
        }
        Oronyx.LOGGER.debug("Updated profitable items list. Size: {}", this.lastProfitableItems.size());
    }

    /**
     * Gets an immutable view of the last found profitable items.
     * @return An unmodifiable list of ProfitableItem.
     */
    public List<ProfitableItem> getLastProfitableItems() {
        return Collections.unmodifiableList(this.lastProfitableItems);
    }
}