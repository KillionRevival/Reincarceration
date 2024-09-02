package org.kif.reincarceration.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.cycle.CycleManager;
import org.kif.reincarceration.cycle.CycleModule;
import org.kif.reincarceration.rank.RankManager;
import org.kif.reincarceration.rank.RankModule;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.MessageUtil;

public class RankupListener implements Listener {
    private final Reincarceration plugin;
    private final CycleManager cycleManager;
    private final RankManager rankManager;

    public RankupListener(Reincarceration plugin) {
        this.plugin = plugin;
        this.cycleManager = this.plugin.getModuleManager().getModule(CycleModule.class).getCycleManager();
        this.rankManager = this.plugin.getModuleManager().getModule(RankModule.class).getRankManager();
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().equalsIgnoreCase("/rankup")) {
            Player player = event.getPlayer();
            if (cycleManager.isPlayerInCycle(player)) {
                ConsoleUtil.sendDebug(
                        "Player " + player.getName() + " ran the /rankup command and is in cycle");

                try {
                    if (rankManager.canRankUp(player)) {
                        rankManager.rankUp(player);
                        MessageUtil.sendPrefixMessage(player, "<green>You've successfully ranked up!");
                    } else {
                        MessageUtil.sendPrefixMessage(player, "<red>You can't rank up right now.");
                    }
                } catch (Exception e) {
                    MessageUtil.sendPrefixMessage(player, "<red>Error ranking up: " + e.getMessage());
                }

                // Cancel the rest of processing for the rankup command
                event.setCancelled(true);
            }
        }
    }
}