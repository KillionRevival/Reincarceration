package org.kif.reincarceration.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.cycle.CycleManager;
import org.kif.reincarceration.cycle.CycleModule;
import org.kif.reincarceration.rank.RankManager;
import org.kif.reincarceration.util.ConsoleUtil;

public class RankupListener implements Listener {
    private Reincarceration plugin;
    private CycleManager cycleManager;
    private RankManager rankManager;

    public RankupListener(Reincarceration plugin) {
        this.plugin = plugin;
        this.cycleManager = this.plugin.getModuleManager().getModule(CycleModule.class).getCycleManager();
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
                        player.sendMessage(ChatColor.GREEN + "You've successfully ranked up!");
                    } else {
                        player.sendMessage(ChatColor.RED + "You can't rank up right now.");
                    }
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error ranking up: " + e.getMessage());
                }

                // Cancel the reset of processing for the rankup command
                event.setCancelled(true);
            }
        }
    }
}
