package org.kif.reincarceration.gui;

import me.gypopo.economyshopgui.api.events.PostTransactionEvent;
import me.gypopo.economyshopgui.api.events.PreTransactionEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.core.CoreModule;
import org.kif.reincarceration.cycle.CycleManager;
import org.kif.reincarceration.cycle.CycleModule;
import org.kif.reincarceration.modifier.core.IModifier;
import org.kif.reincarceration.modifier.core.ModifierManager;
import org.kif.reincarceration.modifier.core.ModifierModule;
import org.kif.reincarceration.rank.RankManager;
import org.kif.reincarceration.config.ConfigManager;
import org.kif.reincarceration.rank.RankModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class GUIListener implements Listener {
    private final Reincarceration plugin;
    private final GUIManager guiManager;
    private final CycleManager cycleManager;
    private final RankManager rankManager;
    private final ModifierManager modifierManager;
    private final ConfigManager configManager;
    private static final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

    public GUIListener(Reincarceration plugin) {
        this.plugin = plugin;
        this.guiManager = plugin.getModuleManager().getModule(GUIModule.class).getGuiManager();
        this.cycleManager = plugin.getModuleManager().getModule(CycleModule.class).getCycleManager();
        this.rankManager = plugin.getModuleManager().getModule(RankModule.class).getRankManager();
        this.modifierManager = plugin.getModuleManager().getModule(ModifierModule.class).getModifierManager();
        this.configManager = plugin.getModuleManager().getModule(CoreModule.class).getConfigManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = plainSerializer.serialize(event.getView().title());

        // Cancel all events in our custom GUIs
        if (title.contains("Reincarceration") || title.contains("Cycle") ||
                title.contains("Modifier") || title.contains("Player") ||
                title.contains("Rank Up") || title.contains("Warning")) {
            event.setCancelled(true);

            // Prevent any item movement, even within the inventory
            if (event.getAction().name().contains("MOVE_TO_OTHER_INVENTORY") ||
                    event.getAction().name().contains("COLLECT_TO_CURSOR")) {
                return;
            }
        }

        if (event.getCurrentItem() == null)
            return;

        try {
            switch (title) {
                case "Reincarceration Menu" -> handleMainMenu(player, event);
                case "Player Info" -> handlePlayerInfoMenu(player, event);
                case "Rank Up" -> handleRankUpMenu(player, event);
                case "Complete Cycle" -> handleCompleteCycleMenu(player, event);
                case "Quit Cycle" -> handleQuitCycleMenu(player, event);
                case "Warning: Start Cycle" -> handleStartCycleWarningMenu(player, event);
                case "Cycle Rewards" -> handleRewardsMenu(player, event);
                default -> {
                    if (title.startsWith("Start Cycle")) handleStartCycleMenu(player, event);
                    else if (title.startsWith("Available Modifiers")) handleAvailableModifiersMenu(player, event);
                    else if (title.startsWith("Completed Modifiers")) handleCompletedModifiersMenu(player, event);
                    else if (title.startsWith("Online Players")) handleOnlinePlayersMenu(player, event);
                }
            }
        } catch (Exception e) {
            player.sendMessage(Component.text("An error occurred while processing your request.", NamedTextColor.RED));
            plugin.getLogger().log(Level.SEVERE, "Error in GUI interaction", e);
        }
    }

    private void handleMainMenu(Player player, InventoryClickEvent event) {
        if (Objects.requireNonNull(event.getCurrentItem()).getType() == Material.BARRIER) return;

        switch (event.getCurrentItem().getType()) {
            case BOOK -> guiManager.openPlayerInfoGUI(player);
            case IRON_BARS -> guiManager.openStartCycleGUI(player, 0);
            case EMERALD -> guiManager.openRankUpGUI(player);
            case CARTOGRAPHY_TABLE -> guiManager.openAvailableModifiersGUI(player, 0);
            case PLAYER_HEAD -> guiManager.openOnlinePlayersGUI(player, 0);
            case END_CRYSTAL -> guiManager.openCompleteCycleGUI(player);
            case LEAD -> {
                if (cycleManager.isPlayerInCycle(player)) {
                    guiManager.openQuitCycleGUI(player);
                }
            }
        }
    }

    private void handlePlayerInfoMenu(Player player, InventoryClickEvent event) {
        if (Objects.requireNonNull(event.getCurrentItem()).getType() == Material.RED_WOOL) {
            guiManager.openMainMenu(player);
        }
    }

    private void handleStartCycleMenu(Player player, InventoryClickEvent event) {
        if (handleNavigationButtons(player, event)) return;

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            if (clickedItem.getType() == Material.RABBIT_FOOT) {
                guiManager.openStartCycleWarningGUI(player, createRandomModifier());
            } else {
                String modifierName = getItemName(clickedItem);
                try {
                    IModifier modifier = modifierManager.getModifierByName(modifierName);
                    if (modifier != null) {
                        if (event.isRightClick()) {
                            guiManager.openRewardItemGUI(player, modifier);
                        } else {
                            guiManager.openStartCycleWarningGUI(player, modifier);
                        }
                    }
                } catch (Exception e) {
                    player.sendMessage(Component.text("Error selecting modifier: " + e.getMessage(), NamedTextColor.RED));
                    plugin.getLogger().log(Level.WARNING, "Error selecting modifier", e);
                }
            }
        }
    }

    private void handleRewardsMenu(Player player, InventoryClickEvent event) {
        if (event.getCurrentItem().getType() == Material.REDSTONE_BLOCK) {
            guiManager.openMainMenu(player);
        }
    }

    private void handleStartCycleWarningMenu(Player player, InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null) return;

        if (clickedItem.getType() == Material.EMERALD_BLOCK) {
            ItemStack modifierItem = event.getInventory().getItem(13);
            if (modifierItem != null && modifierItem.hasItemMeta() && modifierItem.getItemMeta().hasLore()) {
                List<Component> lore = modifierItem.getItemMeta().lore();
                if (lore != null && lore.size() > 2) {
                    String modifierName = plainSerializer.serialize(lore.get(2));
                    modifierName = modifierName.substring(modifierName.lastIndexOf(":") + 2);
                    try {
                        IModifier modifier = "Random Challenge".equals(modifierName) ?
                                createRandomModifier() : modifierManager.getModifierByName(modifierName);
                        if (modifier != null) {
                            cycleManager.startNewCycle(player, modifier);
                            player.closeInventory();
                        }
                    } catch (Exception e) {
                        player.sendMessage(Component.text("Error starting cycle: " + e.getMessage(), NamedTextColor.RED));
                        plugin.getLogger().log(Level.WARNING, "Error starting cycle", e);
                    }
                }
            }
        } else if (clickedItem.getType() == Material.REDSTONE_BLOCK) {
            guiManager.openMainMenu(player);
        }
    }

    private void handleRankUpMenu(Player player, InventoryClickEvent event) {
        if (event.getCurrentItem().getType() == Material.RED_WOOL) {
            guiManager.openMainMenu(player);
        } else if (event.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
            try {
                if (rankManager.canRankUp(player)) {
                    rankManager.rankUp(player);
                    player.closeInventory();
                    player.sendMessage(Component.text("You've successfully ranked up!", NamedTextColor.GREEN));
                    guiManager.openRankUpGUI(player);
                } else {
                    player.sendMessage(Component.text("You can't rank up right now.", NamedTextColor.RED));
                }
            } catch (Exception e) {
                player.sendMessage(Component.text("Error ranking up: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().log(Level.WARNING, "Error ranking up player", e);
            }
        }
    }

    private void handleQuitCycleMenu(Player player, InventoryClickEvent event) {
        switch (event.getCurrentItem().getType()) {
            case EMERALD_BLOCK -> {
                cycleManager.quitCycle(player);
                player.closeInventory();
            }
            case REDSTONE_BLOCK -> guiManager.openMainMenu(player);
        }
    }

    private void handleOnlinePlayersMenu(Player player, InventoryClickEvent event) {
        handleNavigationButtons(player, event);
    }

    private void handleCompleteCycleMenu(Player player, InventoryClickEvent event) {
        if (event.getCurrentItem().getType() == Material.RED_WOOL) {
            guiManager.openMainMenu(player);
        } else if (event.getCurrentItem().getType() == Material.EMERALD_BLOCK) {
            try {
                if (cycleManager.isPlayerInCycle(player) && configManager.isMaxRank(rankManager.getPlayerRank(player))) {
                    cycleManager.completeCycle(player);
                    player.closeInventory();
                    player.sendMessage(Component.text("You've successfully completed your cycle!", NamedTextColor.GREEN));
                    guiManager.openMainMenu(player);
                } else {
                    player.sendMessage(Component.text("You can't complete the cycle right now.", NamedTextColor.RED));
                }
            } catch (Exception e) {
                player.sendMessage(Component.text("Error completing cycle: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().log(Level.WARNING, "Error completing cycle", e);
            }
        }
    }

    private void handleAvailableModifiersMenu(Player player, InventoryClickEvent event) {
        if (event.getCurrentItem() == null) return;

        if (event.getCurrentItem().getType() == Material.BOOK) {
            guiManager.openCompletedModifiersGUI(player, 0);
        } else if (event.isRightClick()) {
            String modifierName = getItemName(event.getCurrentItem());
            try {
                IModifier modifier = modifierManager.getModifierByName(modifierName);
                if (modifier != null) {
                    guiManager.openRewardItemGUI(player, modifier);
                }
            } catch (Exception e) {
                player.sendMessage(Component.text("Error selecting modifier: " + e.getMessage(), NamedTextColor.RED));
                plugin.getLogger().log(Level.WARNING, "Error selecting modifier", e);
            }
        } else {
            handleNavigationButtons(player, event);
        }
    }

    private void handleCompletedModifiersMenu(Player player, InventoryClickEvent event) {
        if (event.getCurrentItem().getType() == Material.CARTOGRAPHY_TABLE) {
            guiManager.openAvailableModifiersGUI(player, 0);
        } else {
            handleNavigationButtons(player, event);
        }
    }

    private boolean handleNavigationButtons(Player player, InventoryClickEvent event) {
        String title = plainSerializer.serialize(event.getView().title());
        if (event.getCurrentItem().getType() == Material.RED_WOOL) {
            guiManager.openMainMenu(player);
            return true;
        } else if (event.getCurrentItem().getType() == Material.ARROW) {
            String pageStr = title.substring(title.lastIndexOf("Page ") + 5, title.length() - 1);
            int currentPage = Integer.parseInt(pageStr) - 1;

            String buttonName = getItemName(event.getCurrentItem());
            if ("Next Page".equals(buttonName)) {
                openNextPage(player, title, currentPage);
                return true;
            } else if ("Previous Page".equals(buttonName)) {
                openPreviousPage(player, title, currentPage);
                return true;
            }
        }
        return false;
    }

    private void openNextPage(Player player, String guiName, int currentPage) {
        if (guiName.startsWith("Start Cycle")) {
            guiManager.openStartCycleGUI(player, currentPage + 1);
        } else if (guiName.startsWith("Available Modifiers")) {
            guiManager.openAvailableModifiersGUI(player, currentPage + 1);
        } else if (guiName.startsWith("Completed Modifiers")) {
            guiManager.openCompletedModifiersGUI(player, currentPage + 1);
        } else if (guiName.startsWith("Online Players")) {
            guiManager.openOnlinePlayersGUI(player, currentPage + 1);
        }
    }

    private void openPreviousPage(Player player, String guiName, int currentPage) {
        if (guiName.startsWith("Start Cycle")) {
            guiManager.openStartCycleGUI(player, currentPage - 1);
        } else if (guiName.startsWith("Available Modifiers")) {
            guiManager.openAvailableModifiersGUI(player, currentPage - 1);
        } else if (guiName.startsWith("Completed Modifiers")) {
            guiManager.openCompletedModifiersGUI(player, currentPage - 1);
        } else if (guiName.startsWith("Online Players")) {
            guiManager.openOnlinePlayersGUI(player, currentPage - 1);
        }
    }

    private IModifier createRandomModifier() {
        return new IModifier() {
            @Override
            public String getId() {
                return "random";
            }

            @Override
            public String getName() {
                return "Random Challenge";
            }

            @Override
            public String getDescription() {
                return "A randomly selected challenge";
            }

            @Override
            public List<ItemStack> getItemRewards() {
                return new ArrayList<>();
            }

            @Override
            public void apply(Player player) {
            } // Empty implementation

            @Override
            public void remove(Player player) {
            } // Empty implementation

            @Override
            public boolean isActive(Player player) {
                return false;
            } // Always return false for the placeholder

            @Override
            public boolean isSecret() {
                return false;
            }

            @Override
            public boolean handleBlockBreak(BlockBreakEvent event) {
                return false;
            }

            @Override
            public boolean handleFishing(PlayerFishEvent event) {
                return false;
            }

            @Override
            public boolean handleSellTransaction(PreTransactionEvent event) {
                return false;
            }

            @Override
            public boolean handleBuyTransaction(PreTransactionEvent event) {
                return false;
            }

            @Override
            public boolean handlePostTransaction(PostTransactionEvent event) {
                return false;
            }

            @Override
            public boolean handleVaultAccess(PlayerInteractEvent event) {
                return false;
            }
            // Implement any other abstract methods from IModifier interface here
            // For example:
            // @Override
            // public boolean handleBlockBreak(BlockBreakEvent event) { return false; }
            // @Override
            // public boolean handleFishing(PlayerFishEvent event) { return false; }
            // Add any other methods required by your IModifier interface
        };
    }

    private String getItemName(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                return plainSerializer.serialize(Objects.requireNonNull(meta.displayName()));
            }
        }
        return "";
    }
}