package org.kif.reincarceration.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.kif.reincarceration.config.ConfigManager;
import org.kif.reincarceration.cycle.CycleManager;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.economy.EconomyManager;
import org.kif.reincarceration.modifier.core.ModifierManager;
import org.kif.reincarceration.rank.RankManager;
import org.kif.reincarceration.permission.PermissionManager;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.modifier.core.IModifier;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class GUIManager {
    private final GUIModule guiModule;
    private final ConfigManager configManager;
    private final CycleManager cycleManager;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final RankManager rankManager;
    private final PermissionManager permissionManager;
    private final ModifierManager modifierManager;
    private final Map<String, Material> modifierMaterials = new HashMap<>();
    private static final int ITEMS_PER_PAGE = 45;

    public GUIManager(GUIModule guiModule, ConfigManager configManager, CycleManager cycleManager,
                      DataManager dataManager, EconomyManager economyManager, RankManager rankManager,
                      PermissionManager permissionManager, ModifierManager modifierManager) {
        this.guiModule = guiModule;
        this.configManager = configManager;
        this.cycleManager = cycleManager;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
        this.rankManager = rankManager;
        this.permissionManager = permissionManager;
        this.modifierManager = modifierManager;

        // Initialize modifier materials
        modifierMaterials.put("ore_sickness", Material.DEEPSLATE_DIAMOND_ORE);
        modifierMaterials.put("combustion", Material.BLAZE_POWDER);
        modifierMaterials.put("compact", Material.CHEST);
        modifierMaterials.put("angler", Material.FISHING_ROD);
        modifierMaterials.put("tortoise", Material.TURTLE_HELMET);
        modifierMaterials.put("neolithic", Material.MUTTON);
        modifierMaterials.put("hardcore", Material.BONE);
        modifierMaterials.put("lumberjack", Material.WOODEN_AXE);

        ConsoleUtil.sendSuccess("GUIManager initialized with all required components");
    }

    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, ChatColor.DARK_PURPLE + "Reincarceration Menu");

        // Always show Player Info
        inventory.setItem(11, createEnchantedGuiItem(Material.BOOK, ChatColor.BLUE + "Player Info", "View your current status"));

        try {

            boolean inCycle = cycleManager.isPlayerInCycle(player);
            int currentRank = rankManager.getPlayerRank(player);
            boolean isMaxRank = configManager.isMaxRank(currentRank);

            if (!inCycle && player.hasPermission("reincarceration.startcycle")) {
                inventory.setItem(21, createGuiItem(Material.IRON_BARS, ChatColor.GREEN + "Start Cycle", "Begin a new reincarceration cycle"));
            } else {
                inventory.setItem(21, createDisabledGuiItem(Material.BARRIER, ChatColor.GRAY + "Start Cycle", "You can't start a new cycle now"));
            }

            if (inCycle && !isMaxRank && player.hasPermission("reincarceration.rankup")) {
                inventory.setItem(23, createGuiItem(Material.EMERALD, ChatColor.GREEN + "Rank Up", "Advance to the next rank"));
            } else {
                inventory.setItem(23, createDisabledGuiItem(Material.BARRIER, ChatColor.GRAY + "Rank Up", "You can't rank up now"));
            }

            if (player.hasPermission("reincarceration.listmodifiers")) {
                inventory.setItem(15, createGuiItem(Material.CARTOGRAPHY_TABLE, ChatColor.LIGHT_PURPLE + "Modifier List", "View modifier information"));
            } else {
                inventory.setItem(15, createDisabledGuiItem(Material.BARRIER, ChatColor.GRAY + "Modifier List", "You don't have permission"));
            }

            if (player.hasPermission("reincarceration.viewonlineplayers")) {
                inventory.setItem(13, createGuiItem(Material.PLAYER_HEAD, ChatColor.BLUE + "Online Players", "View info of online players"));
            } else {
                inventory.setItem(13, createDisabledGuiItem(Material.BARRIER, ChatColor.GRAY + "Online Players", "You don't have permission"));
            }

            if (inCycle && isMaxRank && player.hasPermission("reincarceration.completecycle")) {
                inventory.setItem(25, createGuiItem(Material.END_CRYSTAL, ChatColor.RED + "Complete Cycle", "Complete your current cycle"));
            } else {
                inventory.setItem(25, createDisabledGuiItem(Material.BARRIER, ChatColor.GRAY + "Complete Cycle", "You can't complete a cycle now"));
            }

            if (cycleManager.isPlayerInCycle(player)) {
                inventory.setItem(19, createGuiItem(Material.LEAD, ChatColor.RED + "Quit Cycle", "End your current cycle"));
            } else {
                inventory.setItem(19, createDisabledGuiItem(Material.BARRIER, ChatColor.GRAY + "Quit Cycle", "You can't end a cycle now"));
            }

            // Add player status summary
            List<String> statusLore = new ArrayList<>();
            if (inCycle) {
                statusLore.add("Rank: " + configManager.getRankName(currentRank));
            }
            statusLore.add("Cycles Completed: " + dataManager.getPlayerCycleCount(player));
            statusLore.add((inCycle ? "Currently in a cycle" : "Not in a cycle"));
            if (inCycle) {
                statusLore.add("Active Modifier: " + modifierManager.getActiveModifier(player).getName());
            }
            ItemStack statusSummary = createGuiItem(Material.PAPER, ChatColor.GOLD + "Your Status", statusLore.toArray(new String[0]));

            inventory.setItem(31, statusSummary);

            inventory.setItem(31, statusSummary);

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "Error retrieving player data.");
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

    public void openPlayerInfoGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Player Info");

        try {
            int currentRank = rankManager.getPlayerRank(player);
            int cycleCount = dataManager.getPlayerCycleCount(player);
            BigDecimal balance = economyManager.getBalance(player);
            BigDecimal storedBalance = dataManager.getStoredBalance(player);
            boolean inCycle = cycleManager.isPlayerInCycle(player);

            if (inCycle) {
                inventory.setItem(11, createGuiItem(Material.DIAMOND_SWORD, ChatColor.AQUA + "Current Rank",
                        "Rank: " + configManager.getRankName(currentRank),
                        "Level: " + currentRank));

                inventory.setItem(13, createGuiItem(Material.GOLD_INGOT, ChatColor.YELLOW + "Economy",
                        "Balance: " + balance,
                        "Stored Balance: " + storedBalance));

                inventory.setItem(15, createGuiItem(Material.CLOCK, ChatColor.GREEN + "Cycle Info",
                        "Total Completed Cycles: " + cycleCount,
                        "Currently in Cycle: " + "Yes"));
            } else {
                inventory.setItem(13, createGuiItem(Material.CLOCK, ChatColor.GREEN + "Cycle Info",
                        "Total Completed Cycles: " + cycleCount,
                        "Currently in Cycle: " + (inCycle ? "Yes" : "No")));
            }

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "Error retrieving player data.");
            e.printStackTrace();
        }

        inventory.setItem(26, createBackButton());

        player.openInventory(inventory);
    }

    public void openStartCycleGUI(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "Start Cycle (Page " + (page + 1) + ")");

        try {
            List<IModifier> availableModifiers = modifierManager.getAvailableModifiers(player);
            int totalPages = (availableModifiers.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

            for (int i = page * ITEMS_PER_PAGE; i < Math.min((page + 1) * ITEMS_PER_PAGE, availableModifiers.size()); i++) {
                IModifier modifier = availableModifiers.get(i);
                inventory.addItem(createModifierItem(modifier, ChatColor.AQUA + "Available"));
            }

            setNavigationButtons(inventory, page, totalPages);
        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "Error retrieving available modifiers.");
            e.printStackTrace();
        }

        player.openInventory(inventory);
    }

    public void openRankUpGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.GREEN + "Rank Up");

        int currentRank = rankManager.getPlayerRank(player);
        BigDecimal balance = economyManager.getBalance(player);
        BigDecimal rankUpCost = configManager.getRankUpCost(currentRank);

        inventory.setItem(11, createGuiItem(Material.DIAMOND_SWORD, ChatColor.AQUA + "Current Rank",
                "Rank: " + configManager.getRankName(currentRank),
                "Level: " + currentRank));

        inventory.setItem(13, createGuiItem(Material.GOLD_INGOT, ChatColor.YELLOW + "Economy",
                "Current Balance: " + balance,
                "Cost to Rank Up: " + rankUpCost));

        Material rankUpMaterial = (balance.compareTo(rankUpCost) >= 0) ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        String rankUpStatus = (balance.compareTo(rankUpCost) >= 0) ? "Click to Rank Up!" : "Insufficient Funds";
        inventory.setItem(15, createGuiItem(rankUpMaterial, ChatColor.GREEN + "Rank Up", rankUpStatus));

        inventory.setItem(26, createBackButton());

        player.openInventory(inventory);
    }

