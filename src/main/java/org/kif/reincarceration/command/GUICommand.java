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
import org.kif.reincarceration.gui.GUIModule;
import org.kif.reincarceration.util.MessageUtil;
import org.kif.reincarceration.util.ConsoleUtil;

import java.util.logging.Level;

/**
 * Command to open the main GUI for the Reincarceration plugin.
 */
public class GUICommand implements CommandExecutor {
    private final CommandModule commandModule;
    private final ConfigManager configManager;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final CycleManager cycleManager;
    private final GUIModule guiModule;

    /**
     * Constructs a new GUICommand.
     *
     * @param commandModule The command module instance.
     * @param configManager The config manager instance.
     * @param cycleModule   The cycle module instance.
     * @param dataModule    The data module instance.
     * @param economyModule The economy module instance.
     * @param guiModule     The GUI module instance.
     */
    public GUICommand(CommandModule commandModule, ConfigManager configManager,
                      CycleModule cycleModule, DataModule dataModule, EconomyModule economyModule, GUIModule guiModule) {
        this.commandModule = commandModule;
        this.configManager = configManager;
        this.dataManager = dataModule.getDataManager();
        this.economyManager = economyModule.getEconomyManager();
        this.cycleManager = cycleModule.getCycleManager();
        this.guiModule = guiModule;
    }

    /**
     * Executes the GUI command.
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
            sender.sendMessage(configManager.getPrefix() + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("reincarceration.gui")) {
            MessageUtil.sendPrefixMessage(player, "<red>You don't have permission to use this command.");
            return true;
        }

        try {
            if (guiModule != null) {
                guiModule.getGuiManager().openMainMenu(player);
                ConsoleUtil.sendInfo("Player " + player.getName() + " opened the Reincarceration GUI.");
            } else {
                MessageUtil.sendPrefixMessage(player, "<red>GUI system is currently unavailable.");
                ConsoleUtil.sendError("GUI Module is null when player " + player.getName() + " tried to open the GUI.");
            }
        } catch (Exception e) {
            MessageUtil.sendPrefixMessage(player, "<red>An error occurred while trying to open the GUI.");
            ConsoleUtil.sendError("Error in GUICommand: " + e.getMessage());
            commandModule.getPlugin().getLogger().log(Level.SEVERE, "Error in GUICommand", e);
        }

        return true;
    }
}