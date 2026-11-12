package com.earthquakeready.db;

import java.nio.file.Path;
import java.sql.*;
import java.util.Map;
import com.earthquakeready.domain.ResourceType;

public final class DatabaseManager {
    private final String url;
    public DatabaseManager(Path path) { url = "jdbc:sqlite:" + path.toAbsolutePath(); }
    public Connection connection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            return connection;
        } catch (SQLException ex) {
            try { connection.close(); } catch (SQLException closeError) { ex.addSuppressed(closeError); }
            throw ex;
        }
    }

    public void initialize() throws SQLException {
        try (Connection connection = connection()) { new DatabaseInitializer().initialize(connection); }
    }

    public void saveEvent(String id, String type, String zone, int severity, long tick, String description, String timestamp) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO simulation_events(id,event_type,zone_id,severity,simulation_tick,description,created_at) VALUES(?,?,?,?,?,?,?)")) {
            statement.setString(1, id); statement.setString(2, type); statement.setString(3, zone);
            statement.setInt(4, severity); statement.setLong(5, tick); statement.setString(6, description);
            statement.setString(7, timestamp); statement.executeUpdate();
        }
    }
    public int eventCount() throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM simulation_events");
             ResultSet result = statement.executeQuery()) { return result.next() ? result.getInt(1) : 0; }
    }
    public java.util.List<String> recentEvents(int limit) throws SQLException {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("History limit must be from 1 to 1000");
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT event_type,zone_id,simulation_tick,description,created_at FROM simulation_events ORDER BY created_at DESC LIMIT ?")) {
            statement.setInt(1, limit);
            try (ResultSet result = statement.executeQuery()) {
                java.util.List<String> rows = new java.util.ArrayList<>();
                while (result.next()) rows.add("T+" + result.getLong("simulation_tick") + " · "
                        + result.getString("event_type") + " · " + result.getString("zone_id") + " · "
                        + result.getString("description") + " · " + result.getString("created_at"));
                return java.util.List.copyOf(rows);
            }
        }
    }
    public void saveScenario(String name, String type, int intensity, long seed, String createdAt) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO scenarios(name,scenario_type,intensity,random_seed,created_at) VALUES(?,?,?,?,?)")) {
            statement.setString(1, name); statement.setString(2, type); statement.setInt(3, intensity);
            statement.setLong(4, seed); statement.setString(5, createdAt); statement.executeUpdate();
        }
    }
    public void saveZoneHistory(String zoneId, int hazard, int preparedness, String infrastructure, long tick) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO zone_history(zone_id,hazard_score,preparedness_score,infrastructure_status,simulation_tick) VALUES(?,?,?,?,?)")) {
            statement.setString(1, zoneId); statement.setInt(2, hazard); statement.setInt(3, preparedness);
            statement.setString(4, infrastructure); statement.setLong(5, tick); statement.executeUpdate();
        }
    }
    public void saveOperationalLog(String type, String description, String timestamp) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO operational_logs(event_type,description,timestamp) VALUES(?,?,?)")) {
            statement.setString(1, type); statement.setString(2, description); statement.setString(3, timestamp);
            statement.executeUpdate();
        }
    }
    public void saveTask(String name, String category, boolean complete, String updated) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO preparedness_tasks(task_name,category,completion_status,updated_at) VALUES(?,?,?,?) ON CONFLICT(task_name) DO UPDATE SET category=excluded.category,completion_status=excluded.completion_status,updated_at=excluded.updated_at")) {
            statement.setString(1, name); statement.setString(2, category); statement.setInt(3, complete ? 1 : 0);
            statement.setString(4, updated); statement.executeUpdate();
        }
    }
    public void deleteTask(String name) throws SQLException {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement("DELETE FROM preparedness_tasks WHERE task_name=?")) {
            statement.setString(1, name); statement.executeUpdate();
        }
    }

    public void syncInventory(Map<ResourceType, Integer> total, Map<ResourceType, Integer> available) throws SQLException {
            try (Connection connection = connection()) {
                connection.setAutoCommit(false);
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO resource_inventory(resource_type,total_quantity,available_quantity) VALUES(?,?,?) ON CONFLICT(resource_type) DO UPDATE SET total_quantity=excluded.total_quantity,available_quantity=excluded.available_quantity")) {
                    for (ResourceType type : ResourceType.values()) {
                        statement.setString(1, type.name());
                        statement.setInt(2, total.get(type));
                        statement.setInt(3, available.get(type));
                        statement.addBatch();
                    }
                    statement.executeBatch();
                    connection.commit();
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                }
            }
        }

        public void createMissionTransactional(String id, String type, String zone,
                                               Map<ResourceType, Integer> allocation) throws SQLException {
            try (Connection connection = connection()) {
                connection.setAutoCommit(false);
                try (PreparedStatement inventory = connection.prepareStatement(
                        "UPDATE resource_inventory SET available_quantity=available_quantity-? WHERE resource_type=? AND available_quantity>=?");
                     PreparedStatement mission = connection.prepareStatement(
                        "INSERT INTO response_missions(id,mission_type,target_zone,status,progress,created_at) VALUES(?,?,?,'ACTIVE',0,?)");
                     PreparedStatement parts = connection.prepareStatement(
                        "INSERT INTO mission_resources(mission_id,resource_type,allocated_quantity) VALUES(?,?,?)")) {
                    try (PreparedStatement existing = connection.prepareStatement("SELECT status,progress FROM response_missions WHERE id=?")) {
                        existing.setString(1, id);
                        try (ResultSet result = existing.executeQuery()) {
                            if (result.next()) {
                                try (PreparedStatement update = connection.prepareStatement(
                                        "UPDATE response_missions SET status=?,progress=? WHERE id=?")) {
                                    update.setString(1, result.getString(1)); update.setInt(2, result.getInt(2)); update.setString(3, id);
                                    update.executeUpdate();
                                }
                                connection.commit();
                                return;
                            }
                        }
                    }
                    for (var entry : allocation.entrySet()) {
                        inventory.setInt(1, entry.getValue());
                        inventory.setString(2, entry.getKey().name());
                        inventory.setInt(3, entry.getValue());
                        if (inventory.executeUpdate() != 1) throw new SQLException("Database inventory cannot reserve " + entry.getKey());
                    }
                    mission.setString(1, id); mission.setString(2, type); mission.setString(3, zone);
                    mission.setString(4, java.time.Instant.now().toString());
                    mission.executeUpdate();
                    for (var entry : allocation.entrySet()) {
                        parts.setString(1, id); parts.setString(2, entry.getKey().name()); parts.setInt(3, entry.getValue());
                        parts.addBatch();
                    }
                    parts.executeBatch();
                    connection.commit();
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                }
            }
        }

        public void updateMissionTransactional(String id, String status, int progress,
                                               Map<ResourceType, Integer> allocation) throws SQLException {
            try (Connection connection = connection()) {
                connection.setAutoCommit(false);
                try {
                    boolean release = false;
                    try (PreparedStatement find = connection.prepareStatement("SELECT status FROM response_missions WHERE id=?")) {
                        find.setString(1, id);
                        try (ResultSet result = find.executeQuery()) {
                            if (!result.next()) throw new SQLException("Unknown mission: " + id);
                            release = !"COMPLETED".equals(result.getString(1)) && "COMPLETED".equals(status);
                        }
                    }
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE response_missions SET status=?,progress=?,completed_at=? WHERE id=?")) {
                        update.setString(1, status); update.setInt(2, progress);
                        update.setString(3, "COMPLETED".equals(status) ? java.time.Instant.now().toString() : null);
                        update.setString(4, id); update.executeUpdate();
                    }
                    if (release) try (PreparedStatement inventory = connection.prepareStatement(
                            "UPDATE resource_inventory SET available_quantity=MIN(total_quantity,available_quantity+?) WHERE resource_type=?")) {
                        for (var entry : allocation.entrySet()) {
                            inventory.setInt(1, entry.getValue()); inventory.setString(2, entry.getKey().name()); inventory.addBatch();
                        }
                        inventory.executeBatch();
                    }
                    connection.commit();
                } catch (SQLException ex) {
                    connection.rollback();
                    throw ex;
                }
            }
        }
    public String jdbcUrl() { return url; }
}
