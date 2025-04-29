package me.khaithomx.oronyx.util;

import me.khaithomx.oronyx.Oronyx;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent; // <-- Import เพิ่ม

public class ChatUtils {

    private static final String PREFIX_STRING = EnumChatFormatting.AQUA + "[" + Oronyx.NAME + "] " + EnumChatFormatting.RESET;
    private static final IChatComponent PREFIX_COMPONENT = new ChatComponentText(PREFIX_STRING);

    // Send a message with the mod prefix (using String)
    public static void sendModMessage(String message) {
        IChatComponent finalComponent = PREFIX_COMPONENT.createCopy().appendSibling(new ChatComponentText(message));
        sendMessage(finalComponent);
    }

    // Send a plain message to chat (using String)
    public static void sendPlainMessage(String message) {
        sendMessage(new ChatComponentText(message));
    }

    /**
     * Sends an IChatComponent to the player's chat.
     * Handles scheduling to the main thread.
     * @param component The chat component to send.
     */
    public static void sendMessage(IChatComponent component) { // <-- เมธอดที่อัปเดต / เพิ่มใหม่
        Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.thePlayer != null && component != null) {
            mc.addScheduledTask(() -> {
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(component); // <-- ส่ง Component โดยตรง
                }
            });
        } else if (mc == null || mc.thePlayer == null) {
            Oronyx.LOGGER.debug("Attempted to send chat component when player is not available.");
        }
    }

    // Send a debug message
    public static void sendDebugMessage(String message) {
        // Consider adding a config option for this later
        sendModMessage(EnumChatFormatting.GRAY + "[DEBUG] " + message);
    }
}