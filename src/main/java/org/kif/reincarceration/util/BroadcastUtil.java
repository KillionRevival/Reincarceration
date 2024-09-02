package org.kif.reincarceration.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.config.ConfigManager;

public class BroadcastUtil {

    private static Reincarceration plugin;
    private static ConfigManager configManager;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Initializes the BroadcastUtil with the plugin instance.
     *
     * @param plugin The Reincarceration plugin instance
     */
    public static void initialize(Reincarceration plugin) {
        BroadcastUtil.plugin = plugin;
        BroadcastUtil.configManager = plugin.getModuleManager().getConfigManager();
    }

    /**
     * Broadcasts a message to all online players.
     *
     * @param message The message to broadcast
     */
    public static void broadcastMessage(String message) {
        Component formattedMessage = formatMessage(message);
        Bukkit.getServer().sendMessage(formattedMessage);
    }

    /**
     * Broadcasts a message to all online players with a specific permission.
     *
     * @param message The message to broadcast
     * @param permission The permission required to receive the broadcast
     */
    public static void broadcastMessageWithPermission(String message, String permission) {
        Component formattedMessage = formatMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(formattedMessage);
            }
        }
    }

    /**
     * Broadcasts a message to all online players except the specified player.
     *
     * @param message The message to broadcast
     * @param excludedPlayer The player to exclude from the broadcast
     */
    public static void broadcastMessageExcept(String message, Player excludedPlayer) {
        Component formattedMessage = formatMessage(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(excludedPlayer)) {
                player.sendMessage(formattedMessage);
            }
        }
    }

    /**
     * Formats the message with the prefix and parses MiniMessage format.
     *
     * @param message The message to format
     * @return The formatted message as a Component
     */
    private static Component formatMessage(String message) {
        Component prefix = configManager.getPrefix();
        Component messageComponent = miniMessage.deserialize(message);
        return Component.empty().append(prefix).append(Component.space()).append(messageComponent);
    }
}