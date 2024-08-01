package org.kif.reincarceration.rewards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.economy.EconomyModule;
import org.kif.reincarceration.modifier.core.IModifier;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.RewardUtil;

import java.io.Console;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RewardManager {
    private final EconomyModule economyModule;
    private final Reincarceration plugin;
    private final ConcurrentHashMap<UUID, IModifier> playersNeedingRewards;

    public RewardManager(Reincarceration plugin) {
        this.economyModule = plugin.getModuleManager().getModule(EconomyModule.class);
        this.plugin = plugin;
        playersNeedingRewards = new ConcurrentHashMap<>();
    }

    public boolean isPlayerNeededReward(final Player player) {
        if (player == null) return false;
        return playersNeedingRewards.containsKey(player.getUniqueId());
    }

    public void setPlayerNeedsReward(@NotNull final Player player, @NotNull final IModifier modifier) {
        playersNeedingRewards.put(player.getUniqueId(), modifier);
    }

    public void rewardPlayer(@NotNull final Player player) {
        synchronized (playersNeedingRewards) {
            final IModifier modifier = playersNeedingRewards.get(player.getUniqueId());
            final CycleReward reward = RewardUtil.getCycleRewardForModifier(modifier, plugin);
            if (reward == null) {
                ConsoleUtil.sendDebug("No reward configured for " + modifier.getName());
                return;
            }

            if (economyModule != null && (reward.getMoney() == null || reward.getMoney().equals(BigDecimal.ZERO))) {
                economyModule.getEconomyManager().depositMoney(player, reward.getMoney());
            }

            for (final String command : reward.getCommands()) {
                plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(),
                        command
                );
            }

            final List<ItemStack> itemStackList = reward.getItems()
                                                        .stream()
                                                        .map(RewardUtil::buildItemStackFromRewardItem)
                                                        .toList();
            // will a player ever not be able to fit items? this is called on respawn
            itemStackList.forEach(item -> player.getInventory().addItem(item));
            playersNeedingRewards.remove(player.getUniqueId());
        }
    }

    public void rewardPlayer(final UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            ConsoleUtil.sendError("Tried to reward player but they don't exist!");
            ConsoleUtil.sendError("UUID: " + uuid);
            ConsoleUtil.sendError("Modifier completed: " +
                    playersNeedingRewards.get(uuid).getName()
            );
            return;
        }

        rewardPlayer(player);
    }
}
