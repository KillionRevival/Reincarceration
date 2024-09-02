package org.kif.reincarceration.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kif.reincarceration.cycle.CycleManager;
import org.kif.reincarceration.cycle.CycleModule;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.data.DataModule;
import org.kif.reincarceration.util.MessageUtil;
import org.kif.reincarceration.util.ConsoleUtil;

import java.util.logging.Level;

/**
 * Command to forcefully reset a player's reincarceration data and remove them from the system.
 */
public class ForceResetCommand implements CommandExecutor {
    private final CycleManager cycleManager;
    private final DataManager dataManager;
    private final CycleModule cycleModule;

    /**
     * Constructs a new ForceResetCommand.
     *
     * @param cycleModule The cycle module instance.
     * @param dataModule  The data module instance.
     */
    public ForceResetCommand(CycleModule cycleModule, DataModule dataModule) {
        this.cycleModule = cycleModule;
        this.cycleManager = cycleModule.getCycleManager();
        this.dataManager = dataModule.getDataManager();
    }

    /**
     * Executes the force reset command.
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

        if (!player.hasPermission("reincarceration.admin.forcereset") && !player.hasPermission("reincarceration.admin")) {
            MessageUtil.sendPrefixMessage(player, "<red>You don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            MessageUtil.sendPrefixMessage(player, "<red>Usage: /forcereset <player>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            MessageUtil.sendPrefixMessage(player, "<red>Player not found or not online.");
            return true;
        }

        try {
            ConsoleUtil.sendDebug("Attempting to force reset player: " + targetPlayer.getName() + " (UUID: " + targetPlayer.getUniqueId() + ")");

            if (cycleManager.isPlayerInCycle(targetPlayer)) {
                ConsoleUtil.sendDebug("Player is in cycle. Quitting cycle for: " + targetPlayer.getName());
                cycleManager.quitCycle(targetPlayer);
            } else {
                ConsoleUtil.sendDebug("Player is not in a cycle: " + targetPlayer.getName());
            }

            ConsoleUtil.sendDebug("Clearing player data for: " + targetPlayer.getName());
            dataManager.clearPlayerData(targetPlayer);
            ConsoleUtil.sendDebug("Reinitializing player data for: " + targetPlayer.getName());
            dataManager.createPlayerData(targetPlayer);

            MessageUtil.sendPrefixMessage(player, "<green>You have forcefully reset " + targetPlayer.getName() + "'s data and removed them from the reincarceration system.");
            MessageUtil.sendPrefixMessage(targetPlayer, "<red>An admin has forcefully reset your reincarceration data.");
            ConsoleUtil.sendInfo("Admin " + player.getName() + " has forcefully reset " + targetPlayer.getName() + "'s reincarceration data.");

        } catch (Exception e) {
            MessageUtil.sendPrefixMessage(player, "<red>An error occurred while resetting the player's data.");
            ConsoleUtil.sendError("Error in ForceResetCommand for player " + targetPlayer.getName() + " (UUID: " + targetPlayer.getUniqueId() + "): " + e.getMessage());
            cycleModule.getPlugin().getLogger().log(Level.SEVERE, "Error in ForceResetCommand", e);
        }

        return true;
    }
}