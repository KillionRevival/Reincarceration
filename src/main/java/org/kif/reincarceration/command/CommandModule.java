package org.kif.reincarceration.command;

import lombok.Getter;
import org.bukkit.command.PluginCommand;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.config.ConfigManager;
import org.kif.reincarceration.core.Module;
import org.kif.reincarceration.cycle.CycleModule;
import org.kif.reincarceration.data.DataModule;
import org.kif.reincarceration.economy.EconomyModule;
import org.kif.reincarceration.modifier.core.ModifierModule;
import org.kif.reincarceration.rank.RankModule;
import org.kif.reincarceration.gui.GUIModule;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.rewards.RewardModule;

import java.util.logging.Level;

/**
 * The CommandModule is responsible for registering and managing all commands for the Reincarceration plugin.
 * It implements the Module interface to integrate with the plugin's modular structure.
 */
public class CommandModule implements Module {
    /**
     * -- GETTER --
     *  Gets the main plugin instance.
     */
    @Getter
    private final Reincarceration plugin;
    private final ConfigManager configManager;

    /**
     * Constructs a new CommandModule.
     *
     * @param plugin The main plugin instance.
     */
    public CommandModule(Reincarceration plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getModuleManager().getConfigManager();
    }

    /**
     * Enables the CommandModule, registering all commands.
     */
    @Override
    public void onEnable() {
        try {
            registerCommands();
            ConsoleUtil.sendSuccess("Command Module enabled");
        } catch (Exception e) {
            ConsoleUtil.sendError("Failed to enable Command Module: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Error enabling Command Module", e);
        }
    }

    /**
     * Disables the CommandModule.
     */
    @Override
    public void onDisable() {
        ConsoleUtil.sendSuccess("Command Module disabled");
    }

    /**
     * Registers all commands for the plugin.
     */
    private void registerCommands() {
        try {
            CycleModule cycleModule = plugin.getModuleManager().getModule(CycleModule.class);
            DataModule dataModule = plugin.getModuleManager().getModule(DataModule.class);
            EconomyModule economyModule = plugin.getModuleManager().getModule(EconomyModule.class);
            RankModule rankModule = plugin.getModuleManager().getModule(RankModule.class);
            ModifierModule modifierModule = plugin.getModuleManager().getModule(ModifierModule.class);
            GUIModule guiModule = plugin.getModuleManager().getModule(GUIModule.class);
            RewardModule rewardModule = plugin.getModuleManager().getModule(RewardModule.class);

            registerCommand("rc", new GUICommand(this, configManager, cycleModule, dataModule, economyModule, guiModule));
            registerCommand("flagitem", new FlagItemCommand(plugin));
            registerCommand("inspectitem", new InspectItemCommand(plugin));
            registerCommand("inspectinventory", new InspectInventoryCommand(plugin));
            registerCommand("viewplayerdata", new ViewPlayerDataCommand(plugin));
            registerCommand("completeCycle", new CompleteCycleCommand(cycleModule));
            registerCommand("rcreloadtags", new ReloadTagsCommand(plugin));
            registerCommand("forcequit", new ForceQuitCommand(cycleModule));
            registerCommand("forcereset", new ForceResetCommand(cycleModule, dataModule));
            registerCommand("rcreward", new RewardCommand(plugin));

        } catch (Exception e) {
            ConsoleUtil.sendError("Error registering commands: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Error registering commands", e);
        }
    }

    /**
     * Registers a single command with the plugin.
     *
     * @param name     The name of the command.
     * @param executor The CommandExecutor for the command.
     */
    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
            ConsoleUtil.sendDebug("Registered command: " + name);
        } else {
            ConsoleUtil.sendError("Failed to register command: " + name);
        }
    }
}