//    public void openModifierListGUI(Player player, int page) {
//        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.LIGHT_PURPLE + "Modifier List (Page " + (page + 1) + ")");
//
//        try {
//            IModifier activeModifier = modifierManager.getActiveModifier(player);
//            List<IModifier> availableModifiers = modifierManager.getAvailableModifiers(player);
//            List<String> completedModifierIds = dataManager.getCompletedModifiers(player);
//
//            List<ItemStack> allModifierItems = new ArrayList<>();
//
//            if (activeModifier != null) {
//                allModifierItems.add(createModifierItem(activeModifier, ChatColor.GREEN + "Active Modifier"));
//            }
//
//            for (IModifier modifier : availableModifiers) {
//                allModifierItems.add(createModifierItem(modifier, ChatColor.AQUA + "Available"));
//            }
//
//            for (String modifierId : completedModifierIds) {
//                IModifier modifier = modifierManager.getModifier(modifierId);
//                if (modifier != null) {
//                    allModifierItems.add(createModifierItem(modifier, ChatColor.GOLD + "Completed"));
//                }
//            }
//
//            int totalPages = (allModifierItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
//
//            for (int i = page * ITEMS_PER_PAGE; i < Math.min((page + 1) * ITEMS_PER_PAGE, allModifierItems.size()); i++) {
//                inventory.addItem(allModifierItems.get(i));
//            }
//
//            setNavigationButtons(inventory, page, totalPages);
//        } catch (SQLException e) {
//            player.sendMessage(ChatColor.RED + "Error retrieving modifier data.");
//            e.printStackTrace();
//        }
//
//        player.openInventory(inventory);
//    }

    public void openAvailableModifiersGUI(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.AQUA + "Available Modifiers (Page " + (page + 1) + ")");

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
                inventory.addItem(createModifierItem(modifier, ChatColor.AQUA + "Available"));
            }

            setNavigationButtons(inventory, page, totalPages);

            // Display current modifier if in cycle
            if (activeModifier != null) {
                inventory.setItem(53, createModifierItem(activeModifier, ChatColor.GREEN + "Active Modifier"));
            }

            inventory.setItem(45, createGuiItem(Material.BOOK, ChatColor.GOLD + "Completed Modifiers", "View completed modifiers"));

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "Error retrieving modifier data.");
            e.printStackTrace();
        }

        player.openInventory(inventory);
    }


    public void openCompletedModifiersGUI(Player player, int page) {
        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.GOLD + "Completed Modifiers (Page " + (page + 1) + ")");

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
                inventory.addItem(createModifierItem(modifier, ChatColor.GOLD + "Completed"));
            }

            setNavigationButtons(inventory, page, totalPages);

            // Display current modifier if in cycle
            IModifier activeModifier = modifierManager.getActiveModifier(player);
            if (activeModifier != null) {
                inventory.setItem(53, createModifierItem(activeModifier, ChatColor.GREEN + "Active Modifier"));
            }

            inventory.setItem(45, createGuiItem(Material.CARTOGRAPHY_TABLE, ChatColor.AQUA + "Available Modifiers", "View available modifiers"));

        } catch (SQLException e) {
            player.sendMessage(ChatColor.RED + "Error retrieving modifier data.");
            e.printStackTrace();
        }

        player.openInventory(inventory);
    }

    public void openOnlinePlayersGUI(Player player, int page) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int totalPages = (onlinePlayers.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;

        Inventory inventory = Bukkit.createInventory(null, 54, ChatColor.BLUE + "Online Players (Page " + (page + 1) + ")");

        for (int i = page * ITEMS_PER_PAGE; i < Math.min((page + 1) * ITEMS_PER_PAGE, onlinePlayers.size()); i++) {
            Player onlinePlayer = onlinePlayers.get(i);
            ItemStack skull = createPlayerSkull(onlinePlayer);
            inventory.addItem(skull);
        }

        setNavigationButtons(inventory, page, totalPages);

        player.openInventory(inventory);
    }

    public void openCompleteCycleGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.RED + "Complete Cycle");

        boolean inCycle = cycleManager.isPlayerInCycle(player);
        int currentRank = rankManager.getPlayerRank(player);
        boolean canComplete = inCycle && configManager.isMaxRank(currentRank);

        inventory.setItem(11, createGuiItem(Material.BOOK, ChatColor.YELLOW + "Current Status",
                "In Cycle: " + (inCycle ? "Yes" : "No"),
                "Current Rank: " + configManager.getRankName(currentRank)));

        Material completeMaterial = canComplete ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK;
        String completeStatus = canComplete ? "Click to Complete Cycle!" : "Cannot Complete Cycle";
        inventory.setItem(15, createGuiItem(completeMaterial, ChatColor.RED + "Complete Cycle", completeStatus));

        inventory.setItem(26, createBackButton());

        player.openInventory(inventory);
    }

    public void openQuitCycleGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.RED + "Quit Cycle");

        inventory.setItem(11, createGuiItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Confirm Quit",
                "Click to quit the cycle",
                ChatColor.RED + "Warning: You will not receive a refund"));

        inventory.setItem(15, createGuiItem(Material.REDSTONE_BLOCK, ChatColor.RED + "Cancel",
                "Return to the main menu"));

        inventory.setItem(22, createGuiItem(Material.PAPER, ChatColor.YELLOW + "Info",
                "Quitting will end your current cycle",
                "You will lose all progress",
                "No refund will be given"));

        player.openInventory(inventory);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnchantedGuiItem(Material material, String name, String... lore) {
        ItemStack item = createGuiItem(material, name, lore);
        item.addUnsafeEnchantment(Enchantment.LUCK, 1);
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

    private List<String> wrapText(String text, int lineLength) {
        List<String> wrappedText = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        String[] words = text.split("\\s+");
        String colorCode = ChatColor.GRAY.toString(); // Initialize with gray color

        for (String word : words) {
            if (line.length() + word.length() > lineLength) {
                wrappedText.add(colorCode + line.toString().trim());
                line = new StringBuilder();
            }
            // Check if the word contains a color code
            if (word.matches("§[0-9a-fk-or]")) {
                colorCode = word;
            }
            line.append(word).append(" ");
        }
        if (line.length() > 0) {
            wrappedText.add(colorCode + line.toString().trim());
        }
        return wrappedText;
    }



    private ItemStack createModifierItem(IModifier modifier, String status) {
        Material material = modifierMaterials.getOrDefault(modifier.getId(), Material.ENDER_PEARL);
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + modifier.getName());
        List<String> lore = new ArrayList<>();
        lore.add(status);
        // Use ChatColor.GRAY for the description text
        lore.addAll(wrapText(ChatColor.GRAY + modifier.getDescription(), 40));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }


    private ItemStack createPlayerSkull(Player player) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName(ChatColor.YELLOW + player.getName());

        List<String> lore = new ArrayList<>();
        try {
            int currentRank = rankManager.getPlayerRank(player);
            int cycleCount = dataManager.getPlayerCycleCount(player);
            BigDecimal balance = economyManager.getBalance(player);
            BigDecimal storedBalance = dataManager.getStoredBalance(player);
            boolean inCycle = cycleManager.isPlayerInCycle(player);
            IModifier activeModifier = modifierManager.getActiveModifier(player);

            if(inCycle) {
                lore.add(ChatColor.GOLD + "Rank: " + configManager.getRankName(currentRank));
            }
            lore.add(ChatColor.AQUA + "Cycle Count: " + cycleCount);
            lore.add(ChatColor.GREEN + "Balance: " + balance);
            lore.add(ChatColor.GREEN + "Stored Balance: " + storedBalance);
            lore.add(ChatColor.LIGHT_PURPLE + "In Cycle: " + (inCycle ? "Yes" : "No"));
            if (activeModifier != null) {
                lore.add(ChatColor.LIGHT_PURPLE + "Active Modifier: " + activeModifier.getName());
            }
        } catch (SQLException e) {
            lore.add(ChatColor.RED + "Error retrieving player data");
        }

        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack createBackButton() {
        return createGuiItem(Material.RED_WOOL, ChatColor.RED + "Back", "Return to previous menu");
    }

    private ItemStack createNextPageButton() {
        return createGuiItem(Material.ARROW, ChatColor.GREEN + "Next Page", "Go to the next page");
    }

    private ItemStack createPreviousPageButton() {
        return createGuiItem(Material.ARROW, ChatColor.GREEN + "Previous Page", "Go to the previous page");
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