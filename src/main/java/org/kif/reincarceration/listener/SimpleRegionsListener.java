package org.kif.reincarceration.listener;

import moe.krp.simpleregions.events.DeductUpkeepEvent;
import moe.krp.simpleregions.events.PreUpkeepCostCheckEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.data.DataModule;
import org.kif.reincarceration.util.ConsoleUtil;

import java.math.BigDecimal;
import java.sql.SQLException;

public class SimpleRegionsListener implements Listener {
    private final DataManager dataManager;


    public SimpleRegionsListener(
            Reincarceration plugin
    ) {
        this.dataManager = plugin.getModuleManager().getModule(DataModule.class).getDataManager();
    }

    @EventHandler
    public void onCheckUpkeepCost(PreUpkeepCostCheckEvent event) {
        final BigDecimal cost = event.getCost();
        BigDecimal balance;
        try {
            balance = dataManager.getStoredBalance(event.getPlayer().getUniqueId());
        } catch (SQLException e) {
            ConsoleUtil.sendError("Error retrieving stored balance for player: " + event.getPlayer().getName());
            ConsoleUtil.sendError(e.getMessage());
            return;
        }
        if (balance.compareTo(cost) < 0) {
            event.setHasEnough(false);
            return;
        }
        event.setHasEnough(true);
    }

    @EventHandler
    public void onDeductUpkeep(DeductUpkeepEvent event) {
        final BigDecimal cost = event.getUpkeepCost();
        BigDecimal balance;
        try {
            balance = dataManager.getStoredBalance(event.getPlayer().getUniqueId());
            final BigDecimal newBalance = balance.subtract(cost);
            ConsoleUtil.sendDebug("Old Balance: " + balance + ", New balance: " + balance);
            dataManager.setStoredBalance(event.getPlayer().getUniqueId(), newBalance);
            event.setEconomyInteractHandled(true);
        }  catch (SQLException e) {
            ConsoleUtil.sendError("Error setting stored balance for player: " + event.getPlayer().getName());
            ConsoleUtil.sendError(e.getMessage());
        }
    }
}
