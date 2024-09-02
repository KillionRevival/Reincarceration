package org.kif.reincarceration.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kif.reincarceration.config.ConfigManager;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.modifier.core.IModifier;
import org.kif.reincarceration.modifier.core.ModifierRegistry;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.MessageUtil;

import java.sql.SQLException;
import java.util.List;

public class ListModifiersCommand implements CommandExecutor {
    private final CommandModule commandModule;
    private final ConfigManager configManager;
    private final ModifierRegistry modifierRegistry;
    private final DataManager dataManager;

    public ListModifiersCommand(CommandModule commandModule, ConfigManager configManager, ModifierRegistry modifierRegistry, DataManager dataManager) {
        this.commandModule = commandModule;
        this.configManager = configManager;
        this.modifierRegistry = modifierRegistry;
        this.dataManager = dataManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getPrefix() + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("reincarceration.listmodifiers")) {
            MessageUtil.sendPrefixMessage(player, "<red>Insufficent Permissions");
            ConsoleUtil.sendError("Player " + player.getName() + " does not have permission to use /listmodifiers! Review permissions!");
            return true;
        }

        try {
            List<String> completedModifiers = dataManager.getCompletedModifiers(player);
            List<IModifier> availableModifiers = modifierRegistry.getAvailableModifiers(completedModifiers);

            if (availableModifiers.isEmpty()) {
                MessageUtil.sendPrefixMessage(player, "<dark_green>You have completed all available modifiers!");
            } else {

                MessageUtil.sendPrefixMessage(player, "<dark_green>Available modifiers:");
                for (IModifier modifier : availableModifiers) {
                    MessageUtil.sendPrefixMessage(player, "<dark_green>- " + modifier.getName() + " (" + modifier.getId() + "): " + modifier.getDescription());
                }
            }
        } catch (SQLException e) {
            MessageUtil.sendPrefixMessage(player, "<red>An error occurred while retrieving modifiers. Please try again later.");
            commandModule.getPlugin().getLogger().severe("Error in ListModifiersCommand: " + e.getMessage());
        }

        return true;
    }
}