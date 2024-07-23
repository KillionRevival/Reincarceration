package org.kif.reincarceration.listener;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.modifier.core.ModifierModule;
import org.kif.reincarceration.permission.PermissionManager;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.ItemUtil;
import org.kif.reincarceration.modifier.core.IModifier;
import org.kif.reincarceration.modifier.core.ModifierManager;
import org.kif.reincarceration.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class BlockBreakListener implements Listener {
    private final Reincarceration plugin;
    private final PermissionManager permissionManager;
    private final ModifierManager modifierManager;

    public BlockBreakListener(Reincarceration plugin) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin);

        ModifierModule modifierModule = plugin.getModuleManager().getModule(ModifierModule.class);
        this.modifierManager = modifierModule.getModifierManager();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();

        boolean isAssociated = permissionManager.isAssociatedWithBaseGroup(player);

        ConsoleUtil.sendDebug("Player " + player.getName() + " broke a block. Associated with base group: " + isAssociated);

        if (isAssociated) {
            if (!canBreakBlock(player, block)) {
                event.setCancelled(true);
                MessageUtil.sendPrefixMessage(player, "&cYou cannot break blocks in this area.");
                return;
            }
            // Check if the item in hand is flagged
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (!ItemUtil.hasReincarcerationFlag(itemInHand) && !itemInHand.getType().isAir()) {
                event.setCancelled(true);
                MessageUtil.sendPrefixMessage(player, "&cYou can only break blocks with flagged items.");
                ConsoleUtil.sendDebug("Player " + player.getName() + " attempted to break a block with an unflagged item: " + itemInHand.getType());
                return;
            }

            try {
                IModifier activeModifier = modifierManager.getActiveModifier(player);
                if (activeModifier != null && activeModifier.handleBlockBreak(event)) {
                    // The modifier handled the event, so we're done
                    return;
                }

                // If we get here, either there was no active modifier or it didn't handle the event
                handleDefaultBreak(event);

            } catch (Exception e) {
                ConsoleUtil.sendError("Error handling block break: " + e.getMessage());
                event.setCancelled(true);
            }
        }
    }

    private boolean canBreakBlock(Player player, Block block) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(block.getLocation());
        com.sk89q.worldguard.LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        ApplicableRegionSet set = query.getApplicableRegions(loc);
        if (set.size() == 0) {
            // If there are no regions, allow breaking
            ConsoleUtil.sendDebug("No WorldGuard regions at " + block.getLocation() + ", allowing break");
            return true;
        }

        boolean canBreak = query.testState(loc, localPlayer, Flags.BLOCK_BREAK);
        ConsoleUtil.sendDebug("WorldGuard check for " + player.getName() + " at " + block.getLocation() + ": canBreak = " + canBreak);
        return canBreak;
    }

    private void handleDefaultBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        // Cancel normal drops
        event.setDropItems(false);

        // Handle special cases first
        if (handleSpecialCases(event)) {
            return; // If it was a special case, we're done
        }

        // If not a special case, handle normal drops
        List<ItemStack> drops = new ArrayList<>(block.getDrops(player.getInventory().getItemInMainHand()));
        for (ItemStack drop : drops) {
            ItemUtil.addReincarcerationFlag(drop);
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
            ConsoleUtil.sendDebug("Dropped flagged block item: " + drop.getType().name());
        }

        // Handle container contents if necessary
        handleContainerContents(block);
    }

    private boolean handleSpecialCases(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.CACTUS || block.getType() == Material.SUGAR_CANE) {
            if (!event.isCancelled()) {
                breakConnectedBlocks(block, block.getType(), event.getPlayer());
                return true; // We've handled this special case
            } else {
                ConsoleUtil.sendDebug("Block break was cancelled. Not handling connected blocks for " + block.getType());
            }
        }
        return false; // Not a special case
    }

    private void breakConnectedBlocks(Block startBlock, Material material, Player player) {
        List<Block> connectedBlocks = new ArrayList<>();
        connectedBlocks.add(startBlock);

        Block above = startBlock.getRelative(BlockFace.UP);
        while (above.getType() == material && connectedBlocks.size() < 3) {
            connectedBlocks.add(above);
            above = above.getRelative(BlockFace.UP);
        }

        // Drop items for all connected blocks
        for (Block block : connectedBlocks) {
            dropFlaggedItem(block.getLocation(), material, player);
            block.setType(Material.AIR);
        }

        ConsoleUtil.sendDebug("Broke and dropped " + connectedBlocks.size() + " connected " + material.name() + " blocks");
    }

    private void dropFlaggedItem(org.bukkit.Location location, Material material, Player player) {
        ItemStack drop = new ItemStack(material);
        ItemUtil.addReincarcerationFlag(drop);
        player.getWorld().dropItemNaturally(location, drop);
        ConsoleUtil.sendDebug("Dropped flagged item: " + material.name());
    }

    private void handleContainerContents(Block block) {
        if (block.getState() instanceof Container) {
            Container container = (Container) block.getState();
            for (ItemStack item : container.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                    ConsoleUtil.sendDebug("Dropped container item: " + item.getType().name() +
                            ", Flagged: " + ItemUtil.hasReincarcerationFlag(item));
                }
            }
            container.getInventory().clear();
        }
    }
}