package org.kif.reincarceration.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.data.DataModule;
import org.kif.reincarceration.util.MessageUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ViewPlayerDataCommand implements CommandExecutor {

    private final DataManager dataManager;

    public ViewPlayerDataCommand(Reincarceration plugin) {
        this.dataManager = plugin.getModuleManager().getModule(DataModule.class).getDataManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (!sender.isOp() && !sender.hasPermission("reincarceration.admin.viewplayerdata")) {
            MessageUtil.sendPrefixMessage((Player) sender, "<red>You don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            MessageUtil.sendPrefixMessage((Player) sender, "<red>Usage: /viewplayerdata <player>");
            return true;
        }

        String playerName = args[0];
        Player targetPlayer = Bukkit.getPlayer(playerName);
        UUID playerUUID = targetPlayer != null ? targetPlayer.getUniqueId() : null;

        if (playerUUID == null) {
            MessageUtil.sendPrefixMessage((Player) sender, "<red>Player not found in the database.");
            return true;
        }

        try {
            displayPlayerData(sender, playerUUID, playerName);
        } catch (SQLException e) {
            MessageUtil.sendPrefixMessage((Player) sender, "<red>Error retrieving player data: " + e.getMessage());
        }

        return true;
    }

    private void displayPlayerData(CommandSender sender, UUID playerUUID, String playerName) throws SQLException {
        Player player = (Player) sender;

        MessageUtil.sendPrefixMessage(player, "<gold><bold>=== Player Data for " + playerName + " ===</bold>");
        MessageUtil.sendPrefixMessage(player, "<gray>UUID: <white>" + playerUUID);

        // Player Data
        int currentRank = dataManager.getPlayerRank(player);
        boolean inCycle = dataManager.isPlayerInCycle(player);
        int cycleCount = dataManager.getPlayerCycleCount(player);
        BigDecimal storedBalance = dataManager.getStoredBalance(player);

        MessageUtil.sendPrefixMessage(player, "<gray>Current Rank: <white>" + currentRank);
        MessageUtil.sendPrefixMessage(player, "<gray>In Cycle: <white>" + (inCycle ? "Yes" : "No"));
        MessageUtil.sendPrefixMessage(player, "<gray>Cycle Count: <white>" + cycleCount);
        MessageUtil.sendPrefixMessage(player, "<gray>Stored Balance: <white>" + storedBalance);

        // Active Modifier
        String activeModifier = dataManager.getActiveModifier(player);
        MessageUtil.sendPrefixMessage(player, "<gray>Active Modifier: <white>" + (activeModifier != null ? activeModifier : "None"));

        // Completed Modifiers
        List<String> completedModifiers = dataManager.getCompletedModifiers(player);
        MessageUtil.sendPrefixMessage(player, "<gray>Completed Modifiers:");
        if (completedModifiers.isEmpty()) {
            MessageUtil.sendPrefixMessage(player, "<white>  None");
        } else {
            for (String modifier : completedModifiers) {
                MessageUtil.sendPrefixMessage(player, "<gray>  - <white>" + modifier);
            }
        }
    }
}