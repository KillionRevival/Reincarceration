package org.kif.reincarceration.modifier.types;

import me.gypopo.economyshopgui.api.events.PreTransactionEvent;
import me.gypopo.economyshopgui.objects.ShopItem;
import me.gypopo.economyshopgui.util.Transaction;
import org.bukkit.Material;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.GlowSquid;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.modifier.core.AbstractModifier;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.ItemUtil;
import org.kif.reincarceration.util.MessageUtil;
import org.bukkit.entity.Item;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class AnglerModifier extends AbstractModifier implements Listener {
    private final Reincarceration plugin;
    private boolean provideRodOnDeath;
    private boolean preventRodDurabilityLoss;
    private final Set<Material> allowedItems;
    private final Map<Material, Integer> disallowedSwapItems = new EnumMap<>(Material.class);
    private final Map<UUID, BukkitRunnable> activeWaterTasks;
    private final Map<UUID, GlowSquid> activeSquids;
    private final Map<UUID, FishingData> playerFishingData;
    private final Map<UUID, Boolean> playerInWaterStatus;

    // Configuration options
    private double pushForce;
    private int pushInterval;
    private int minWaterSize;
    private double squidSpeed;
    private double fishingPullForce;
    private int fishingPullDuration;
    private int safeguardThreshold;
    private double safeguardUpwardForce;
    private double safeguardHorizontalForce;
    private double minSquidSpawnDistance;
    private double maxSquidSpawnDistance;

    // Fish materials - not configurable



    private static class FishingData {
        Vector waterDirection;
        int unchangedCount;
        double lastRelevantCoordinate;

        FishingData(Vector waterDirection, double lastRelevantCoordinate) {
            this.waterDirection = waterDirection;
            this.unchangedCount = 0;
            this.lastRelevantCoordinate = lastRelevantCoordinate;
        }
    }

    public AnglerModifier(Reincarceration plugin) {
        super("angler", "Angler", "Provides fishing benefits, restricts selling to fish-related items, adds dangerous water encounters, and applies a pull effect on successful catches with safeguards");
        this.plugin = plugin;
        this.allowedItems = new HashSet<>();
        this.activeWaterTasks = new HashMap<>();
        this.activeSquids = new HashMap<>();
        this.playerFishingData = new HashMap<>();
        this.playerInWaterStatus = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("modifiers.angler");
        if (config != null) {
            this.provideRodOnDeath = config.getBoolean("provide_rod_on_death", true);
            this.preventRodDurabilityLoss = config.getBoolean("prevent_rod_durability_loss", true);
            this.pushForce = config.getDouble("push_force", 0.5);
            this.pushInterval = config.getInt("push_interval", 20);
            this.minWaterSize = config.getInt("min_water_size", 50);
            this.squidSpeed = config.getDouble("squid_speed", 0.5);
            this.fishingPullForce = config.getDouble("fishing_pull_force", 0.05);
            this.fishingPullDuration = config.getInt("fishing_pull_duration", 20);
            this.safeguardThreshold = config.getInt("safeguard_threshold", 5);
            this.safeguardUpwardForce = config.getDouble("safeguard_upward_force", 1.0);
            this.safeguardHorizontalForce = config.getDouble("safeguard_horizontal_force", 1.5);
            this.minSquidSpawnDistance = config.getDouble("min_squid_spawn_distance", 5.0);
            this.maxSquidSpawnDistance = config.getDouble("max_squid_spawn_distance", 15.0);

            List<String> allowedItemsList = config.getStringList("allowed_items");
            if (!allowedItemsList.isEmpty()) {
                for (String item : allowedItemsList) {
                    try {
                        Material material = Material.valueOf(item.toUpperCase());
                        allowedItems.add(material);
                    } catch (IllegalArgumentException e) {
                        ConsoleUtil.sendError("Invalid material in Angler modifier config: " + item);
                    }
                }
            } else {
                ConsoleUtil.sendError("No allowed items specified in Angler modifier config. Using default values.");
                initializeDefaultAllowedItems();
            }
            List<String> swapListList = config.getStringList("disallowed_swap_items");
            if (!swapListList.isEmpty()) {
                for (String item : swapListList) {
                    try {
                        String[] parts = item.split(" ");
                        if (parts.length != 2) {
                            throw new IllegalArgumentException("Invalid format");
                        }
                        Material material = Material.valueOf(parts[0].toUpperCase());
                        int weight = Integer.parseInt(parts[1]);
                        disallowedSwapItems.put(material, weight);
                    } catch (IllegalArgumentException e) {
                        if (e instanceof NumberFormatException) {
                            ConsoleUtil.sendError("Invalid weight in Angler modifier config: " + item);
                        } else {
                            ConsoleUtil.sendError("Invalid material or format in Angler modifier config: " + item);
                        }
                        if (!disallowedSwapItems.isEmpty()) {
                            disallowedSwapItems.clear();
                        }
                        initializeDefaultDisallowedSwapItems();
                        break;
                    }
                }
            } else {
                ConsoleUtil.sendError("No allowed items specified in Angler modifier config. Using default values.");
                initializeDefaultDisallowedSwapItems();
            }
        } else {
            ConsoleUtil.sendError("Angler modifier configuration not found. Using default values.");
            this.provideRodOnDeath = true;
            this.preventRodDurabilityLoss = true;
            this.pushForce = 0.5;
            this.pushInterval = 20;
            this.minWaterSize = 50;
            this.squidSpeed = 0.5;
            this.fishingPullForce = 0.05;
            this.fishingPullDuration = 20;
            this.safeguardThreshold = 5;
            this.safeguardUpwardForce = 1.0;
            this.safeguardHorizontalForce = 1.5;
            this.minSquidSpawnDistance = 5.0;
            this.maxSquidSpawnDistance = 15.0;
            initializeDefaultAllowedItems();
        }
        ConsoleUtil.sendDebug("Angler Modifier Config: Provide Rod On Death = " + provideRodOnDeath +
                ", Prevent Rod Durability Loss = " + preventRodDurabilityLoss +
                ", Push Force = " + pushForce +
                ", Push Interval = " + pushInterval +
                ", Min Water Size = " + minWaterSize +
                ", Squid Speed = " + squidSpeed +
                ", Fishing Pull Force = " + fishingPullForce +
                ", Fishing Pull Duration = " + fishingPullDuration +
                ", Safeguard Threshold = " + safeguardThreshold +
                ", Safeguard Upward Force = " + safeguardUpwardForce +
                ", Safeguard Horizontal Force = " + safeguardHorizontalForce +
                ", Min Squid Spawn Distance = " + minSquidSpawnDistance +
                ", Max Squid Spawn Distance = " + maxSquidSpawnDistance);
        ConsoleUtil.sendDebug("Angler Modifier Config: Allowed Items = " + allowedItems);
    }

    private void initializeDefaultAllowedItems() {
        allowedItems.add(Material.COD);
        allowedItems.add(Material.SALMON);
        allowedItems.add(Material.TROPICAL_FISH);
        allowedItems.add(Material.PUFFERFISH);
        allowedItems.add(Material.NAUTILUS_SHELL);
        allowedItems.add(Material.FISHING_ROD);
        allowedItems.add(Material.ENCHANTED_BOOK);
        allowedItems.add(Material.BOW);
        allowedItems.add(Material.LILY_PAD);
        allowedItems.add(Material.BOWL);
        allowedItems.add(Material.LEATHER);
        allowedItems.add(Material.LEATHER_BOOTS);
        allowedItems.add(Material.SADDLE);
        allowedItems.add(Material.NAME_TAG);
        allowedItems.add(Material.TRIPWIRE_HOOK);
        allowedItems.add(Material.STICK);
        allowedItems.add(Material.INK_SAC);
        allowedItems.add(Material.BAMBOO);
        allowedItems.add(Material.COOKED_COD);
        allowedItems.add(Material.COOKED_SALMON);
    }

    private void initializeDefaultDisallowedSwapItems() {
        disallowedSwapItems.put(Material.COD, 60);
        disallowedSwapItems.put(Material.SALMON, 25);
        disallowedSwapItems.put(Material.TROPICAL_FISH, 2);
        disallowedSwapItems.put(Material.PUFFERFISH, 13);
    }

    @Override
    public void apply(Player player) {
        super.apply(player);

        if (player.isOnline() && player.isValid() && !playerHasFishingRod(player)) {
            provideFishingRod(player);
        }

        ConsoleUtil.sendDebug("Applied Angler Modifier to " + player.getName());
    }

    @Override
    public void remove(Player player) {
        super.remove(player);
        stopWaterTask(player);
        playerInWaterStatus.remove(player.getUniqueId());
        ConsoleUtil.sendDebug("Removed Angler Modifier from " + player.getName());
    }

    private boolean playerHasFishingRod(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.FISHING_ROD) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isActive(player) && provideRodOnDeath) {
            // Use a delayed task to ensure the item is given after respawn
            new BukkitRunnable() {
                @Override
                public void run() {
                    provideFishingRod(player);
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (!isActive(player)) return;

        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH || event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            Entity caught = event.getCaught();
            if (caught instanceof Item) {
                Item caughtItem = (Item) caught;
                ConsoleUtil.sendDebug("Angler - Caught ITEM: " + caughtItem.getItemStack().getType());
                if (!allowedItems.contains(caughtItem.getItemStack().getType())) {
                    Material randomFish = getRandomFish();
                    ItemStack newItem = new ItemStack(randomFish);
                    ItemUtil.addReincarcerationFlag(newItem);
                    caughtItem.setItemStack(newItem);
                    ConsoleUtil.sendDebug("Angler - Changed caught item to: " + newItem.getType());
                }
            } else if (caught != null) {
                ConsoleUtil.sendDebug("Angler - Caught non-item entity: " + ((Entity) caught).getType());
            }
            handleFishingPull(player);
        }

        if (preventRodDurabilityLoss) {
            ItemStack fishingRod = player.getInventory().getItemInMainHand();
            if (fishingRod.getType() == Material.FISHING_ROD) {
                ItemMeta meta = fishingRod.getItemMeta();
                if (meta != null && meta.hasEnchants()) {
                    return; // Exit the method if the fishing rod is enchanted
                }

                if (meta instanceof Damageable) {
                    Damageable damageableMeta = (Damageable) meta;
                    damageableMeta.setDamage(50);
                    fishingRod.setItemMeta((ItemMeta) damageableMeta);
                    ConsoleUtil.sendDebug("Restored durability of fishing rod for " + player.getName());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isActive(player)) return;

        boolean wasInWater = playerInWaterStatus.getOrDefault(player.getUniqueId(), false);
        boolean isInWater = player.getLocation().getBlock().getType() == Material.WATER;

        if (!wasInWater && isInWater) {
            // Player just entered water
            playerInWaterStatus.put(player.getUniqueId(), true);
            startWaterTask(player);
        } else if (wasInWater && !isPlayerInOrOnWater(player)) {
            // Player might be leaving water, but we'll let the water task handle it
            playerInWaterStatus.put(player.getUniqueId(), false);
        }
    }

    private boolean isPlayerInOrOnWater(Player player) {
        Block block = player.getLocation().getBlock();
        Block blockBelow = block.getRelative(BlockFace.DOWN);

        // Check if the player is in water or if the block below is water
        return block.getType() == Material.WATER || blockBelow.getType() == Material.WATER;
    }

    private void handleFishingPull(Player player) {
        Vector waterDirection = findNearestWaterDirection(player.getLocation());
        if (waterDirection == null) return;

        FishingData fishingData = playerFishingData.computeIfAbsent(player.getUniqueId(),
            k -> new FishingData(waterDirection, getRelevantCoordinate(player.getLocation(), waterDirection)));

        double currentRelevantCoordinate = getRelevantCoordinate(player.getLocation(), waterDirection);
        if (Math.abs(currentRelevantCoordinate - fishingData.lastRelevantCoordinate) < 0.01) {
            fishingData.unchangedCount++;
        } else {
            fishingData.unchangedCount = 0;
        }
        fishingData.lastRelevantCoordinate = currentRelevantCoordinate;

        if (fishingData.unchangedCount >= safeguardThreshold) {
            applySafeguardEffect(player, waterDirection);
        } else {
            applyFishingPullEffect(player, waterDirection);
        }
    }

    private double getRelevantCoordinate(Location location, Vector direction) {
        if (Math.abs(direction.getX()) > Math.abs(direction.getZ())) {
            return location.getX();
        } else {
            return location.getZ();
        }
    }

    private void applySafeguardEffect(Player player, Vector waterDirection) {
        player.setVelocity(new Vector(0, safeguardUpwardForce, 0));

        new BukkitRunnable() {
            @Override
            public void run() {
                player.setVelocity(waterDirection.multiply(safeguardHorizontalForce));
            }
        }.runTaskLater(plugin, 3L);

        playerFishingData.remove(player.getUniqueId());
    }

    private void applyFishingPullEffect(Player player, Vector pullDirection) {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !isActive(player) || ticks >= fishingPullDuration) {
                    this.cancel();
                    return;
                }

                player.setVelocity(player.getVelocity().add(pullDirection.multiply(fishingPullForce)));
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startWaterTask(Player player) {
        if (activeWaterTasks.containsKey(player.getUniqueId())) {
            return; // Task already running
        }

        BukkitRunnable task = new BukkitRunnable() {
            int outOfWaterTicks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !isActive(player)) {
                    stopWaterTask(player);
                    return;
                }

                if (!isPlayerInOrOnWater(player)) {
                    outOfWaterTicks++;
                    if (outOfWaterTicks > 10) { // 0.5 seconds (10 ticks) grace period
                        stopWaterTask(player);
                        return;
                    }
                } else {
                    outOfWaterTicks = 0;
                    pushPlayerAway(player);
                    if (isLargeWaterBody(player.getLocation()) && !activeSquids.containsKey(player.getUniqueId())) {
                        spawnSquid(player);
                    }
                }
            }
        };

        task.runTaskTimer(plugin, 0L, pushInterval);
        activeWaterTasks.put(player.getUniqueId(), task);
    }

    private void stopWaterTask(Player player) {
        BukkitRunnable task = activeWaterTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            removeSquid(player);
            playerInWaterStatus.put(player.getUniqueId(), false);
            ConsoleUtil.sendDebug("Stopped water task for " + player.getName());
        }
    }

    private void pushPlayerAway(Player player) {
        Location loc = player.getLocation();
        Vector direction = new Vector(Math.random() - 0.5, -0.2, Math.random() - 0.5).normalize().multiply(pushForce);
        player.setVelocity(player.getVelocity().add(direction));
    }

    private boolean isLargeWaterBody(Location location) {
        int waterBlocks = 0;
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    Block block = location.getBlock().getRelative(x, y, z);
                    if (block.getType() == Material.WATER) {
                        waterBlocks++;
                        if (waterBlocks >= minWaterSize) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Location findWaterBlockNearby(World world, double x, int startY, double z) {
        // Search in a vertical range of ±10 blocks from the start Y
        for (int yOffset = 0; yOffset <= 10; yOffset++) {
            for (int sign : new int[]{1, -1}) {  // Check both above and below
                int y = startY + (yOffset * sign);
                if (y > 0 && y < world.getMaxHeight()) {  // Ensure y is within world bounds
                    Location loc = new Location(world, x, y, z);
                    if (isValidSquidSpawnLocation(loc)) {
                        return loc;
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidSquidSpawnLocation(Location location) {
        Block block = location.getBlock();
        Block above = block.getRelative(BlockFace.UP);
        Block below = block.getRelative(BlockFace.DOWN);

        // Check if the block and the block above it are water, and the block below is not air
        return block.getType() == Material.WATER &&
                above.getType() == Material.WATER &&
                below.getType() != Material.AIR;
    }

    private void spawnSquid(Player player) {
        Location spawnLoc = findWaterSpawnLocation(player.getLocation());
        if (spawnLoc != null) {
            GlowSquid squid = player.getWorld().spawn(spawnLoc, GlowSquid.class);
            activeSquids.put(player.getUniqueId(), squid);
            pursuePlayer(squid, player);
            ConsoleUtil.sendDebug("Spawned squid for " + player.getName() + " at " + spawnLoc);
        } else {
            ConsoleUtil.sendDebug("Failed to find valid squid spawn location for " + player.getName());
        }
    }

    private Location findWaterSpawnLocation(Location playerLoc) {
        World world = playerLoc.getWorld();
        if (world == null) return null;

        for (int i = 0; i < 50; i++) {  // Increased attempts to find a suitable location
            double distance = minSquidSpawnDistance + (Math.random() * (maxSquidSpawnDistance - minSquidSpawnDistance));
            double angle = Math.random() * 2 * Math.PI;

            double x = playerLoc.getX() + (distance * Math.cos(angle));
            double z = playerLoc.getZ() + (distance * Math.sin(angle));

            // Start from the player's y-coordinate and search up and down
            int startY = playerLoc.getBlockY();
            Location potentialLoc = findWaterBlockNearby(world, x, startY, z);

            if (potentialLoc != null) {
                return potentialLoc;
            }
        }
        return null;
    }

    private void pursuePlayer(GlowSquid squid, Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            int outOfWaterTicks = 0;
            @Override
            public void run() {
                if (!squid.isValid() || !player.isOnline() || !isActive(player)) {
                    removeSquid(player);
                    this.cancel();
                    return;
                }

                if (!isPlayerInOrOnWater(player)) {
                    outOfWaterTicks++;
                    if (outOfWaterTicks > 40) { // 2 seconds (40 ticks) grace period
                        removeSquid(player);
                        this.cancel();
                        return;
                    }
                } else {
                    outOfWaterTicks = 0;
                    Vector direction = player.getLocation().toVector().subtract(squid.getLocation().toVector()).normalize().multiply(squidSpeed);
                    squid.setVelocity(direction);

                    if (squid.getLocation().distance(player.getLocation()) < 1.5) {
                        player.setHealth(0);
                        removeSquid(player);
                        this.cancel();
                        return;
                    }
                }

                ticks++;
                if (ticks >= 20 * 60) { // 60 seconds max pursuit time
                    removeSquid(player);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void removeSquid(Player player) {
        GlowSquid squid = activeSquids.remove(player.getUniqueId());
        if (squid != null) {
            squid.remove();
        }
    }

    private Vector findNearestWaterDirection(Location location) {
        World world = location.getWorld();
        if (world == null) return null;

        int radius = 5;
        Vector playerPos = location.toVector();
        Vector nearestWater = null;
        double minDistanceSquared = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                    if (block.getType() == Material.WATER) {
                        Vector waterPos = block.getLocation().toVector();
                        double distanceSquared = waterPos.distanceSquared(playerPos);
                        if (distanceSquared < minDistanceSquared) {
                            minDistanceSquared = distanceSquared;
                            nearestWater = waterPos;
                        }
                    }
                }
            }
        }

        return nearestWater != null ? nearestWater.subtract(playerPos).normalize() : null;
    }

    @Override
    public boolean handleSellTransaction(PreTransactionEvent event) {
        Player player = event.getPlayer();
        ShopItem shopItem = event.getShopItem();
        if (shopItem != null) {
            ItemStack itemStack = shopItem.getItemToGive();
            if (itemStack != null) {
                ConsoleUtil.sendDebug("Angler modifier checking sell transaction for item: " + itemStack.getType());
                if (!allowedItems.contains(itemStack.getType())) {
                    event.setCancelled(true);
                    MessageUtil.sendPrefixMessage(player, "<red>Transaction Denied - Attempted to sell prohibited items.");
                    ConsoleUtil.sendDebug("Transaction cancelled because item " + itemStack.getType() + " is not allowed.");
                    return true;
                }
            }
        }
        return false;  // Allow the transaction to proceed
    }

    private void provideFishingRod(Player player) {
        if (!player.getInventory().contains(Material.FISHING_ROD)) {
            ItemStack item = new ItemStack(Material.FISHING_ROD);

            ItemMeta meta = item.getItemMeta();

            if (meta instanceof Damageable) {
                Damageable damageable = (Damageable) meta;
                // int maxDurability = Material.FISHING_ROD.getMaxDurability();
                int damage = 50;
                damageable.setDamage(damage);
            }

            item.setItemMeta(meta);

            ItemUtil.addReincarcerationFlag(item);
            player.getInventory().addItem(item);
            ConsoleUtil.sendDebug("Provided fishing rod to " + player.getName());
        }
    }

    private Material getRandomFish() {
        int totalWeight = disallowedSwapItems.values().stream().mapToInt(Integer::intValue).sum();
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;

        for (Map.Entry<Material, Integer> entry : disallowedSwapItems.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }
        return Material.COD; // Default to COD if something goes wrong
    }

    public void reloadConfig() {
        loadConfig();
    }
}
