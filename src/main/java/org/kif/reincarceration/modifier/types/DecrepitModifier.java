package org.kif.reincarceration.modifier.types;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.cycle.CycleManager;
import org.kif.reincarceration.cycle.CycleModule;
import org.kif.reincarceration.modifier.core.AbstractModifier;
import org.kif.reincarceration.util.ConsoleUtil;

public class DecrepitModifier extends AbstractModifier implements Listener {
    private final Reincarceration plugin;
    private final int maxHearts;

    public DecrepitModifier(Reincarceration plugin) {
        super("decrepit", "Decrepit", "Single heart");
        this.plugin = plugin;
        this.maxHearts = plugin.getConfig().getInt("modifiers.decrepit.max_hearts", 1);
        ConsoleUtil.sendDebug("DecrepitModifier initialized with max hearts: " + maxHearts);
    }

    @Override
    public void apply(Player player) {
        super.apply(player);
        setMaxHealth(player);
        ConsoleUtil.sendDebug("Applied Decrepit modifier to " + player.getName() + " with " + maxHearts + " max hearts");
    }

    @Override
    public void remove(Player player) {
        super.remove(player);
        resetMaxHealth(player);
        ConsoleUtil.sendDebug("Removed Decrepit modifier from " + player.getName());
    }

    private void setMaxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) {
            double oldValue = attribute.getBaseValue();
            attribute.setBaseValue(maxHearts * 2); // Each heart is 2 health points
            player.setHealth(attribute.getValue()); // Set current health to new max
            ConsoleUtil.sendDebug("Set max health for " + player.getName() + " from " + oldValue + " to " + attribute.getBaseValue());
        } else {
            ConsoleUtil.sendDebug("Failed to set max health for " + player.getName() + ": Attribute is null");
        }
    }

    private void resetMaxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) {
            double oldValue = attribute.getBaseValue();
            attribute.setBaseValue(20); // Reset to default 10 hearts (20 health points)
            player.setHealth(attribute.getValue()); // Set current health to new max
            ConsoleUtil.sendDebug("Reset max health for " + player.getName() + " from " + oldValue + " to " + attribute.getBaseValue());
        } else {
            ConsoleUtil.sendDebug("Failed to reset max health for " + player.getName() + ": Attribute is null");
        }
    }
}