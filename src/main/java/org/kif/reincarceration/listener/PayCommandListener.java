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
import java.util.Objects;
import java.util.UUID;

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
        ConsoleUtil.sendInfo("Processing command: " + event.getMessage());
        String[] args = event.getMessage().toLowerCase().split("\\s+");
        if (args.length >= 3 && args[0].equals("/pay")) {
            Player sender = event.getPlayer();
            ConsoleUtil.sendInfo("Sender: " + sender.getName());
            String recipientName = args[1];
            BigDecimal amount;
            try {
                amount = new BigDecimal(args[2]);
                ConsoleUtil.sendInfo("Amount: " + amount.toPlainString());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    ConsoleUtil.sendInfo("Amount is non-positive, letting normal economy plugin handle it.");
                    return;
                }
            } catch (NumberFormatException e) {
                ConsoleUtil.sendInfo("Invalid amount format: " + args[2]);
                return;
            }

            OfflinePlayer recipient = Bukkit.getPlayerExact(recipientName);
            UUID recipientUUID;
            if (recipient == null) {
                ConsoleUtil.sendInfo("Recipient not online, checking offline players.");
                recipient = Bukkit.getOfflinePlayer(recipientName);
                if (!recipient.hasPlayedBefore()) {
                    ConsoleUtil.sendInfo("Recipient has never played before: " + recipientName);
                    return;
                }
            }
            recipientUUID = recipient.getUniqueId();

            ConsoleUtil.sendInfo("Recipient UUID: " + recipientUUID);

            boolean senderInSystem = dataManager.isPlayerInCycle(sender);
            boolean recipientInSystem = dataManager.isPlayerInCycle(recipientUUID);
            ConsoleUtil.sendInfo("Sender in system: " + senderInSystem + ", Recipient in system: " + recipientInSystem);

            if (senderInSystem || recipientInSystem) {
                event.setCancelled(true);
                try {
                    if (senderInSystem) {
                        ConsoleUtil.sendInfo("Handling sender in system.");
                        handleSenderInSystem(sender, recipient, amount);
                    } else {
                        ConsoleUtil.sendInfo("Handling recipient in system.");
                        handleRecipientInSystem(sender, recipient, amount);
                    }
                } catch (SQLException e) {
                    ConsoleUtil.sendError("Error processing payment: " + e.getMessage());
                    MessageUtil.sendPrefixMessage(sender, "&cAn error occurred while processing the payment. Please try again later.");
                }
            } else {
                ConsoleUtil.sendInfo("Neither player is in the system, letting normal economy plugin handle it.");
            }
        }
    }

    private void handleSenderInSystem(Player sender, OfflinePlayer recipient, BigDecimal amount) throws SQLException {
        ConsoleUtil.sendInfo("Handling payment where sender is in the system.");
        BigDecimal storedBalance = dataManager.getStoredBalance(sender);
        ConsoleUtil.sendInfo("Sender's stored balance: " + storedBalance.toPlainString());
        if (storedBalance.compareTo(amount) >= 0) {
            dataManager.setStoredBalance(sender, storedBalance.subtract(amount));
            ConsoleUtil.sendInfo("Deducted amount from sender's stored balance.");
            if (permissionManager.isAssociatedWithBaseGroup(recipient.getUniqueId())) {
                ConsoleUtil.sendInfo("Recipient is in the system, updating stored balance.");
                BigDecimal recipientStoredBalance = dataManager.getStoredBalance(recipient.getUniqueId());
                dataManager.setStoredBalance(recipient.getUniqueId(), recipientStoredBalance.add(amount));
                ConsoleUtil.sendInfo("Updated recipient's stored balance: " + dataManager.getStoredBalance(recipient.getUniqueId()).toPlainString());
            } else {
                ConsoleUtil.sendInfo("Recipient is outside the system, depositing to regular balance.");
                economyManager.depositMoney(recipient, amount);
            }
            MessageUtil.sendPrefixMessage(sender, "&aYou have sent $" + amount.toPlainString() + " to " + recipient.getName() + ".");
            if (recipient.isOnline()) {
                MessageUtil.sendPrefixMessage(Objects.requireNonNull(recipient.getPlayer()), "&aYou have received $" + amount.toPlainString() + " from " + sender.getName() + ".");
            }
        } else {
            ConsoleUtil.sendInfo("Sender does not have enough stored balance.");
            MessageUtil.sendPrefixMessage(sender, "&cYou don't have enough stored balance to send $" + amount.toPlainString() + ".");
        }
    }

    private void handleRecipientInSystem(Player sender, OfflinePlayer recipient, BigDecimal amount) throws SQLException {
        ConsoleUtil.sendInfo("Handling payment where recipient is in the system.");
        if (economyManager.hasEnoughBalance(sender, amount)) {
            ConsoleUtil.sendInfo("Sender has enough balance, proceeding with withdrawal.");
            economyManager.withdrawMoney(sender, amount);
            BigDecimal recipientStoredBalance = dataManager.getStoredBalance(recipient.getUniqueId());
            dataManager.setStoredBalance(recipient.getUniqueId(), recipientStoredBalance.add(amount));
            ConsoleUtil.sendInfo("Updated recipient's stored balance: " + dataManager.getStoredBalance(recipient.getUniqueId()).toPlainString());
            MessageUtil.sendPrefixMessage(sender, "&aYou have sent $" + amount.toPlainString() + " to " + recipient.getName() + "'s stored balance.");
            if (recipient.isOnline()) {
                MessageUtil.sendPrefixMessage(Objects.requireNonNull(recipient.getPlayer()), "&aYou have received $" + amount.toPlainString() + " in your stored balance from " + sender.getName() + ".");
            }
        } else {
            ConsoleUtil.sendInfo("Sender does not have enough money.");
            MessageUtil.sendPrefixMessage(sender, "&cYou don't have enough money to send $" + amount.toPlainString() + ".");
        }
    }
}