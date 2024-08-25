package org.kif.reincarceration.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.ItemUtil;
import org.kif.reincarceration.util.MessageUtil;

import java.util.logging.Level;

/**
 * This command allows administrators to add a reincarceration flag to the item they are holding.
 * The flag is used to mark items as part of the reincarceration system.
 */
public class FlagItemCommand implements CommandExecutor {

    private final Reincarceration plugin;

    /**
     * Constructs a new FlagItemCommand.
     *
     * @param plugin The main plugin instance.
     */
    public FlagItemCommand(Reincarceration plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the flag item command.
     *
     * @param sender  The sender of the command.
     * @param command The command that was executed.
     * @param label   The alias of the command used.
     * @param args    The arguments passed to the command.
     * @return true if the command was successful, false otherwise.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ConsoleUtil.sendError("This command can only be used by players.");
            return true;
        }

        if (!player.isOp() && (!player.hasPermission("reincarceration.admin.flagitem") || !player.hasPermission("reincarceration.admin"))) {
            MessageUtil.sendPrefixMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType().isAir()) {
            MessageUtil.sendPrefixMessage(player, "&cYou must be holding an item to flag it.");
            return true;
        }

        try {
            if (ItemUtil.hasReincarcerationFlag(itemInHand)) {
                MessageUtil.sendPrefixMessage(player, "&cThis item is already flagged.");
                return true;
            }

            ItemUtil.addReincarcerationFlag(itemInHand);
            MessageUtil.sendPrefixMessage(player, "&aSuccessfully added flag to the item in your hand.");
            ConsoleUtil.sendInfo("Admin " + player.getName() + " flagged item: " + itemInHand.getType());
        } catch (Exception e) {
            MessageUtil.sendPrefixMessage(player, "&cAn error occurred while trying to flag the item.");
            ConsoleUtil.sendError("Error in FlagItemCommand: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Error in FlagItemCommand", e);
        }

        return true;
    }
}