package me.khaithomx.oronyx.gui;

import me.khaithomx.oronyx.Oronyx;
import me.khaithomx.oronyx.config.ModConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
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
        super(parentScreen,
                getConfigElements(), // Call helper method
                Oronyx.MODID,
                false,
                false,
                GuiConfig.getAbridgedConfigPath(ModConfig.config.toString()));
        // Alternative custom title:
        this.title = Oronyx.NAME + " Configuration";
    }

    /**
     * Generates the list of configuration categories to be displayed in the GUI.
     * @return A List of IConfigElement objects representing the configuration categories.
     */
    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> list = new ArrayList<>();
        Configuration config = ModConfig.config;

        if (config != null) {
            // Add categories that should appear in the GUI
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_GENERAL)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_PROFIT)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_PRICE)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_VOLUME)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_PURSE)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_ORDERS)));
            list.add(new ConfigElement(config.getCategory(ModConfig.CATEGORY_API)));

        } else {
            Oronyx.LOGGER.error("Configuration object is null when trying to build config GUI elements!");
        }
        return list;
    }

    /** Overrides background drawing to use a solid dark grey color. */
    @Override
    public void drawWorldBackground(int tint) {
        Gui.drawRect(0, 0, this.width, this.height, 0xFF202020); // Dark grey background
    }

    // Standard GuiConfig handles saving and element interaction automatically.
}