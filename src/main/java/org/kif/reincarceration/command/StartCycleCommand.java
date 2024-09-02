package org.kif.reincarceration.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.kif.reincarceration.config.ConfigManager;
import org.kif.reincarceration.cycle.CycleManager;
import org.kif.reincarceration.cycle.CycleModule;
import org.kif.reincarceration.modifier.core.IModifier;
import org.kif.reincarceration.modifier.core.ModifierManager;
import org.kif.reincarceration.modifier.core.ModifierRegistry;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.MessageUtil;

import java.sql.SQLException;

public class StartCycleCommand implements CommandExecutor {
    private final CommandModule commandModule;
    private final ConfigManager configManager;
    private final CycleManager cycleManager;
    private final ModifierManager modifierManager;
    private final ModifierRegistry modifierRegistry;

    public StartCycleCommand(CommandModule commandModule, ConfigManager configManager,
                             CycleModule cycleModule, ModifierManager modifierManager,
                             ModifierRegistry modifierRegistry) {
        this.commandModule = commandModule;
        this.configManager = configManager;
        this.cycleManager = cycleModule.getCycleManager();
        this.modifierManager = modifierManager;
        this.modifierRegistry = modifierRegistry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getPrefix() + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("reincarceration.startcycle")) {
            MessageUtil.sendPrefixMessage(player, "<red>Insufficent Permissions");
            return true;
        }

        if (cycleManager.isPlayerInCycle(player)) {
            MessageUtil.sendPrefixMessage(player, "<red>Invalid: Cycle Presence");
            ConsoleUtil.sendError("Player " + player.getName() + " is already in a cycle but has permissions! Review permissions!");
            return true;
        }

        if (args.length < 1) {
            MessageUtil.sendPrefixMessage(player, "<red>Usage: /startcycle <modifier>");
            return true;
        }

        String modifierId = args[0];
        IModifier modifier = modifierRegistry.getModifier(modifierId);

        if (modifier == null) {
            MessageUtil.sendPrefixMessage(player, "<red>Invalid modifier. Use /listmodifiers to see available modifiers.");
            return true;
        }

        try {
            if (!modifierManager.canUseModifier(player, modifier)) {
                MessageUtil.sendPrefixMessage(player, "<red>You have already completed this modifier. Choose a different one.");
                return true;
            }

            cycleManager.startNewCycle(player, modifier);

        } catch (SQLException e) {
            MessageUtil.sendPrefixMessage(player, "<red>An error occurred while starting the cycle. Please try again later.");
            commandModule.getPlugin().getLogger().severe("Error in StartCycleCommand: " + e.getMessage());
        }

        return true;
    }
}