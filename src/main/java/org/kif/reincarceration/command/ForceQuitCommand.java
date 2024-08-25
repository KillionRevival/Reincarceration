package org.kif.reincarceration.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kif.reincarceration.cycle.CycleManager;
import org.kif.reincarceration.cycle.CycleModule;
import org.kif.reincarceration.util.MessageUtil;
import org.kif.reincarceration.util.ConsoleUtil;

import java.util.logging.Level;

/**
 * Command to forcefully remove a player from their current reincarceration cycle.
 */
public class ForceQuitCommand implements CommandExecutor {
    private final CycleManager cycleManager;
    private final CycleModule cycleModule;

    /**
     * Constructs a new ForceQuitCommand.
     *
     * @param cycleModule The cycle module instance.
     */
    public ForceQuitCommand(CycleModule cycleModule) {
        this.cycleModule = cycleModule;
        this.cycleManager = cycleModule.getCycleManager();
    }

    /**
     * Executes the force quit command.
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

        if (!player.hasPermission("reincarceration.admin.forcequit") && !player.hasPermission("reincarceration.admin")) {
            MessageUtil.sendPrefixMessage(player, "&cYou don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            MessageUtil.sendPrefixMessage(player, "&cUsage: /forcequit <player>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            MessageUtil.sendPrefixMessage(player, "&cPlayer not found or not online.");
            return true;
        }

        try {
            if (!cycleManager.isPlayerInCycle(targetPlayer)) {
                MessageUtil.sendPrefixMessage(player, "&cThe specified player is not currently in a cycle.");
                return true;
            }

            cycleManager.quitCycle(targetPlayer);
            MessageUtil.sendPrefixMessage(player, "&aYou have forcefully removed " + targetPlayer.getName() + " from their cycle.");
            MessageUtil.sendPrefixMessage(targetPlayer, "&cAn admin has forcefully removed you from your cycle.");
            ConsoleUtil.sendInfo("Admin " + player.getName() + " forcefully removed " + targetPlayer.getName() + " from their cycle.");
        } catch (Exception e) {
            MessageUtil.sendPrefixMessage(player, "&cAn error occurred while trying to force quit the player's cycle.");
            ConsoleUtil.sendError("Error in ForceQuitCommand: " + e.getMessage());
            cycleModule.getPlugin().getLogger().log(Level.SEVERE, "Error in ForceQuitCommand", e);
        }

        return true;
    }
}