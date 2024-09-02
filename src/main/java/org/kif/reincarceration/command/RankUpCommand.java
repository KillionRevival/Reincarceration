package org.kif.reincarceration.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kif.reincarceration.config.ConfigManager;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.data.DataModule;
import org.kif.reincarceration.economy.EconomyManager;
import org.kif.reincarceration.economy.EconomyModule;
import org.kif.reincarceration.rank.RankManager;
import org.kif.reincarceration.rank.RankModule;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.MessageUtil;

public class RankUpCommand implements CommandExecutor {
    private final CommandModule commandModule;
    private final ConfigManager configManager;
    private final RankManager rankManager;
    private final DataManager dataManager;
    private final EconomyManager economyManager;

    public RankUpCommand(CommandModule commandModule, ConfigManager configManager,
                         RankModule rankModule, DataModule dataModule, EconomyModule economyModule) {
        this.commandModule = commandModule;
        this.configManager = configManager;
        this.rankManager = rankModule.getRankManager();
        this.dataManager = dataModule.getDataManager();
        this.economyManager = economyModule.getEconomyManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getPrefix() + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("reincarceration.rankup")) {
            MessageUtil.sendPrefixMessage(player, "<red>Insufficent Permissions");
            ConsoleUtil.sendError("Player " + player.getName() + " does not have permission to use /rankup but could execute command. Review permissions to ensure they are correct.");
            return true;
        }

        if (!rankManager.canRankUp(player)) {
            return true;
        }

        rankManager.rankUp(player);

        return true;
    }
}