package org.kif.reincarceration.listener;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.permission.PermissionManager;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.ItemUtil;
import org.kif.reincarceration.util.MessageUtil;

import java.util.HashMap;
import java.util.Map;

public class InventoryCloseListener implements Listener {
    private final Reincarceration plugin;
    private final PermissionManager permissionManager;

    public InventoryCloseListener(Reincarceration plugin) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            ConsoleUtil.sendDebug("InventoryCloseListener: Event player is not a Player instance");
            return;
        }

        Player player = (Player) event.getPlayer();
        ConsoleUtil.sendDebug("InventoryCloseListener: Processing inventory close for player " + player.getName());

        if (!permissionManager.isAssociatedWithBaseGroup(player.getUniqueId())) {
            ConsoleUtil.sendDebug("InventoryCloseListener: Player " + player.getName() + " is not associated with reincarceration system");
            return;
        }

        PlayerInventory inventory = player.getInventory();
        Map<Integer, ItemStack> unflaggedItems = new HashMap<>();

        // Check main inventory
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (isUnflaggedItem(item)) {
                unflaggedItems.put(i, item.clone());
                inventory.setItem(i, null);
            }
        }

        // Check armor contents
        ItemStack[] armorContents = inventory.getArmorContents();
        for (int i = 0; i < armorContents.length; i++) {
            if (isUnflaggedItem(armorContents[i])) {
                unflaggedItems.put(100 + i, armorContents[i].clone());
                armorContents[i] = null;
            }
        }
        inventory.setArmorContents(armorContents);

        // Check off-hand item
        ItemStack offHandItem = inventory.getItemInOffHand();
        if (isUnflaggedItem(offHandItem)) {
            unflaggedItems.put(200, offHandItem.clone());
            inventory.setItemInOffHand(null);
        }

        // Drop unflagged items
        if (!unflaggedItems.isEmpty()) {
            Location dropLocation = player.getLocation();
            for (ItemStack item : unflaggedItems.values()) {
                player.getWorld().dropItemNaturally(dropLocation, item);
                ConsoleUtil.sendDebug("InventoryCloseListener: Dropped item - " + item.getType().name());
            }

            player.updateInventory();

            MessageUtil.sendPrefixMessage(player, "<red>Unflagged items have been removed from your inventory.");
            ConsoleUtil.sendDebug("InventoryCloseListener: Removed " + unflaggedItems.size() + " unflagged items from " + player.getName() + "'s inventory");
        } else {
            ConsoleUtil.sendDebug("InventoryCloseListener: No unflagged items found in " + player.getName() + "'s inventory");
        }
    }

    private boolean isUnflaggedItem(ItemStack item) {
        return item != null && !item.getType().isAir() && !ItemUtil.hasReincarcerationFlag(item);
    }
}