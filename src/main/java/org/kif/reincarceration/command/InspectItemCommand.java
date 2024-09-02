package org.kif.reincarceration.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.ItemUtil;
import org.kif.reincarceration.util.MessageUtil;

public class InspectItemCommand implements CommandExecutor {

    private final Reincarceration plugin;

    public InspectItemCommand(Reincarceration plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            ConsoleUtil.sendError("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp() && !player.hasPermission("reincarceration.admin.inspectitem")) {
            MessageUtil.sendPrefixMessage(player, "<red>You don't have permission to use this command.");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType().isAir()) {
            MessageUtil.sendPrefixMessage(player, "<red>You must be holding an item to inspect it.");
            return true;
        }

        boolean hasFlag = ItemUtil.hasReincarcerationFlag(itemInHand);

        if (hasFlag) {
            MessageUtil.sendPrefixMessage(player, "<green>The item in your hand <dark_green>IS flagged <green>for the reincarceration system.");
        } else {
            MessageUtil.sendPrefixMessage(player, "<red>The item in your hand <dark_red>IS NOT flagged <red>for the reincarceration system.");
        }

        // Additional item information
        MessageUtil.sendPrefixMessage(player, "<yellow>Item Details:");
        MessageUtil.sendPrefixMessage(player, "<gray>- Type: <white>" + itemInHand.getType());
        MessageUtil.sendPrefixMessage(player, "<gray>- Amount: <white>" + itemInHand.getAmount());
        if (itemInHand.hasItemMeta()) {
            ItemMeta meta = itemInHand.getItemMeta();
            if (meta.hasDisplayName()) {
                MessageUtil.sendPrefixMessage(player, "<gray>- Display Name: <white>" + meta.getDisplayName());
            }
            if (meta.hasLore()) {
                MessageUtil.sendPrefixMessage(player, "<gray>- Lore:");
                for (String loreLine : meta.getLore()) {
                    MessageUtil.sendPrefixMessage(player, "<gray>  <white>" + loreLine);
                }
            }
        }

        return true;
    }
}