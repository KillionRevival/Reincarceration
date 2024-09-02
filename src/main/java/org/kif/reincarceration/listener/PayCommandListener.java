package org.kif.reincarceration.listener;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.economy.EconomyManager;
import org.kif.reincarceration.economy.EconomyModule;
import org.kif.reincarceration.permission.PermissionManager;
import org.kif.reincarceration.data.DataModule;
import org.kif.reincarceration.util.MessageUtil;
import org.kif.reincarceration.util.ConsoleUtil;

import java.math.BigDecimal;
import java.sql.SQLException;

public class PayCommandListener implements Listener {
    private final Reincarceration plugin;
    private final PermissionManager permissionManager;
    private final DataManager dataManager;
    private final EconomyManager economyManager;

    public PayCommandListener(Reincarceration plugin) {
        this.plugin = plugin;
        this.permissionManager = new PermissionManager(plugin);
        DataModule dataModule = plugin.getModuleManager().getModule(DataModule.class);
        if (dataModule == null) {
            throw new IllegalStateException("DataModule is not initialized");
        }
        this.dataManager = dataModule.getDataManager();
        EconomyModule economyModule = plugin.getModuleManager().getModule(EconomyModule.class);
        this.economyManager = economyModule.getEconomyManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) throws SQLException {
        String[] args = event.getMessage().toLowerCase().split("\\s+");
        if (args.length >= 3 && args[0].equals("/pay")) {
            Player sender = event.getPlayer();
            String recipientName = args[1];
            BigDecimal amount;
            try {
                amount = new BigDecimal(args[2]);
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    return; // Let the normal economy plugin handle non-positive amounts
                }
            } catch (NumberFormatException e) {
                return; // Let the normal economy plugin handle invalid amounts
            }

            OfflinePlayer recipient = Bukkit.getPlayer(recipientName);
            if (recipient == null) {
                // check just to make sure...
                final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(recipientName);
                if (!offlinePlayer.hasPlayedBefore() || offlinePlayer.getPlayer() == null) {
                    return; // get out!
                }
                recipient = offlinePlayer.getPlayer();
            }

            boolean senderInSystem = dataManager.isPlayerInCycle(sender);
            boolean recipientInSystem = dataManager.isPlayerInCycle(recipient.getUniqueId());

            // Only intervene if at least one player is in the reincarceration system
            if (senderInSystem || recipientInSystem) {
                event.setCancelled(true);
                try {
                    if (senderInSystem) {
                        handleSenderInSystem(sender, recipient, amount);
                    } else {
                        handleRecipientInSystem(sender, recipient, amount);
                    }
                } catch (SQLException e) {
                    ConsoleUtil.sendError("Error processing payment: " + e.getMessage());
                    MessageUtil.sendPrefixMessage(sender, "<red>An error occurred while processing the payment. Please try again later.");
                }
            }
            // If neither player is in the system, do nothing and let the normal economy plugin handle it
        }
    }

    private void handleSenderInSystem(Player sender, OfflinePlayer recipient, BigDecimal amount) throws SQLException {
        BigDecimal storedBalance = dataManager.getStoredBalance(sender);
        if (storedBalance.compareTo(amount) >= 0) {
            dataManager.setStoredBalance(sender, storedBalance.subtract(amount));
            if (permissionManager.isAssociatedWithBaseGroup(recipient.getUniqueId())) {
                // Recipient is also in the system, add to their stored balance
                BigDecimal recipientStoredBalance = dataManager.getStoredBalance(recipient.getUniqueId());
                dataManager.setStoredBalance(recipient.getUniqueId(), recipientStoredBalance.add(amount));
            } else {
                // Recipient is outside the system, add to their regular balance
                economyManager.depositMoney(recipient, amount);
            }
            MessageUtil.sendPrefixMessage(sender, "<green>You have sent $" + amount.toPlainString() + " to " + recipient.getName() + ".");
            if (recipient.getPlayer() != null) {
                MessageUtil.sendPrefixMessage(recipient.getPlayer(), "<green>You have received $" + amount.toPlainString() + " from " + sender.getName() + ".");
            }
        } else {
            MessageUtil.sendPrefixMessage(sender, "<red>You don't have enough stored balance to send $" + amount.toPlainString() + ".");
        }
    }

    private void handleRecipientInSystem(Player sender, OfflinePlayer recipient, BigDecimal amount) throws SQLException {
        if (economyManager.hasEnoughBalance(sender, amount)) {
            economyManager.withdrawMoney(sender, amount);
            BigDecimal recipientStoredBalance = dataManager.getStoredBalance(recipient.getUniqueId());
            dataManager.setStoredBalance(recipient.getUniqueId(), recipientStoredBalance.add(amount));
            MessageUtil.sendPrefixMessage(sender, "<green>You have sent $" + amount.toPlainString() + " to " + recipient.getName() + "'s stored balance.");
            if (recipient.getPlayer() != null) {
                MessageUtil.sendPrefixMessage(recipient.getPlayer(), "<green>aYou have received $" + amount.toPlainString() + " in your stored balance from " + sender.getName() + ".");
            }
        } else {
            MessageUtil.sendPrefixMessage(sender, "<red>You don't have enough money to send $" + amount.toPlainString() + ".");
        }
    }
}