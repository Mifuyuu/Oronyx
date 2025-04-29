package me.khaithomx.oronyx.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

import java.util.Set;

/**
 * Factory class required by Forge to locate and display the configuration GUI
 * in the Mod Options screen.
 */
public class OronyxGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {
        // Method called by Forge during initialization.
        // Can be left empty for basic config GUIs.
    }

    /**
     * Returns the class of the main configuration GUI screen.
     * @return The ModConfigGui class.
     */
    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ModConfigGui.class;
    }

    /**
     * Used for runtime GUI options. Return null if not implemented.
     * @return null.
     */
    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    /**
     * Used for runtime GUI options handler. Return null if not implemented.
     * @param element The runtime option category element.
     * @return null.
     */
    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }
}