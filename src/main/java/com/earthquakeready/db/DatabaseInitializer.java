package com.earthquakeready.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {
    public void initialize(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS scenarios (id INTEGER PRIMARY KEY, name TEXT NOT NULL, scenario_type TEXT NOT NULL, intensity INTEGER NOT NULL, random_seed INTEGER NOT NULL, created_at TEXT NOT NULL, completed_at TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS simulation_events (id TEXT PRIMARY KEY, scenario_id INTEGER, event_type TEXT NOT NULL, zone_id TEXT NOT NULL, severity INTEGER NOT NULL, simulation_tick INTEGER NOT NULL, description TEXT NOT NULL, created_at TEXT NOT NULL, FOREIGN KEY(scenario_id) REFERENCES scenarios(id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS zone_history (id INTEGER PRIMARY KEY, scenario_id INTEGER, zone_id TEXT NOT NULL, hazard_score INTEGER NOT NULL, preparedness_score INTEGER NOT NULL, infrastructure_status TEXT NOT NULL, simulation_tick INTEGER NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS preparedness_tasks (id INTEGER PRIMARY KEY, task_name TEXT NOT NULL UNIQUE, category TEXT NOT NULL, completion_status INTEGER NOT NULL, updated_at TEXT NOT NULL)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS response_missions (id TEXT PRIMARY KEY, scenario_id INTEGER, mission_type TEXT NOT NULL, target_zone TEXT NOT NULL, status TEXT NOT NULL, progress INTEGER NOT NULL, created_at TEXT NOT NULL, completed_at TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS resource_inventory (resource_type TEXT PRIMARY KEY, total_quantity INTEGER NOT NULL, available_quantity INTEGER NOT NULL CHECK(available_quantity >= 0))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mission_resources (id INTEGER PRIMARY KEY, mission_id TEXT NOT NULL, resource_type TEXT NOT NULL, allocated_quantity INTEGER NOT NULL CHECK(allocated_quantity > 0), FOREIGN KEY(mission_id) REFERENCES response_missions(id), FOREIGN KEY(resource_type) REFERENCES resource_inventory(resource_type))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS operational_logs (id INTEGER PRIMARY KEY, scenario_id INTEGER, event_type TEXT NOT NULL, description TEXT NOT NULL, timestamp TEXT NOT NULL)");
        }
    }
}
