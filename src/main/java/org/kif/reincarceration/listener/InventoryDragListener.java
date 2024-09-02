package org.kif.reincarceration.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.permission.PermissionManager;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.ItemUtil;
import org.kif.reincarceration.util.MessageUtil;

public class InventoryDragListener implements Listener {
    private final Reincarceration plugin;
    private final PermissionManager permissionManager;

    public InventoryDragListener(Reincarceration plugin) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        boolean isAssociated = permissionManager.isAssociatedWithBaseGroup(player.getUniqueId());

        if (isAssociated) {
            for (Integer slot : event.getRawSlots()) {
                if (slot >= player.getInventory().getSize()) {
                    // This slot is outside the player's inventory
                    ItemStack draggedItem = event.getNewItems().get(slot);
                    if (draggedItem != null && !draggedItem.getType().isAir() && !ItemUtil.hasReincarcerationFlag(draggedItem)) {
                        event.setCancelled(true);
                        MessageUtil.sendPrefixMessage(player, "<red>You cannot drag unflagged items outside your inventory.");
                        ConsoleUtil.sendDebug("Cancelled inventory drag for " + player.getName() + " involving unflagged item: " + draggedItem.getType().name());
                        return;
                    }
                }
            }
        }
    }
}
