package me.khaithomx.oronyx.handler;

import me.khaithomx.oronyx.Oronyx;
import me.khaithomx.oronyx.gui.ModConfigGui; // Import the new GuiConfig based class
// ProfitDisplayGui import commented out or removed if not implemented
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Handles key presses for the mod.
 */
@SideOnly(Side.CLIENT)
public class KeyInputHandler {

    private final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        // Check if the keybind exists and was pressed
        if (Oronyx.openConfigKeybind != null && Oronyx.openConfigKeybind.isPressed()) {
            // Check if we are not in a complex GUI already. Allow opening from null (main menu/no gui) or chat.
            if (mc.currentScreen == null || mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) {
                // Schedule the GUI opening on the main thread to avoid potential issues
                // Pass the current screen as the parent, allowing return via ESC or Done button
                mc.addScheduledTask(() -> mc.displayGuiScreen(new ModConfigGui(mc.currentScreen)));
            }
        }

        // Profit Display GUI keybind (Placeholder)
        /*
        if (Oronyx.openProfitGuiKeybind != null && Oronyx.openProfitGuiKeybind.isPressed()) {
            if (mc.currentScreen == null) {
                mc.addScheduledTask(() -> mc.displayGuiScreen(new ProfitDisplayGui(null)));
            }
        }
        */
    }
}