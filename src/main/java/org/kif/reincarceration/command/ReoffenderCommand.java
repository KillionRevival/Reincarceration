package org.kif.reincarceration.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kif.reincarceration.config.ConfigManager;
import org.kif.reincarceration.cycle.CycleManager;
import org.kif.reincarceration.cycle.CycleModule;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.data.DataModule;
import org.kif.reincarceration.economy.EconomyManager;
import org.kif.reincarceration.economy.EconomyModule;
import org.kif.reincarceration.util.MessageUtil;

import java.math.BigDecimal;
import java.sql.SQLException;

public class ReoffenderCommand implements CommandExecutor {
    private final CommandModule commandModule;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final CycleManager cycleManager;

    public ReoffenderCommand(CommandModule commandModule, ConfigManager configManager,
                             CycleModule cycleModule, DataModule dataModule, EconomyModule economyModule) {
        this.commandModule = commandModule;
        this.configManager = configManager;
        this.dataManager = dataModule.getDataManager();
        this.economyManager = economyModule.getEconomyManager();
        this.cycleManager = cycleModule.getCycleManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(configManager.getPrefix() + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("reincarceration.use")) {
            MessageUtil.sendPrefixMessage(player, "&cInsufficent Permissions");
            return true;
        }

        try {
            dataManager.createPlayerData(player);
            int currentRank = dataManager.getPlayerRank(player);
            BigDecimal balance = economyManager.getBalance(player);
            BigDecimal storedBalance = dataManager.getStoredBalance(player);
            String rankName = configManager.getRankName(currentRank);
            boolean inCycle = cycleManager.isPlayerInCycle(player);
            int cycleCount = dataManager.getPlayerCycleCount(player);

            String prefix = configManager.getPrefix();

            MessageUtil.sendMessage(player, "&4--- Reincarceration Profile ---");
            if (inCycle) {
                MessageUtil.sendMessage(player, "&4| &r&cCurrent Rank: " + rankName + " (Level " + currentRank + ")");
            }
            MessageUtil.sendMessage(player, "&4| &r&cCurrent Balance: &r&c" + balance);
            MessageUtil.sendMessage(player, "&4| &r&cStored Balance: &r&c" + storedBalance);
            MessageUtil.sendMessage(player, "&4| &r&cTotal Completed Cycles: &r&c" + cycleCount);
            MessageUtil.sendMessage(player, "&4| &r&cCurrently in Cycle: &r&c" + (inCycle ? "Yes" : "No"));

            if (inCycle) {
                if (currentRank < configManager.getRankUpCosts().size()) {
                    BigDecimal nextRankCost = configManager.getRankUpCost(currentRank);
                    MessageUtil.sendMessage(player, "&4| &r&cCost to rank up: &r&c" + nextRankCost);

                    if (economyManager.hasEnoughBalance(player, nextRankCost)) {
                        MessageUtil.sendMessage(player, "&4| &r&cYou have enough money to rank up! Use &n/rankup&r&c to proceed.");
                    } else {
                        MessageUtil.sendMessage(player, "&4| &r&cYou need " + nextRankCost.subtract(balance) + " more to rank up.");
                    }
                } else {
                    MessageUtil.sendMessage(player, "&4| &r&cYou have reached the maximum rank! Use &n/completecycle to finish your cycle.");
                }
            } else {
                BigDecimal entryFee = configManager.getEntryFee();
                MessageUtil.sendMessage(player, "&4| &r&cEntry fee for new cycle: &r&c" + entryFee);
                if (balance.compareTo(entryFee) >= 0) {
                    MessageUtil.sendMessage(player, "&4| &r&cYou can start a new cycle with &n/startcycle");
                } else {
                    MessageUtil.sendMessage(player, "&4| &r&cYou need " + entryFee.subtract(balance) + " more to start a new cycle.");
                }
            }
        } catch (SQLException e) {
            MessageUtil.sendPrefixMessage(player, "&cAn error occurred while retrieving your reoffender information. Please try again later.");
            commandModule.getPlugin().getLogger().severe("Error in ReoffenderCommand: " + e.getMessage());
        }

        return true;
    }
}