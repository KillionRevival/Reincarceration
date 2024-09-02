package org.kif.reincarceration.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.config.ConfigManager;

public class ConsoleUtil {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    private static Reincarceration plugin;
    private static ConfigManager configManager;

    public static void initialize(Reincarceration plugin) {
        ConsoleUtil.plugin = plugin;
        ConsoleUtil.configManager = plugin.getModuleManager().getConfigManager();
    }

    public static void sendFormatMessage(String message) {
        Component prefix = configManager.getPrefix();
        Component formattedMessage = prefix.append(miniMessage.deserialize(message));
        Bukkit.getConsoleSender().sendMessage(formattedMessage);
    }

    public static void sendInfo(String message) {
        sendFormatMessage("<aqua>" + message);
    }

    public static void sendError(String message) {
        sendFormatMessage("<red>" + message);
    }

    public static void sendSuccess(String message) {
        sendFormatMessage("<dark_green>" + message);
    }

    public static void sendDebug(String message) {
        boolean debugMode = configManager.isDebugMode();
        if (debugMode) {
            sendFormatMessage("<light_purple>" + message);
        }
    }
}