package org.kif.reincarceration.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.config.ConfigManager;
import org.kif.reincarceration.cycle.CycleManager;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.economy.EconomyManager;
import org.kif.reincarceration.modifier.core.ModifierManager;
import org.kif.reincarceration.rank.RankManager;
import org.kif.reincarceration.permission.PermissionManager;
import org.kif.reincarceration.rewards.CycleReward;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.modifier.core.IModifier;
import org.kif.reincarceration.util.RewardUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class GUIManager {
    private final Reincarceration plugin;
    private final ConfigManager configManager;
    private final CycleManager cycleManager;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final RankManager rankManager;
    private final ModifierManager modifierManager;
    private final Map<String, Material> modifierMaterials = new HashMap<>();
    private static final int ITEMS_PER_PAGE = 45;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public GUIManager(final Reincarceration plugin, GUIModule guiModule, ConfigManager configManager,
                      CycleManager cycleManager, DataManager dataManager, EconomyManager economyManager,
                      RankManager rankManager, PermissionManager permissionManager, ModifierManager modifierManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.cycleManager = cycleManager;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
        this.rankManager = rankManager;
        this.modifierManager = modifierManager;

        // Initialize modifier materials
        modifierMaterials.put("ore_sickness", Material.DEEPSLATE_DIAMOND_ORE);
        modifierMaterials.put("immolation", Material.BLAZE_POWDER);
        modifierMaterials.put("compact", Material.CHEST);
        modifierMaterials.put("angler", Material.FISHING_ROD);
        modifierMaterials.put("tortoise", Material.TURTLE_HELMET);
        modifierMaterials.put("neolithic", Material.MUTTON);
        modifierMaterials.put("hardcore", Material.BONE);
        modifierMaterials.put("decrepit", Material.BOWL);
        modifierMaterials.put("lumberjack", Material.WOODEN_AXE);
        modifierMaterials.put("gambler", Material.GOLD_INGOT);

        ConsoleUtil.sendSuccess("GUIManager initialized with all required components");
    }

    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, Component.text("Reincarceration Menu", NamedTextColor.DARK_PURPLE));

        inventory.setItem(11, createEnchantedGuiItem(Material.BOOK, "<blue>Player Info", "View your current status"));

        try {
            boolean inCycle = cycleManager.isPlayerInCycle(player);
            int currentRank = rankManager.getPlayerRank(player);
            boolean isMaxRank = configManager.isMaxRank(currentRank);

            if (!inCycle && player.hasPermission("reincarceration.startcycle")) {
                inventory.setItem(21, createGuiItem(Material.IRON_BARS, "<green>Start Cycle",
                        "Begin a new cycle ($" + configManager.getEntryFee() + ")",
                        "All Cycles reward a plaque with the cycle name, date, and time it took you to complete it!"));
            } else {
                inventory.setItem(21, createDisabledGuiItem(Material.BARRIER, "<gray>Start Cycle",
                        "You can't start a new cycle now"));
            }

            if (inCycle && !isMaxRank && player.hasPermission("reincarceration.rankup")) {
                inventory.setItem(23, createGuiItem(Material.EMERALD, "<green>Rank Up",
                        "Advance to the next rank ($" + configManager.getRankUpCost(rankManager.getPlayerRank(player)) + ")"));
            } else {
                inventory.setItem(23, createDisabledGuiItem(Material.BARRIER, "<gray>Rank Up", "You can't rank up now"));
            }

            if (player.hasPermission("reincarceration.listmodifiers")) {
                inventory.setItem(15, createGuiItem(Material.CARTOGRAPHY_TABLE,
                        "<light_purple>Modifier List", "View modifier information"));
            } else {
                inventory.setItem(15, createDisabledGuiItem(Material.BARRIER, "<gray>Modifier List",
                        "You don't have permission"));
            }

            if (player.hasPermission("reincarceration.viewonlineplayers")) {
                inventory.setItem(13, createGuiItem(Material.PLAYER_HEAD, "<blue>Online Players",
                        "View info of online players"));
            } else {
                inventory.setItem(13, createDisabledGuiItem(Material.BARRIER, "<gray>Online Players",
                        "You don't have permission"));
            }

            if (inCycle && isMaxRank && player.hasPermission("reincarceration.completecycle")) {
                inventory.setItem(25, createGuiItem(Material.END_CRYSTAL, "<red>Complete Cycle",
                        "Complete your current cycle"));
            } else {
                inventory.setItem(25, createDisabledGuiItem(Material.BARRIER, "<gray>Complete Cycle",
                        "You can't complete a cycle now"));
            }

            if (cycleManager.isPlayerInCycle(player)) {
                inventory.setItem(19, createGuiItem(Material.LEAD, "<red>Quit Cycle", "End your current cycle"));
            } else {
                inventory.setItem(19, createDisabledGuiItem(Material.BARRIER, "<gray>Quit Cycle",
                        "You can't end a cycle now"));
            }

            // Add player status summary
            List<Component> statusLore = new ArrayList<>();
            if (inCycle) {
                statusLore.add(Component.text("Rank: " + configManager.getRankName(currentRank)));
            }
            statusLore.add(Component.text("Cycles Completed: " + dataManager.getPlayerCycleCount(player)));
            statusLore.add(Component.text(inCycle ? "Currently in a cycle" : "Not in a cycle"));
            if (inCycle) {
                IModifier activeModifier = modifierManager.getActiveModifier(player);
                if (activeModifier != null) {
                    statusLore.add(Component.text("Active Modifier: " + activeModifier.getName()));
                } else {
                    statusLore.add(Component.text("Active Modifier: None"));
                    ConsoleUtil.sendDebug("Player " + player.getName() + " is in cycle but has no active modifier.");
                }
            }
            ItemStack statusSummary = createGuiItem(Material.PAPER, "<gold>Your Status", statusLore);

            inventory.setItem(31, statusSummary);

        } catch (SQLException e) {
            player.sendMessage(Component.text("Error retrieving player data.", NamedTextColor.RED));
            e.printStackTrace();
        }

        // Add glass panes to fill empty slots
        ItemStack filler = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
    }

    public void openStartCycleGUI(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                Component.text("Start Cycle (Page " + (page + 1) + ")", NamedTextColor.AQUA));

        try {
            List<IModifier> availableModifiers = modifierManager.getAvailableModifiers(player);
            int totalPages = (availableModifiers.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

            // Add Random Challenge option
            ItemStack randomItem = createGuiItem(Material.RABBIT_FOOT, "<gold>Random Challenge",
                    "<yellow>Click to start a cycle with a random modifier! " + configManager.getRandomModifierDiscount() + "%");
            inventory.setItem(45, randomItem);

            for (int i = page * ITEMS_PER_PAGE; i < Math.min((page + 1) * ITEMS_PER_PAGE, availableModifiers.size()); i++) {
                IModifier modifier = availableModifiers.get(i);
                inventory.addItem(createModifierItem(modifier, "<aqua>Available", true));
            }

            setNavigationButtons(inventory, page, totalPages);
        } catch (SQLException e) {
            player.sendMessage(Component.text("Error retrieving available modifiers.", NamedTextColor.RED));
            e.printStackTrace();
        }

        player.openInventory(inventory);
    }

    public void openRewardItemGUI(Player player, IModifier modifier) {
        final CycleReward reward = RewardUtil.getCycleRewardForModifier(modifier, plugin);
        if (reward == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("Cycle Rewards", NamedTextColor.GOLD));
        final List<ItemStack> items = reward.getItems()
                .stream()
                .map(RewardUtil::buildItemStackFromRewardItem)
                .toList();
        for (int i = 0; i < Math.min(18, items.size()); i++) {
            final ItemStack item = items.get(i);
            inventory.setItem(i, item);
        }

        ItemStack cancelItem = createGuiItem(Material.REDSTONE_BLOCK, "<red>Cancel",
                "<yellow>Click to return to the main menu");

        inventory.setItem(22, cancelItem);

        player.openInventory(inventory);
    }

    public void openStartCycleWarningGUI(Player player, IModifier selectedModifier) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("Warning: Start Cycle", NamedTextColor.RED));

        ItemStack warningSign = createGuiItem(Material.BARRIER, "<red>Warning!",
                "<yellow>Starting a cycle will kill you!",
                "<yellow>Ensure your items are stored!",
                "<yellow>Selected Modifier: " + (selectedModifier.getId().equals("random") ? "Random Challenge" : selectedModifier.getName()),
                "");

        ItemStack confirmItem = createGuiItem(Material.EMERALD_BLOCK, "<green>Confirm Start Cycle",
                "<yellow>Click to start the cycle" + (selectedModifier.getId().equals("random") ? " with a random modifier"
                        : " with " + selectedModifier.getName()));

        ItemStack cancelItem = createGuiItem(Material.REDSTONE_BLOCK, "<red>Cancel",
                "<yellow>Click to return to the main menu");

        inventory.setItem(13, warningSign);
        inventory.setItem(11, confirmItem);
        inventory.setItem(15, cancelItem);

        player.openInventory(inventory);
    }

    public void openPlayerInfoGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("Player Info", NamedTextColor.GOLD));

        try {
            int currentRank = rankManager.getPlayerRank(player);
            int cycleCount = dataManager.getPlayerCycleCount(player);
            BigDecimal balance = economyManager.getBalance(player);
            BigDecimal storedBalance = dataManager.getStoredBalance(player);
            boolean inCycle = cycleManager.isPlayerInCycle(player);

            if (inCycle) {
                inventory.setItem(11, createGuiItem(Material.DIAMOND_SWORD, "<aqua>Current Rank",
                        "Rank: " + configManager.getRankName(currentRank)));

                inventory.setItem(13, createGuiItem(Material.GOLD_INGOT, "<yellow>Economy",
                        "Balance: " + balance,
                        "Stored Balance: " + storedBalance));

                inventory.setItem(15, createGuiItem(Material.CLOCK, "<green>Cycle Info",
                        "Total Completed Cycles: " + cycleCount,
                        "Currently in Cycle: Yes"));
            } else {
                inventory.setItem(13, createGuiItem(Material.CLOCK, "<green>Cycle Info",
                        "Total Completed Cycles: " + cycleCount,
                        "Currently in Cycle: No"));
            }

        } catch (SQLException e) {
            player.sendMessage(Component.text("Error retrieving player data.", NamedTextColor.RED));
            e.printStackTrace();
        }

        inventory.setItem(26, createBackButton());

        player.openInventory(inventory);
    }

    public void openRankUpGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("Rank Up", NamedTextColor.GREEN));

        int currentRank = rankManager.getPlayerRank(player);
        BigDecimal balance = economyManager.getBalance(player);
        BigDecimal rankUpCost = configManager.getRankUpCost(currentRank);

        inventory.setItem(11, createGuiItem(Material.DIAMOND_SWORD, "<aqua>Current Rank",
                "Rank: " + configManager.getRankName(currentRank)));

        inventory.setItem(13, createGuiItem(Material.GOLD_INGOT, "<yellow>Economy",
                "Current Balance: " + balance,
                "Cost to Rank Up: " + rankUpCost));

        Material rankUpMaterial = (balance.compareTo(rankUpCost) >= 0) ? Material.EMERALD_BLOCK
                : Material.REDSTONE_BLOCK;
        String rankUpStatus = (balance.compareTo(rankUpCost) >= 0) ? "Click to Rank Up!" : "Insufficient Funds";
        inventory.setItem(15, createGuiItem(rankUpMaterial, "<green>Rank Up", rankUpStatus));

        inventory.setItem(26, createBackButton());

        player.openInventory(inventory);
    }

    public void openAvailableModifiersGUI(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                Component.text("Available Modifiers (Page " + (page + 1) + ")", NamedTextColor.AQUA));

        try {
            List<IModifier> availableModifiers = modifierManager.getAvailableModifiers(player);
            IModifier activeModifier = modifierManager.getActiveModifier(player);

            // Filter out the active modifier from the available modifiers
            if (activeModifier != null) {
                availableModifiers.removeIf(modifier -> modifier.getId().equals(activeModifier.getId()));
            }

            int totalPages = (availableModifiers.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

            for (int i = page * ITEMS_PER_PAGE; i < Math.min((page + 1) * ITEMS_PER_PAGE, availableModifiers.size()); i++) {
                IModifier modifier = availableModifiers.get(i);
                inventory.addItem(createModifierItem(modifier, "<aqua>Available", true));
            }

            setNavigationButtons(inventory, page, totalPages);

            // Display current modifier if in cycle
            if (activeModifier != null) {
                inventory.setItem(53, createModifierItem(activeModifier, "<green>Active Modifier", false));
            }

            inventory.setItem(45, createGuiItem(Material.BOOK, "<gold>Completed Modifiers", "View completed modifiers"));

        } catch (SQLException e) {
            player.sendMessage(Component.text("Error retrieving modifier data.", NamedTextColor.RED));
            e.printStackTrace();
        }

        player.openInventory(inventory);
    }

    public void openCompletedModifiersGUI(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(null, 54,
                Component.text("Completed Modifiers (Page " + (page + 1) + ")", NamedTextColor.GOLD));

        try {
            List<String> completedModifierIds = dataManager.getCompletedModifiers(player);
            List<IModifier> completedModifiers = new ArrayList<>();
            for (String id : completedModifierIds) {
                IModifier modifier = modifierManager.getModifierById(id);
                if (modifier != null) {
                    completedModifiers.add(modifier);
                }
            }

            int totalPages = (completedModifiers.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

            for (int i = page * ITEMS_PER_PAGE; i < Math.min((page + 1) * ITEMS_PER_PAGE, completedModifiers.size()); i++) {
                IModifier modifier = completedModifiers.get(i);
                inventory.addItem(createModifierItem(modifier, "<gold>Completed", false));
            }

            setNavigationButtons(inventory, page, totalPages);

            // Display current modifier if in cycle
            IModifier activeModifier = modifierManager.getActiveModifier(player);
            if (activeModifier != null) {
                inventory.setItem(53, createModifierItem(activeModifier, "<green>Active Modifier", false));
            }

            inventory.setItem(45, createGuiItem(Material.CARTOGRAPHY_TABLE, "<aqua>Available Modifiers",
                    "View available modifiers"));

        } catch (SQLException e) {
            player.sendMessage(Component.text("Error retrieving modifier data.", NamedTextColor.RED));
            e.printStackTrace();
        }

        player.openInventory(inventory);
    }

    public void openOnlinePlayersGUI(Player player, int page) {
        List<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.hasPermission("reincarceration.admin.invisible"))
                .collect(Collectors.toList());
        int totalPages = (onlinePlayers.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

        Inventory inventory = Bukkit.createInventory(null, 54,
                Component.text("Online Players (Page " + (page + 1) + ")", NamedTextColor.BLUE));

        for (int i = page * ITEMS_PER_PAGE; i < Math.min((page + 1) * ITEMS_PER_PAGE, onlinePlayers.size()); i++) {
            Player onlinePlayer = onlinePlayers.get(i);
            ItemStack skull = createPlayerSkull(onlinePlayer);
            inventory.addItem(skull);
        }

        setNavigationButtons(inventory, page, totalPages);

        player.openInventory(inventory);
    }

    public void openCompleteCycleGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("Complete Cycle", NamedTextColor.RED));

        boolean inCycle = cycleManager.isPlayerInCycle(player);
        int currentRank = rankManager.getPlayerRank(player);
        boolean canComplete = inCycle && configManager.isMaxRank(currentRank)
                && economyManager.hasEnoughBalance(player, configManager.getRankUpCost(currentRank));

        inventory.setItem(11, createGuiItem(Material.BOOK, "<yellow>Current Status",
                "In Cycle: " + (inCycle ? "Yes" : "No"),
                "Current Rank: " + configManager.getRankName(currentRank)));

        Material completeMaterial = canComplete ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        String completeStatus = canComplete ? "Click to Complete Cycle!" : "Cannot Complete Cycle";
        inventory.setItem(15, createGuiItem(completeMaterial, "<red>Complete Cycle", completeStatus));

        inventory.setItem(26, createBackButton());

        player.openInventory(inventory);
    }

    public void openQuitCycleGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, Component.text("Quit Cycle", NamedTextColor.RED));

        inventory.setItem(11, createGuiItem(Material.EMERALD_BLOCK, "<green>Confirm Quit",
                "Click to quit the cycle",
                "<red>Warning: You will not receive a refund"));

        inventory.setItem(15, createGuiItem(Material.REDSTONE_BLOCK, "<red>Cancel",
                "Return to the main menu"));

        inventory.setItem(22, createGuiItem(Material.PAPER, "<yellow>Info",
                "Quitting will end your current cycle",
                "You will lose all progress",
                "No refund will be given"));

        player.openInventory(inventory);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(miniMessage.deserialize(name));
        List<Component> loreComponents = Arrays.stream(lore)
                .map(miniMessage::deserialize)
                .collect(Collectors.toList());
        meta.lore(loreComponents);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(miniMessage.deserialize(name));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnchantedGuiItem(Material material, String name, String... lore) {
        ItemStack item = createGuiItem(material, name, lore);
        item.addUnsafeEnchantment(Enchantment.LUCK_OF_THE_SEA, 1);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDisabledGuiItem(Material material, String name, String... lore) {
        ItemStack item = createGuiItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> wrapText(String text, int lineLength) {
        List<Component> wrappedText = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        String[] words = text.split("\\s+");
        Component colorComponent = Component.empty().color(NamedTextColor.GRAY);

        for (String word : words) {
            if (line.length() + word.length() > lineLength) {
                wrappedText.add(colorComponent.append(Component.text(line.toString().trim())));
                line = new StringBuilder();
            }
            // Check if the word contains a color code (you might need to adjust this for MiniMessage format)
            if (word.startsWith("<") && word.endsWith(">")) {
                colorComponent = miniMessage.deserialize(word);
            } else {
                line.append(word).append(" ");
            }
        }
        if (!line.isEmpty()) {
            wrappedText.add(colorComponent.append(Component.text(line.toString().trim())));
        }
        return wrappedText;
    }

    private ItemStack createModifierItem(IModifier modifier, String status, boolean appendRewards) {
        Material material = modifierMaterials.getOrDefault(modifier.getId(), Material.ENDER_PEARL);
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(modifier.getName(), NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize(status));
        lore.addAll(wrapText(modifier.getDescription(), 40));

        if (appendRewards) {
            CycleReward reward = RewardUtil.getCycleRewardForModifier(modifier, plugin);
            if (reward != null) {
                lore.add(Component.empty());
                lore.addAll(RewardUtil.getRewardLore(reward));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerSkull(Player player) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(Component.text(player.getName(), NamedTextColor.YELLOW));

        List<Component> lore = new ArrayList<>();
        try {
            int currentRank = rankManager.getPlayerRank(player);
            int cycleCount = dataManager.getPlayerCycleCount(player);
            BigDecimal balance = economyManager.getBalance(player);
            BigDecimal storedBalance = dataManager.getStoredBalance(player);
            boolean inCycle = cycleManager.isPlayerInCycle(player);
            IModifier activeModifier = modifierManager.getActiveModifier(player);

            if (inCycle) {
                lore.add(Component.text("Rank: " + configManager.getRankName(currentRank), NamedTextColor.GOLD));
            }
            lore.add(Component.text("Cycle Count: " + cycleCount, NamedTextColor.AQUA));
            lore.add(Component.text("Balance: " + balance, NamedTextColor.GREEN));
            lore.add(Component.text("Stored Balance: " + storedBalance, NamedTextColor.GREEN));
            lore.add(Component.text("In Cycle: " + (inCycle ? "Yes" : "No"), NamedTextColor.LIGHT_PURPLE));
            if (activeModifier != null) {
                lore.add(Component.text("Active Modifier: " + activeModifier.getName(), NamedTextColor.LIGHT_PURPLE));
            }
        } catch (SQLException e) {
            lore.add(Component.text("Error retrieving player data", NamedTextColor.RED));
        }

        meta.lore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack createBackButton() {
        return createGuiItem(Material.RED_WOOL, "<red>Back", "Return to previous menu");
    }

    private ItemStack createNextPageButton() {
        return createGuiItem(Material.ARROW, "<green>Next Page", "Go to the next page");
    }

    private ItemStack createPreviousPageButton() {
        return createGuiItem(Material.ARROW, "<green>Previous Page", "Go to the previous page");
    }

    private void setNavigationButtons(Inventory inventory, int currentPage, int totalPages) {
        inventory.setItem(inventory.getSize() - 5, createBackButton());

        if (currentPage > 0) {
            inventory.setItem(inventory.getSize() - 9, createPreviousPageButton());
        }

        if (currentPage < totalPages - 1) {
            inventory.setItem(inventory.getSize() - 1, createNextPageButton());
        }
    }
}