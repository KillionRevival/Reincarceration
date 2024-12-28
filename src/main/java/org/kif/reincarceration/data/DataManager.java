package org.kif.reincarceration.data;

import org.bukkit.entity.Player;
import org.kif.reincarceration.entity.CycleHistory;
import org.kif.reincarceration.util.ConsoleUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {
    private final DataModule dataModule;

    public DataManager(DataModule dataModule) {
        this.dataModule = dataModule;
    }

    public void createPlayerData(Player player) throws SQLException {
        String sql = "INSERT OR IGNORE INTO player_data(uuid, name, current_rank, cycle_count, in_cycle, stored_balance) VALUES(?,?,?,?,?,?)";
        try (Connection conn = dataModule.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                pstmt.setString(1, player.getUniqueId().toString());
                pstmt.setString(2, player.getName());
                pstmt.setInt(3, 0);
                pstmt.setInt(4, 0);
                pstmt.setBoolean(5, false);
                pstmt.setDouble(6, 0.0);
                pstmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public int getPlayerRank(Player player) throws SQLException {
        String sql = "SELECT current_rank FROM player_data WHERE uuid = ?";
        try (Connection conn = dataModule.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("current_rank");
                }
            }
        }
        return 0;
    }

    public void updatePlayerRank(Player player, int newRank) throws SQLException {
        String sql = "UPDATE player_data SET current_rank = ? WHERE uuid = ?";
        try (Connection conn = dataModule.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                pstmt.setInt(1, newRank);
                pstmt.setString(2, player.getUniqueId().toString());
                pstmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean isPlayerInCycle(UUID playerUuid) throws SQLException {
        String sql = "SELECT in_cycle FROM player_data WHERE uuid = ?";
        try (Connection conn = dataModule.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("in_cycle");
                }
            }
        }
        return false;
    }

    public boolean isPlayerInCycle(Player player) throws SQLException {
        return isPlayerInCycle(player.getUniqueId());
    }

    public boolean isPlayerInCycleUUID(UUID playerUUID) throws SQLException {
        String sql = "SELECT in_cycle FROM player_data WHERE uuid = ?";
        try (Connection conn = dataModule.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("in_cycle");
                }
            }
        }
        return false;
    }

    public void setPlayerCycleStatus(Player player, boolean inCycle) throws SQLException {
        String sql = "UPDATE player_data SET in_cycle = ? WHERE uuid = ?";
        try (Connection conn = dataModule.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                pstmt.setBoolean(1, inCycle);
                pstmt.setString(2, player.getUniqueId().toString());
                pstmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logSevere("Error setting player cycle status: " + e.getMessage());
            throw e;
        }
    }

    public int getPlayerCycleCount(Player player) throws SQLException {
        String sql = "SELECT cycle_count FROM player_data WHERE uuid = ?";
        try (Connection conn = dataModule.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cycle_count");
                }
            }
        } catch (SQLException e) {
            logSevere("Error getting player cycle count: " + e.getMessage());
            throw e;
        }
        return 0;
    }

    public void incrementPlayerCycleCount(Player player) throws SQLException {
        String sql = "UPDATE player_data SET cycle_count = cycle_count + 1 WHERE uuid = ?";
        try (Connection conn = dataModule.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                pstmt.setString(1, player.getUniqueId().toString());
                pstmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logSevere("Error incrementing player cycle count: " + e.getMessage());
            throw e;
        }
    }

    public BigDecimal getStoredBalance(UUID uuid) throws SQLException {
        String sql = "SELECT stored_balance FROM player_data WHERE uuid = ?";
        try (Connection conn = dataModule.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("stored_balance").setScale(2, RoundingMode.FLOOR);
                }
            }
        } catch (SQLException e) {
            logSevere("Error getting stored balance: " + e.getMessage());
            throw e;
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getStoredBalance(Player player) throws SQLException {
        return getStoredBalance(player.getUniqueId());
    }

    public void setStoredBalance(Player player, BigDecimal balance) throws SQLException {
        setStoredBalance(player.getUniqueId(), balance);
    }

    public void setStoredBalance(UUID uuid, BigDecimal balance) throws SQLException {
        String sql = "UPDATE player_data SET stored_balance = ? WHERE uuid = ?";
        try (Connection conn = dataModule.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            try {
                pstmt.setBigDecimal(1, balance);
                pstmt.setString(2, uuid.toString());
                pstmt.executeUpdate();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logSevere("Error setting stored balance: " + e.getMessage());
            throw e;
        }
    }

    public void addCompletedModifier(Player player, String modifierId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO completed_modifiers (player_uuid, modifier_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, modifierId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logSevere("Error adding completed modifier: " + e.getMessage());
            throw e;
        }
    }

    public List<String> getCompletedModifiers(Player player) throws SQLException {
        List<String> completedModifiers = new ArrayList<>();
        String sql = "SELECT modifier_id FROM completed_modifiers WHERE player_uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    completedModifiers.add(rs.getString("modifier_id"));
                }
            }
        } catch (SQLException e) {
            logSevere("Error getting completed modifiers: " + e.getMessage());
            throw e;
        }
        return completedModifiers;
    }

    public boolean hasCompletedModifier(Player player, String modifierId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM completed_modifiers WHERE player_uuid = ? AND modifier_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, modifierId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logSevere("Error checking if player has completed modifier: " + e.getMessage());
            throw e;
        }
        return false;
    }

    public void setActiveModifier(Player player, String modifierId) throws SQLException {
        String sql = "INSERT OR REPLACE INTO active_modifiers (player_uuid, modifier_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, modifierId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logSevere("Error setting active modifier: " + e.getMessage());
            throw e;
        }
    }

    public String getActiveModifier(Player player) throws SQLException {
        String sql = "SELECT modifier_id FROM active_modifiers WHERE player_uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("modifier_id");
                }
            }
        } catch (SQLException e) {
            logSevere("Error getting active modifier: " + e.getMessage());
            throw e;
        }
        return null;
    }

    public void removeActiveModifier(Player player) throws SQLException {
        String sql = "DELETE FROM active_modifiers WHERE player_uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logSevere("Error removing active modifier: " + e.getMessage());
            throw e;
        }
    }

    public int getCompletedModifierCount(Player player) throws SQLException {
        String sql = "SELECT COUNT(*) FROM completed_modifiers WHERE player_uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public void recordCycleStart(Player player, String modifier_id) throws SQLException {
        String sql = "INSERT INTO cycle_history (player_uuid, modifier_id, start_time, completed) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, modifier_id);
            pstmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            pstmt.setBoolean(4, false);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logSevere("Error starting new cycle: " + e.getMessage());
            throw e;
        }
    }

    public void recordCycleCompletion(Player player, Boolean completed) throws SQLException {
        String sql = "UPDATE cycle_history SET end_time = ?, completed = ? WHERE player_uuid = ? AND end_time IS NULL";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pstmt.setBoolean(2, completed);
            pstmt.setString(3, player.getUniqueId().toString());
            int updatedRows = pstmt.executeUpdate();
            if (updatedRows == 0) {
                ConsoleUtil.sendDebug("No active cycle found to complete for player: " + player.getName());
            } else {
                ConsoleUtil.sendDebug("Completed cycle for player: " + player.getName());
            }
        } catch (SQLException e) {
            logSevere("Error completing cycle: " + e.getMessage());
            throw e;
        }
    }

    public CycleHistory getLastCycleHistoryForId(
            final UUID playerId,
            final String modifierId
    ) throws SQLException {
        String sql =
                "SELECT id, player_uuid, modifier_id, start_time, end_time, completed " +
                "FROM cycle_history                                            " +
                "WHERE ? = player_uuid                                         " +
                "ORDER BY end_time DESC                                        " +
                "LIMIT 1"                                                        ;
        try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            final ResultSet resultSet = statement.executeQuery();
            return CycleHistory.builder()
                               .id(resultSet.getInt("id"))
                               .playerId(UUID.fromString(resultSet.getString("player_uuid")))
                               .modifierId(resultSet.getString("modifier_id"))
                               .startTime(resultSet.getTimestamp("start_time").toInstant())
                               .endTime(resultSet.getTimestamp("end_time").toInstant())
                               .completed(resultSet.getBoolean("completed"))
                               .build();
        } catch (SQLException e) {
            logSevere("Error getting cycle history: " + e.getMessage());
            throw e;
        }
    }

    public List<CycleHistory> getCycleHistoryForPlayer(
            final UUID playerId
    ) throws SQLException {
        final ArrayList<CycleHistory> histories = new ArrayList<>();
        String sql = "SELECT id, player_uuid, modifier_id, start_time, end_time, completed FROM cycle_history WHERE ? = player_uuid";
        try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            final ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                histories.add(
                        CycleHistory.builder()
                                    .id(resultSet.getInt("id"))
                                    .playerId(UUID.fromString(resultSet.getString("player_uuid")))
                                    .modifierId(resultSet.getString("modifier_id"))
                                    .startTime(resultSet.getTimestamp("start_time").toInstant())
                                    .endTime(resultSet.getTimestamp("end_time").toInstant())
                                    .completed(resultSet.getBoolean("completed"))
                                    .build()
                );
            }
            return histories;
        } catch (SQLException e) {
            logSevere("Error getting cycle history: " + e.getMessage());
            throw e;
        }
    }

    public void clearPlayerData(Player player) throws SQLException {
        clearPlayerDataTable(player);
        clearCycleHistoryTable(player);
        clearCompletedModifiersTable(player);
        clearActiveModifiersTable(player);

        ConsoleUtil.sendDebug("Finished clearing all data for player: " + player.getName());
    }

    private void clearPlayerDataTable(Player player) throws SQLException {
        String sql = "DELETE FROM player_data WHERE uuid = ?";
        executeDelete(sql, player, "player_data");
    }

    private void clearCycleHistoryTable(Player player) throws SQLException {
        String sql = "DELETE FROM cycle_history WHERE player_uuid = ?";
        executeDelete(sql, player, "cycle_history");
    }

    private void clearCompletedModifiersTable(Player player) throws SQLException {
        String sql = "DELETE FROM completed_modifiers WHERE player_uuid = ?";
        executeDelete(sql, player, "completed_modifiers");
    }

    private void clearActiveModifiersTable(Player player) throws SQLException {
        String sql = "DELETE FROM active_modifiers WHERE player_uuid = ?";
        executeDelete(sql, player, "active_modifiers");
    }

    private void executeDelete(String sql, Player player, String tableName) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            int rowsAffected = pstmt.executeUpdate();
            ConsoleUtil.sendDebug("Cleared " + rowsAffected + " rows from " + tableName + " for player: " + player.getName());
        } catch (SQLException e) {
            ConsoleUtil.sendError("Error clearing data from " + tableName + " for player " + player.getName() + ": " + e.getMessage());
            throw e;
        }
    }

    // Update this method to use dataModule directly
    public Connection getConnection () throws SQLException {
        return dataModule.getConnection();
    }

    // If you need to log anything, use this method
    private void log (String message){
        dataModule.getPlugin().getLogger().info(message);
    }

    // If you need to log severe errors, use this method
    private void logSevere (String message){
        dataModule.getPlugin().getLogger().severe(message);
    }
}