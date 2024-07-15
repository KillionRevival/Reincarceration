package org.kif.reincarceration.permission;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;
import org.kif.reincarceration.Reincarceration;
import org.kif.reincarceration.config.ConfigManager;
import org.kif.reincarceration.data.DataManager;
import org.kif.reincarceration.data.DataModule;
import org.kif.reincarceration.util.ConsoleUtil;
import org.kif.reincarceration.util.RomanNumeralUtil;

import java.sql.SQLException;

public class PermissionManager {
    private final Reincarceration plugin;
    private final ConfigManager configManager;
    private final LuckPerms luckPerms;
    private final DataManager dataManager;

    public PermissionManager(Reincarceration plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getModuleManager().getConfigManager();
        this.luckPerms = plugin.getServer().getServicesManager().load(LuckPerms.class);

        DataModule dataModule = plugin.getModuleManager().getModule(DataModule.class);
        this.dataManager = dataModule.getDataManager();
    }

    public void updatePlayerRankGroup(Player player, int rank) {
        if (luckPerms == null) {
            plugin.getLogger().severe("LuckPerms not found! Unable to update player rank group.");
            return;
        }

        String groupName = configManager.getRankPermissionGroup(rank);
        String entryGroup = configManager.getEntryGroup();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            plugin.getLogger().severe("Unable to get LuckPerms user for " + player.getName());
            return;
        }

        // Remove from entry group
        user.data().remove(InheritanceNode.builder(entryGroup).build());

        // Remove old rank groups
        user.data().clear(node -> node.getKey().startsWith("group.reoffender_"));

        // Add new rank group
        InheritanceNode groupNode = InheritanceNode.builder(groupName).build();
        user.data().add(groupNode);

        // Set primary group
        user.setPrimaryGroup(groupName);

        // default group
        user.data().clear(node -> node.getKey().equals("group.default"));

        // Save changes
        luckPerms.getUserManager().saveUser(user);
    }

    public void resetToDefaultGroup(Player player) {
        if (luckPerms == null) {
            plugin.getLogger().severe("LuckPerms not found! Unable to reset player group.");
            return;
        }

        String entryGroup = configManager.getEntryGroup();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            plugin.getLogger().severe("Unable to get LuckPerms user for " + player.getName());
            return;
        }

        // Remove all reoffender rank groups
        user.data().clear(node -> node.getKey().startsWith("group.reoffender_"));

        // Add back to entry group
        InheritanceNode groupNode = InheritanceNode.builder(entryGroup).build();
        user.data().add(groupNode);

        // Set primary group back to entry
        user.setPrimaryGroup(entryGroup);

        // default group
        user.data().clear(node -> node.getKey().equals("group.default"));

        // Remove default group
        // user.data().remove(InheritanceNode.builder("default").build());

        addCompletionPrefix(player);

        // Save changes
        luckPerms.getUserManager().saveUser(user);
    }

    public void addPermission(Player player, String permission) {
        if (luckPerms == null) {
            plugin.getLogger().severe("LuckPerms not found! Unable to add permission.");
            return;
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            plugin.getLogger().severe("Unable to get LuckPerms user for " + player.getName());
            return;
        }

        user.data().add(Node.builder(permission).build());
        luckPerms.getUserManager().saveUser(user);
    }

    public void removePermission(Player player, String permission) {
        if (luckPerms == null) {
            plugin.getLogger().severe("LuckPerms not found! Unable to remove permission.");
            return;
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            plugin.getLogger().severe("Unable to get LuckPerms user for " + player.getName());
            return;
        }

        user.data().remove(Node.builder(permission).build());
        luckPerms.getUserManager().saveUser(user);
    }

    public boolean hasPermission(Player player, String permission) {
        if (luckPerms == null) {
            plugin.getLogger().severe("LuckPerms not found! Unable to check permission.");
            return false;
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            plugin.getLogger().severe("Unable to get LuckPerms user for " + player.getName());
            return false;
        }

        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }

    public void addCompletionPrefix(Player player) {
        if (luckPerms == null) {
            plugin.getLogger().severe("LuckPerms not found! Unable to reset player group.");
            return;
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            plugin.getLogger().severe("Unable to get LuckPerms user for " + player.getName());
            return;
        }

        int completedModifiersCount;
        try {
            ConsoleUtil.sendDebug("Getting completed modifier count for " + player.getName());
            completedModifiersCount = dataManager.getCompletedModifierCount(player);
        } catch (SQLException e) {
            ConsoleUtil.sendError("Error getting completed modifier count for " + player.getName() + ": " + e.getMessage());
            throw new RuntimeException(e);
        }

        if (completedModifiersCount == 0) {
            ConsoleUtil.sendDebug("No completed modifiers for " + player.getName());
            return;
        }

        // Remove all previous prefixes
        for (int i = 1; i <= completedModifiersCount; i++) {
            String romanNumeral = RomanNumeralUtil.toRoman(i);
            removePermission(player, "prefix.0.&8[&4" + romanNumeral + "&8]&r");
            removePermission(player, "prefix.0.&8[&6" + romanNumeral + "&8]&r");
            ConsoleUtil.sendDebug("Removed prefix for " + player.getName() + ": " + romanNumeral);
        }

        ConsoleUtil.sendDebug("Adding completion prefix for " + player.getName());
        String currentRomanNumeral = RomanNumeralUtil.toRoman(completedModifiersCount);
        addPermission(player, "prefix.0.&8[&6" + currentRomanNumeral + "&8]&r");

        ConsoleUtil.sendDebug("Saving completion prefix for " + player.getName());
        // Save changes
        luckPerms.getUserManager().saveUser(user);
    }

    public boolean isAssociatedWithBaseGroup(Player player) {
        if (luckPerms == null) {
            plugin.getLogger().severe("LuckPerms not found! Unable to check base group association.");
            return false;
        }

        String baseGroup = configManager.getBaseGroup();
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            plugin.getLogger().warning("Unable to get LuckPerms user for " + player.getName());
            return false;
        }

        // Check all inherited groups
        for (InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
            if (isGroupOrParentBaseGroup(node.getGroupName(), baseGroup)) {
                return true;
            }
        }

        return false;
    }

    private boolean isGroupOrParentBaseGroup(String groupName, String baseGroup) {
        if (groupName.equals(baseGroup)) {
            return true;
        }

        Group group = luckPerms.getGroupManager().getGroup(groupName);
        if (group == null) {
            return false;
        }

        // Check parent groups
        for (InheritanceNode node : group.getNodes(NodeType.INHERITANCE)) {
            if (isGroupOrParentBaseGroup(node.getGroupName(), baseGroup)) {
                return true;
            }
        }

        return false;
    }
}