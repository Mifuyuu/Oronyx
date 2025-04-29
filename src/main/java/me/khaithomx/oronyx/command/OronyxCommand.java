package me.khaithomx.oronyx.command;

import me.khaithomx.oronyx.Oronyx;
import me.khaithomx.oronyx.config.ModConfig; // To save blacklist
import me.khaithomx.oronyx.util.ChatUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the /oronyx command, currently focused on blacklist management.
 */
public class OronyxCommand extends CommandBase {

    private final List<String> subCommands = Collections.singletonList("blacklist"); // Only one subcommand for now
    private final List<String> blacklistActions = Arrays.asList("add", "remove", "clear", "list");

    @Override
    public String getCommandName() {
        return "oronyx";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        // Provide clear usage instructions
        return "/oronyx blacklist <add|remove|list|clear> [item name...]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // No permission needed for client-side commands
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true; // Accessible by anyone on the client
    }

    /**
     * Main command processing logic.
     */
    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1 || !args[0].equalsIgnoreCase("blacklist")) {
            // If first arg isn't "blacklist", show usage
            throw new WrongUsageException(getCommandUsage(sender));
        }

        if (args.length < 2) {
            // If only "blacklist" is provided, require an action
            ChatUtils.sendModMessage(EnumChatFormatting.RED + "Missing action for blacklist. Use: " + String.join(", ", blacklistActions));
            throw new WrongUsageException(getCommandUsage(sender));
        }

        String action = args[1].toLowerCase(); // Get the action (add, remove, etc.)

        // Handle different actions
        switch (action) {
            case "add":
                // Need at least one more arg for item name
                if (args.length < 3) {
                    ChatUtils.sendModMessage(EnumChatFormatting.RED + "Usage: /oronyx blacklist add <item name...>"); return;
                }
                // Join remaining args to form the item name (allows spaces)
                String itemToAdd = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                handleBlacklistAdd(sender, itemToAdd);
                break;
            case "remove":
                // Need at least one more arg for item name
                if (args.length < 3) {
                    ChatUtils.sendModMessage(EnumChatFormatting.RED + "Usage: /oronyx blacklist remove <item name...>"); return;
                }
                String itemToRemove = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                handleBlacklistRemove(sender, itemToRemove);
                break;
            case "clear":
                // No additional args needed
                if (args.length > 2) {
                    ChatUtils.sendModMessage(EnumChatFormatting.RED + "Usage: /oronyx blacklist clear (no item name needed)"); return;
                }
                handleBlacklistClear(sender);
                break;
            case "list":
                // No additional args needed
                if (args.length > 2) {
                    ChatUtils.sendModMessage(EnumChatFormatting.RED + "Usage: /oronyx blacklist list (no item name needed)"); return;
                }
                handleBlacklistList(sender);
                break;
            default:
                // Action wasn't recognized
                ChatUtils.sendModMessage(EnumChatFormatting.RED + "Unknown blacklist action: '" + action + "'. Use: " + String.join(", ", blacklistActions));
                throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    /** Handles adding an item to the blacklist */
    private void handleBlacklistAdd(ICommandSender sender, String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            ChatUtils.sendModMessage(EnumChatFormatting.RED + "Item name cannot be empty.");
            return;
        }
        String lowerItemName = itemName.trim().toLowerCase(); // Use lowercase for storage/checking
        // Set.add returns true if the item was not already present
        if (Oronyx.blacklistSet.add(lowerItemName)) {
            ModConfig.saveBlacklist(); // Save the updated list to the config file
            ChatUtils.sendModMessage(EnumChatFormatting.GREEN + "Added '" + EnumChatFormatting.YELLOW + itemName + EnumChatFormatting.GREEN + "' to the blacklist.");
        } else {
            ChatUtils.sendModMessage(EnumChatFormatting.YELLOW + "'" + itemName + "' is already on the blacklist.");
        }
    }

    /** Handles removing an item from the blacklist */
    private void handleBlacklistRemove(ICommandSender sender, String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            ChatUtils.sendModMessage(EnumChatFormatting.RED + "Item name cannot be empty.");
            return;
        }
        String lowerItemName = itemName.trim().toLowerCase();
        // Set.remove returns true if the item was present and removed
        if (Oronyx.blacklistSet.remove(lowerItemName)) {
            ModConfig.saveBlacklist();
            ChatUtils.sendModMessage(EnumChatFormatting.GREEN + "Removed '" + EnumChatFormatting.YELLOW + itemName + EnumChatFormatting.GREEN + "' from the blacklist.");
        } else {
            ChatUtils.sendModMessage(EnumChatFormatting.RED + "'" + itemName + "' was not found on the blacklist (case-insensitive).");
        }
    }

    /** Handles clearing the entire blacklist */
    private void handleBlacklistClear(ICommandSender sender) {
        if (Oronyx.blacklistSet.isEmpty()) {
            ChatUtils.sendModMessage(EnumChatFormatting.YELLOW + "Blacklist is already empty.");
            return;
        }
        int sizeBefore = Oronyx.blacklistSet.size();
        Oronyx.blacklistSet.clear(); // Clear the in-memory set
        ModConfig.saveBlacklist(); // Save the now empty list to the config
        ChatUtils.sendModMessage(EnumChatFormatting.GREEN + "Cleared " + sizeBefore + " items from the blacklist.");
    }

    /** Handles listing all items currently on the blacklist */
    private void handleBlacklistList(ICommandSender sender) {
        if (Oronyx.blacklistSet.isEmpty()) {
            ChatUtils.sendModMessage(EnumChatFormatting.YELLOW + "The blacklist is empty.");
            return;
        }
        // Create a sorted list for consistent display
        List<String> sortedList = new ArrayList<>(Oronyx.blacklistSet);
        Collections.sort(sortedList);

        ChatUtils.sendModMessage(EnumChatFormatting.GOLD + "--- Oronyx Blacklist (" + sortedList.size() + " items) ---");
        for(String item : sortedList) {
            // Send each item on a new line. Display the stored lowercase version.
            ChatUtils.sendPlainMessage(EnumChatFormatting.YELLOW + "- " + item);
        }
    }

    /**
     * Provides tab completion suggestions for the command.
     */
    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            // Suggest "blacklist"
            return getListOfStringsMatchingLastWord(args, subCommands);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("blacklist")) {
            // Suggest actions "add", "remove", "list", "clear"
            return getListOfStringsMatchingLastWord(args, blacklistActions);
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("blacklist")) {
            String action = args[1].toLowerCase();
            if (action.equals("remove")) {
                // Suggest items currently on the blacklist for removal
                // Combine typed parts of item name
                String currentInput = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).toLowerCase();
                // Filter blacklist set for items starting with current input
                List<String> suggestions = Oronyx.blacklistSet.stream()
                        .filter(item -> item.startsWith(currentInput))
                        .sorted()
                        .collect(Collectors.toList());
                return suggestions;
            }
            // Could add suggestions for 'add' based on recent items? (More complex)
        }
        return null; // No suggestions for other cases
    }
}