package org.kif.reincarceration.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.permission.PermissionManager;
import org.kif.reincarceration.util.ContainerViewerTracker;
import org.kif.reincarceration.util.ItemUtil;
import org.kif.reincarceration.util.MessageUtil;
import org.kif.reincarceration.util.ConsoleUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContainerInteractionListener implements Listener {
    private final Reincarceration plugin;
    private final PermissionManager permissionManager;
    private final List<String> blacklistedContainers;
    private final List<String> allowedContainerTitlePatterns;
    private final Map<Player, BukkitTask> checkTasks = new HashMap<>();
    private static final long CHECK_INTERVAL = 5L;

    public ContainerInteractionListener(Reincarceration plugin) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin);
        this.blacklistedContainers = plugin.getConfig().getStringList("blacklisted_containers");
        this.allowedContainerTitlePatterns = plugin.getConfig().getStringList("allowed_container_title_patterns");
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        if (player.isOp()) return;

        boolean isAssociated = permissionManager.isAssociatedWithBaseGroup(player.getUniqueId());

        Inventory inventory = event.getInventory();

        // Ignore player inventory, crafting tables, and certain other inventory types
        if (shouldIgnoreInventory(inventory)) {
            return;
        }

        if (isAssociated) {
            if (isBlacklistedInventory(inventory)) {
                event.setCancelled(true);
                MessageUtil.sendPrefixMessage(player, "<red>This container has been blacklisted from you.");
                ConsoleUtil.sendDebug("Blocked blacklisted inventory open for " + player.getName() + ": " + inventory.getType());
                return;
            }

            if (isAllowedContainer(inventory)) {
                return;
            }

            // Check player's inventory for unflagged items
            if (playerHasUnflaggedItems(player)) {
                event.setCancelled(true);
                MessageUtil.sendPrefixMessage(player, "<red>You have prohibitted items on your person. Please remove them before accessing this container.");
                ConsoleUtil.sendDebug("Blocked inventory open for " + player.getName() + ": player has unflagged items");
                return;
            }

            if (containsUnflaggedItem(inventory)) {
                event.setCancelled(true);
                MessageUtil.sendPrefixMessage(player, "<red>This container has prohibited contents.");
                ConsoleUtil.sendDebug("Blocked inventory open for " + player.getName() + ": contains unflagged items");
                return;
            }

            // Start a repeating task to check for unflagged items
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (player.getOpenInventory().getTopInventory().equals(inventory)) {
                    if (containsUnflaggedItem(inventory) || playerHasUnflaggedItems(player)) {
                        player.closeInventory();
                        MessageUtil.sendPrefixMessage(player, "<red>This container has been closed due to prohibitted items detection.");
                        ConsoleUtil.sendDebug("Closed inventory for " + player.getName() + ": unflagged items detected");
                    }
                } else {
                    // If the player is no longer viewing this inventory, cancel the task
                    checkTasks.remove(player).cancel();
                }
            }, CHECK_INTERVAL, CHECK_INTERVAL);

            checkTasks.put(player, task);
        } else {
            removeFlagsFromInventory(inventory);
            ConsoleUtil.sendDebug("Removed flags from inventory for non-associated player: " + player.getName());
        }

        // Add player to container viewers
        ContainerViewerTracker.addViewer(inventory, player);
        logContainerViewers(inventory);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();

        // Remove player from container viewers
        ContainerViewerTracker.removeViewer(inventory, player);
        logContainerViewers(inventory);

        // Cancel the check task if it exists
        BukkitTask task = checkTasks.remove(player);
        if (task != null) {
            task.cancel();
        }
    }

    private boolean isBlacklistedInventory(Inventory inventory) {
        return blacklistedContainers.contains(inventory.getType().name());
    }

    private boolean isAllowedContainer(Inventory inventory) {
        // Check if the inventory holder is from EconomyShopGUI
        // This method might need to be adjusted based on how EconomyShopGUI implements its inventories
        if (inventory.getHolder() == null) {
            return false;
        }
        ConsoleUtil.sendDebug("Inventory Name: " + inventory.getHolder().getClass().getName());
        ConsoleUtil.sendDebug("Simple Name: " + inventory.getHolder().getClass().getSimpleName());
        String title = inventory.getHolder().getClass().getName();

        for (String pattern : allowedContainerTitlePatterns) {
            if (title.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    private boolean shouldIgnoreInventory(Inventory inventory) {
        InventoryType type = inventory.getType();
        return inventory.getHolder() == null ||
                type == InventoryType.CRAFTING ||
                type == InventoryType.ANVIL ||
                type == InventoryType.ENCHANTING ||
                type == InventoryType.SMITHING ||
                type == InventoryType.CREATIVE ||
                type == InventoryType.PLAYER;
    }

    private boolean containsUnflaggedItem(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir() && !ItemUtil.hasReincarcerationFlag(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean playerHasUnflaggedItems(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir() && !ItemUtil.hasReincarcerationFlag(item)) {
                return true;
            }
        }
        return false;
    }

    private void removeFlagsFromInventory(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir() && ItemUtil.hasReincarcerationFlag(item)) {
                ItemUtil.removeReincarcerationFlag(item);
            }
        }
    }

    private void logContainerViewers(Inventory inventory) {
        Set<Player> viewers = ContainerViewerTracker.getViewers(inventory);
        if (!viewers.isEmpty()) {
            ConsoleUtil.sendDebug("Current viewers of inventory " + inventory.getType() + ":");
            for (Player viewer : viewers) {
                ConsoleUtil.sendDebug("- " + viewer.getName() + " (Associated: " + permissionManager.isAssociatedWithBaseGroup(viewer.getUniqueId()) + ")");
            }
        }
    }
}