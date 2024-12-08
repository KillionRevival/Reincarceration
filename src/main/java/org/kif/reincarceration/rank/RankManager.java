package org.kif.reincarceration.rank;

import org.bukkit.entity.Player;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.config.ConfigManager;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.economy.EconomyManager;
import org.kif.reincarceration.permission.PermissionManager;
import org.kif.reincarceration.util.BroadcastUtil;
import org.kif.reincarceration.util.MessageUtil;

import java.math.BigDecimal;
import java.sql.SQLException;

public class RankManager {
    private final Reincarceration plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final PermissionManager permissionManager;

    public RankManager(Reincarceration plugin, ConfigManager configManager, DataManager dataManager,
                       EconomyManager economyManager, PermissionManager permissionManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
        this.permissionManager = permissionManager;
    }

    public int getPlayerRank(Player player) {
        try {
            return dataManager.getPlayerRank(player);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting player rank: " + e.getMessage());
            return 0;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canRankUp(Player player) {
        int currentRank = getPlayerRank(player);
        int maxRank = configManager.getMaxRank();
        if (currentRank >= maxRank) {  // Changed from maxRank to maxRank - 1
            MessageUtil.sendPrefixMessage(player, "&cYou are already at the highest rank for rank up. Expected to use /completecycle instead.");
            return false;
        }

        BigDecimal rankUpCost = configManager.getRankUpCost(currentRank);
        if (!economyManager.hasEnoughBalance(player, rankUpCost)) {
            MessageUtil.sendPrefixMessage(player, "&cYou do not have enough money to rank up.");
            return false;
        }

        return true;
    }

    public void rankUp(Player player) {
        if (!canRankUp(player)) {
            return;
        }

        int currentRank = getPlayerRank(player);
        BigDecimal rankUpCost = configManager.getRankUpCost(currentRank);

        if (economyManager.withdrawMoney(player, rankUpCost)) {
            try {
                int newRank = currentRank + 1;
                setPlayerRank(player, newRank);

                BroadcastUtil.broadcastMessage("&c" + player.getName() + " has ranked up to " + configManager.getRankName(newRank));

            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating player rank: " + e.getMessage());
                economyManager.depositMoney(player, rankUpCost);
                MessageUtil.sendPrefixMessage(player, "&cAn error occurred while trying to rank up. Your money has been refunded. Please try again later.");
            }
        } else {
            MessageUtil.sendPrefixMessage(player, "&cAn error occurred while trying to rank up. Please try again later.");
        }
    }

    public void setPlayerRank(Player player, int rank) throws SQLException {
        dataManager.updatePlayerRank(player, rank);
        permissionManager.updatePlayerRankGroup(player, rank);
    }
}