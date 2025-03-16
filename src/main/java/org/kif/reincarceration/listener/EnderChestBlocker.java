package org.kif.reincarceration.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.permission.PermissionManager;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.MessageUtil;

public class EnderChestBlocker implements Listener {
    private final Reincarceration plugin;
    private final PermissionManager permissionManager;

    public EnderChestBlocker(Reincarceration plugin) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();

        // Skip if player is not reincarcerated
        if (!permissionManager.isAssociatedWithBaseGroup(player.getUniqueId())) {
            return;
        }

        // Explicitly check for ENDER_CHEST type
        if (event.getInventory().getType() == InventoryType.ENDER_CHEST) {
            event.setCancelled(true);
            MessageUtil.sendPrefixMessage(player, "&cYou cannot access ender chests while reincarcerated.");
            ConsoleUtil.sendDebug("Blocked ender chest access for reincarcerated player: " + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Skip if player is not reincarcerated
        if (!permissionManager.isAssociatedWithBaseGroup(player.getUniqueId())) {
            return;
        }

        String cmd = event.getMessage().toLowerCase().split("\\s+")[0];

        // Block enderchest commands
        if (cmd.equals("/enderchest") || cmd.equals("/echest") || cmd.equals("/ec")) {
            event.setCancelled(true);
            MessageUtil.sendPrefixMessage(player, "&cYou cannot access ender chests while reincarcerated.");
            ConsoleUtil.sendDebug("Blocked enderchest command for reincarcerated player: " + player.getName() + " - Command: " + cmd);
        }
    }
}