package org.kif.reincarceration.modifier.types;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.modifier.core.AbstractModifier;
import org.kif.reincarceration.util.ConsoleUtil;

public class TortoiseModifier extends AbstractModifier implements Listener {
    private final Reincarceration plugin;
    private int slownessLevel;
    private int resistanceLevel;
    private int miningHasteLevel;
    private int immobilizationDuration;

    public TortoiseModifier(Reincarceration plugin) {
        super("tortoise", "Tortoise",
                "Decreases movement speed but increases resistance and mining speed. Immobilizes player on damage.");
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("modifiers.tortoise");
        if (config != null) {
            this.slownessLevel = config.getInt("slowness_level", 2);
            this.resistanceLevel = config.getInt("resistance_level", 1);
            this.miningHasteLevel = config.getInt("mining_haste_level", 2);
            this.immobilizationDuration = config.getInt("immobilization_duration", 5);
        } else {
            ConsoleUtil.sendError("Tortoise modifier configuration not found. Using default values.");
            this.slownessLevel = 2;
            this.resistanceLevel = 1;
            this.miningHasteLevel = 2;
            this.immobilizationDuration = 5;
        }
        ConsoleUtil.sendDebug("Tortoise Modifier Config: Slowness Level = " + slownessLevel + ", Resistance Level = "
                + resistanceLevel + ", Mining Haste Level = " + miningHasteLevel + ", Immobilization Duration = "
                + immobilizationDuration + " seconds");
    }

    @Override
    public void apply(Player player) {
        super.apply(player);
        applyEffects(player);
        startEffectChecker(player);
        ConsoleUtil.sendDebug("Applied Tortoise Modifier to " + player.getName());
    }

    @Override
    public void remove(Player player) {
        super.remove(player);
        removeEffects(player);
        ConsoleUtil.sendDebug("Removed Tortoise Modifier from " + player.getName());
    }

    private void applyEffects(Player player) {
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, slownessLevel - 1, false, false));
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, resistanceLevel - 1, false, false));
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, miningHasteLevel - 1, false, false));
    }

    private void removeEffects(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.HASTE);
    }

    private void startEffectChecker(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive(player)) {
                    this.cancel();
                    return;
                }
                applyEffects(player);
                ConsoleUtil.sendDebug("Reapplied all Tortoise effects to " + player.getName());
            }
        }.runTaskTimer(plugin, 20L * 30, 20L * 30); // Check every 30 seconds
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isActive(player)) {
            player.addPotionEffect(
                    new PotionEffect(PotionEffectType.HASTE, 40, miningHasteLevel - 1, false, false));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (isActive(player)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    applyEffects(player);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (isActive(player)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    applyEffects(player);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isActive(player)) {
                immobilizePlayer(player);
            }
        }
    }

    private void immobilizePlayer(Player player) {
        player.addPotionEffect(
                new PotionEffect(PotionEffectType.SLOWNESS, immobilizationDuration * 20, 6, false, false)); // Slowness level 7 (index 6) makes the player immobile
        ConsoleUtil.sendDebug(
                "Immobilized " + player.getName() + " for " + immobilizationDuration + " seconds due to damage");
    }

    public void reloadConfig() {
        loadConfig();
    }
}
