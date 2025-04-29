package me.khaithomx.oronyx;

import me.khaithomx.oronyx.api.ItemListCache;
import me.khaithomx.oronyx.bazaar.BazaarProcessor;
import me.khaithomx.oronyx.bazaar.ProfitableItem;
import me.khaithomx.oronyx.config.ModConfig;
import me.khaithomx.oronyx.handler.ClientTickHandler;
import me.khaithomx.oronyx.handler.KeyInputHandler;
// Removed ServerChecker import
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
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
import java.util.concurrent.CopyOnWriteArrayList;

@Mod(modid = Oronyx.MODID, name = Oronyx.NAME, version = Oronyx.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]",
        guiFactory = "me.khaithomx.oronyx.gui.OronyxGuiFactory")
public class Oronyx {
    public static final String MODID = "oronyx";
    public static final String NAME = "Oronyx";
    public static final String VERSION = "1.0-Beta";

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
    public static boolean modEnabled;
    public static int displayLimit;
    public static boolean showEvaluatingMessages;
    public static String delay;
    public static int minProfit;
    public static int maxProfit;
    public static int minProfitPercentage;
    public static int minPricePerUnitBuy;
    public static int maxPricePerUnitBuy;
    public static int maxPricePerUnitSell;
    public static int minBuyVolume;
    public static int minSellVolume;
    public static int minPurse;
    public static long maxSpentPerOrder;
    public static boolean filterByMaxPlayerPurse; // <-- Config field added
    public static String sortBy;
    public static String bazaarUrl;
    public static String itemListUrl;
    public static double tax;
    // --- End of static config fields ---

    // Results list (thread-safe)
    private List<ProfitableItem> lastProfitableItems = new CopyOnWriteArrayList<>();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;
        Oronyx.LOGGER.info("Starting Oronyx v{} Pre-Initialization...", VERSION);
        ModConfig.init(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(new ModConfig.ConfigEventHandler());
        Oronyx.LOGGER.info("Configuration system initialized.");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        Oronyx.LOGGER.info("Starting Oronyx Initialization...");
        itemListCache = new ItemListCache(Oronyx.itemListUrl);
        bazaarProcessor = new BazaarProcessor();
        clientTickHandler = new ClientTickHandler();

        // Register Event Handlers
        MinecraftForge.EVENT_BUS.register(new KeyInputHandler());
        MinecraftForge.EVENT_BUS.register(clientTickHandler);
        // No ServerChecker registration

        // Register Keybinds
        openConfigKeybind = new KeyBinding("Open Oronyx Config", Keyboard.KEY_O, "Oronyx");
        ClientRegistry.registerKeyBinding(openConfigKeybind);

        Oronyx.LOGGER.info("Event handlers and keybinds registered.");
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
        // If a display GUI existed, notify it here
    }

    /**
     * Gets an immutable view of the last found profitable items.
     * @return An unmodifiable list of ProfitableItem.
     */
    public List<ProfitableItem> getLastProfitableItems() {
        return Collections.unmodifiableList(this.lastProfitableItems);
    }
}