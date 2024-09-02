package org.kif.reincarceration.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.config.ConfigManager;

public class MessageUtil {

    private static Reincarceration plugin;
    private static ConfigManager configManager;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static void initialize(Reincarceration plugin) {
        MessageUtil.plugin = plugin;
        MessageUtil.configManager = plugin.getModuleManager().getConfigManager();
    }

    public static void sendMessage(Player player, String message) {
        Component formattedMessage = formatMessage(message, false);
        player.sendMessage(formattedMessage);
    }

    public static void sendPrefixMessage(Player player, String message) {
        Component formattedMessage = formatMessage(message, true);
        player.sendMessage(formattedMessage);
    }

    private static Component formatMessage(String message, boolean prefixToggle) {
        Component messageComponent = miniMessage.deserialize(message);

        if (prefixToggle) {
            Component prefix = configManager.getPrefix();
            return Component.empty().append(prefix).append(Component.space()).append(messageComponent);
        } else {
            return messageComponent;
        }
    }
}