package org.kif.reincarceration.util;

import co.killionrevival.killioncommons.util.TextFormatUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.config.ConfigManager;

public class MessageUtil {

    private static Reincarceration plugin;
    private static ConfigManager configManager;

    public static void initialize(Reincarceration plugin) {
        MessageUtil.plugin = plugin;
        MessageUtil.configManager = plugin.getModuleManager().getConfigManager();
    }

    public static void sendMessage(Player player, String message) {
        player.sendMessage(formatMessage(message, false));
    }

    public static void sendPrefixMessage(Player player, String message) {
        player.sendMessage(formatMessage(message, true));
    }

    private static Component formatMessage(String message, Boolean prefix_toggle) {
        if (prefix_toggle) {
            String prefix = configManager.getPrefix() + " ";
            return TextFormatUtil.getComponentFromLegacyString(prefix + message);
        } else {
            return TextFormatUtil.getComponentFromLegacyString(message);
        }
    }
}