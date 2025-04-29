package me.khaithomx.oronyx.gui;

import me.khaithomx.oronyx.Oronyx;
import me.khaithomx.oronyx.config.ModConfig; // Import for category constants and config object
import net.minecraft.client.gui.Gui; // For drawing background rect
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement; // For creating elements from categories
import net.minecraftforge.common.config.Configuration; // To access the config object
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration GUI screen using Forge's GuiConfig system.
 */
public class ModConfigGui extends GuiConfig {

    /**
     * Constructor for the configuration GUI.
     * @param parentScreen The screen that opened this GUI.
     */
    public ModConfigGui(GuiScreen parentScreen) {
        // Call the super constructor provided by GuiConfig
        super(parentScreen,                                             // Parent screen
                getConfigElements(),                                    // List of config elements (categories) to display
                Oronyx.MODID,                                           // Mod ID
                false,                                                  // Does changing settings require world restart?
                false,                                                  // Does changing settings require MC restart?
                GuiConfig.getAbridgedConfigPath(ModConfig.config.toString())); // Title or path shown at the top
        // Alternative custom title:
        this.title = Oronyx.NAME + " Configuration";
    }

    /**
     * Generates the list of configuration categories to be displayed in the GUI.
     * Each category will appear as a clickable entry on the left side.
     * @return A List of IConfigElement objects representing the configuration categories.
     */
    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        Configuration config = ModConfig.config; // Get the static Configuration instance

        // Check if config exists before trying to access categories
        if (config != null) {
            // Create a ConfigElement for each category. GuiConfig automatically populates
            // the options within that category based on the Configuration object.
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_GENERAL)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_PROFIT)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_PRICE)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_VOLUME)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_PURSE)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_ORDERS)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_API)));
        } else {
            Oronyx.LOGGER.error("Configuration object is null when trying to build config GUI elements!");
            // Optionally add a dummy element indicating an error?
            // list.add(new DummyConfigElement("Error: Config not loaded!", "error", ConfigGuiType.STRING, "error.config.not.loaded"));
        }

        return list;
    }

    /**
     * Overrides the default background drawing behavior to use a solid dark grey color.
     * @param tint Legacy parameter, not typically used for solid backgrounds.
     */
    @Override
    public void drawWorldBackground(int tint) {
        // Draw a solid dark grey rectangle covering the entire screen.
        // 0xFF for alpha (fully opaque), 202020 for the dark grey color in Hex RRGGBB.
        Gui.drawRect(0, 0, this.width, this.height, 0xFF202020);

        // !! DO NOT call super.drawWorldBackground(tint) here !!
        // Calling super would draw the default dirt background over our solid color.
    }

    // Note: For standard config types (boolean, int, double, String), GuiConfig handles
    // drawing the options, handling clicks, text input, saving, etc. automatically.
    // You usually don't need to override initGui, actionPerformed, keyTyped, mouseClicked,
    // or drawScreen unless you are adding highly custom elements beyond the standard config properties.
}