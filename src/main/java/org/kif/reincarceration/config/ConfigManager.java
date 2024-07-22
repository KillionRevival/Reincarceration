package org.kif.reincarceration.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.kif.reincarceration.Reincarceration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class ConfigManager {
    private final Reincarceration plugin;
    private final FileConfiguration config;

    public ConfigManager(Reincarceration plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void reloadConfig() {
        plugin.reloadConfig();
    }

    public BigDecimal getEntryFee() {
        return BigDecimal.valueOf(config.getDouble("economy.entry-fee", 1000));
    }

    public List<Integer> getRankUpCosts() {
        return config.getIntegerList("economy.rank-up-costs");
    }

    public BigDecimal getRankUpCost(int rank) {
        List<Integer> costs = getRankUpCosts();
        return rank < costs.size() ? BigDecimal.valueOf(costs.get(rank)) : BigDecimal.valueOf(Double.MAX_VALUE);
    }

    public String getRankName(int rank) {
        return config.getString("ranks." + rank + ".name", "Rank " + rank);
    }

    public String getRankPermissionGroup(int rank) {
        return config.getString("ranks." + rank + ".permission-group", "reoffender_rank_" + rank);
    }

    public String getDatabaseFilename() {
        return config.getString("database.filename", "reincarceration_data.db");
    }

    public String getEntryGroup() {
        return config.getString("permissions.entry-group", "citizen");
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("prefix", "&8[&6Reincarceration&8] &r"));
    }

    public boolean isDebugMode() {
        return config.getBoolean("debug-mode", false);
    }

    public FileConfiguration getRawConfig() {
        return config;
    }

    public int getMaxRank() {
        return Objects.requireNonNull(config.getConfigurationSection("ranks")).getKeys(false).size() - 1;
    }

    public boolean isMaxRank(int rank) {
        return rank == getMaxRank();
    }

    public String getBaseGroup() {
        return config.getString("permissions.base-group", "reoffender");
    }

    public int getReoffenderVaultNumber() {
        return config.getInt("reoffender-vault-number", 11); // Default to vault 1 if not specified
    }
}