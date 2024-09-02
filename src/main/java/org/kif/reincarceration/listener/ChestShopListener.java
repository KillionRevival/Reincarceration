package org.kif.reincarceration.listener;

import com.Acrobot.ChestShop.Events.Economy.CurrencyTransferEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.data.DataModule;
import org.kif.reincarceration.permission.PermissionManager;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.MessageUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;

public class ChestShopListener implements Listener {
    private final Reincarceration plugin;
    private final PermissionManager permissionManager;
    private final DataManager dataManager;

    public ChestShopListener(
            final Reincarceration plugin
    ) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin);
        this.dataManager = plugin.getModuleManager().getModule(DataModule.class).getDataManager();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCurrencyTransfer(final CurrencyTransferEvent event) {
        final UUID seller = event.getReceiver();
        boolean isAssociated = permissionManager.isAssociatedWithBaseGroup(seller);

        if (!isAssociated) {
            return;
        }

        final BigDecimal amountGoingToOwner = event.getAmountReceived();
        final BigDecimal storedBalance;
        try {
            storedBalance = dataManager.getStoredBalance(seller);

            // Money should not be given to those in reincarceration - store it for later.
            event.setAmountReceived(BigDecimal.ZERO);
            final BigDecimal newBalance = storedBalance.add(amountGoingToOwner);
            ConsoleUtil.sendDebug("Setting new balance for " + seller + " to " + newBalance + " from " + storedBalance);
            dataManager.setStoredBalance(seller, newBalance);

            final Player player = Bukkit.getServer().getPlayer(seller);
            if (player != null) {
                MessageUtil.sendPrefixMessage(player, "<dark_red>You are currently in a cycle, so your ChestShop transaction has been placed with your stored balance.");
            }
        } catch (SQLException e) {
            event.setHandled(false);
            plugin.getLogger().severe("Error in CurrencyTransferEvent: " + e.getMessage());
        }
    }
}
