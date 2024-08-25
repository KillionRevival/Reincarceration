package org.kif.reincarceration.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.util.ItemUtil;
import org.kif.reincarceration.util.MessageUtil;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.permission.PermissionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Command to inspect a player's inventory for flagged or unflagged items.
 */
public class InspectInventoryCommand implements CommandExecutor {

    private final Reincarceration plugin;
    private final PermissionManager permissionManager;

    /**
     * Constructs a new InspectInventoryCommand.
     *
     * @param plugin The main plugin instance.
     */
    public InspectInventoryCommand(Reincarceration plugin) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin);
    }

    /**
     * Executes the inspect inventory command.
     *
     * @param sender  The command sender.
     * @param command The command.
     * @param label   The command label.
     * @param args    The command arguments.
     * @return true if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ConsoleUtil.sendError("This command can only be executed by a player.");
            return true;
        }

        if (!player.hasPermission("reincarceration.admin.inspectinventory") && !player.hasPermission("reincarceration.admin")) {
            MessageUtil.sendPrefixMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            MessageUtil.sendPrefixMessage(player, "&cUsage: /inspectinventory <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            MessageUtil.sendPrefixMessage(player, "&cPlayer not found or not online.");
            return true;
        }

        try {
            boolean isAssociated = permissionManager.isAssociatedWithBaseGroup(target.getUniqueId());
            List<ItemStack> violatingItems = new ArrayList<>();

            for (ItemStack item : target.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    boolean hasFlag = ItemUtil.hasReincarcerationFlag(item);
                    if ((isAssociated && !hasFlag) || (!isAssociated && hasFlag)) {
                        violatingItems.add(item);
                    }
                }
            }

            if (isAssociated) {
                if (violatingItems.isEmpty()) {
                    MessageUtil.sendPrefixMessage(player, "&aNo unflagged items found in " + target.getName() + "'s inventory.");
                } else {
                    MessageUtil.sendPrefixMessage(player, "&cUnflagged items found in " + target.getName() + "'s inventory:");
                    listViolatingItems(player, violatingItems);
                }
            } else {
                if (violatingItems.isEmpty()) {
                    MessageUtil.sendPrefixMessage(player, "&aNo flagged items found in " + target.getName() + "'s inventory.");
                } else {
                    MessageUtil.sendPrefixMessage(player, "&cFlagged items found in " + target.getName() + "'s inventory:");
                    listViolatingItems(player, violatingItems);
                }
            }

            ConsoleUtil.sendInfo("Admin " + player.getName() + " inspected " + target.getName() + "'s inventory.");
        } catch (Exception e) {
            MessageUtil.sendPrefixMessage(player, "&cAn error occurred while inspecting the inventory.");
            ConsoleUtil.sendError("Error in InspectInventoryCommand: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Error in InspectInventoryCommand", e);
        }

        return true;
    }

    /**
     * Lists the violating items to the sender.
     *
     * @param sender The command sender.
     * @param items  The list of violating items.
     */
    private void listViolatingItems(Player sender, List<ItemStack> items) {
        for (ItemStack item : items) {
            String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    ? item.getItemMeta().getDisplayName()
                    : item.getType().toString();
            MessageUtil.sendPrefixMessage(sender, "&7- &f" + itemName + " &7x" + item.getAmount());
        }
    }
}