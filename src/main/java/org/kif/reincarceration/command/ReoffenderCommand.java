package org.kif.reincarceration.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("reincarceration.use")) {
            MessageUtil.sendPrefixMessage(player, "<red>Insufficient Permissions");
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

            MessageUtil.sendMessage(player, "<dark_red><bold>--- Reincarceration Profile ---</bold></dark_red>");
            if (inCycle) {
                MessageUtil.sendMessage(player, "<dark_red>| <red>Current Rank: " + rankName + " (Level " + currentRank + ")");
            }
            MessageUtil.sendMessage(player, "<dark_red>| <red>Current Balance: <reset><red>" + balance);
            MessageUtil.sendMessage(player, "<dark_red>| <red>Stored Balance: <reset><red>" + storedBalance);
            MessageUtil.sendMessage(player, "<dark_red>| <red>Total Completed Cycles: <reset><red>" + cycleCount);
            MessageUtil.sendMessage(player, "<dark_red>| <red>Currently in Cycle: <reset><red>" + (inCycle ? "Yes" : "No"));

            if (inCycle) {
                if (currentRank < configManager.getRankUpCosts().size()) {
                    BigDecimal nextRankCost = configManager.getRankUpCost(currentRank);
                    MessageUtil.sendMessage(player, "<dark_red>| <red>Cost to rank up: <reset><red>" + nextRankCost);

                    if (economyManager.hasEnoughBalance(player, nextRankCost)) {
                        MessageUtil.sendMessage(player, "<dark_red>| <red>You have enough money to rank up! Use <underline>/rankup</underline> to proceed.");
                    } else {
                        MessageUtil.sendMessage(player, "<dark_red>| <red>You need " + nextRankCost.subtract(balance) + " more to rank up.");
                    }
                } else {
                    MessageUtil.sendMessage(player, "<dark_red>| <red>You have reached the maximum rank! Use <underline>/completecycle</underline> to finish your cycle.");
                }
            } else {
                BigDecimal entryFee = configManager.getEntryFee();
                MessageUtil.sendMessage(player, "<dark_red>| <red>Entry fee for new cycle: <reset><red>" + entryFee);
                if (balance.compareTo(entryFee) >= 0) {
                    MessageUtil.sendMessage(player, "<dark_red>| <red>You can start a new cycle with <underline>/startcycle</underline>");
                } else {
                    MessageUtil.sendMessage(player, "<dark_red>| <red>You need " + entryFee.subtract(balance) + " more to start a new cycle.");
                }
            }
        } catch (SQLException e) {
            MessageUtil.sendPrefixMessage(player, "<red>An error occurred while retrieving your reoffender information. Please try again later.");
            commandModule.getPlugin().getLogger().severe("Error in ReoffenderCommand: " + e.getMessage());
        }

        return true;
    }
}