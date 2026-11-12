package com.earthquakeready.db;

import com.earthquakeready.domain.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {
    @TempDir Path temp;

    @Test void schemaAndParameterizedEventTaskOperationsWork() throws Exception {
        DatabaseManager manager = new DatabaseManager(temp.resolve("test.db"));
        manager.initialize();
        manager.saveEvent("event-1", "EARTHQUAKE", "RES", 55, 1, "safe sample", "2026-01-01T00:00:00Z");
        assertEquals(1, manager.eventCount());
        manager.saveTask("Emergency kit", "HOUSEHOLD", true, "2026-01-01T00:00:00Z");
        try (Connection connection = manager.connection();
             PreparedStatement select = connection.prepareStatement("SELECT completion_status FROM preparedness_tasks WHERE task_name=?")) {
            select.setString(1, "Emergency kit");
            try (ResultSet result = select.executeQuery()) { assertTrue(result.next()); assertEquals(1, result.getInt(1)); }
        }
        manager.deleteTask("Emergency kit");
        try (Connection connection = manager.connection();
             PreparedStatement select = connection.prepareStatement("SELECT COUNT(*) FROM preparedness_tasks");
             ResultSet result = select.executeQuery()) { assertTrue(result.next()); assertEquals(0, result.getInt(1)); }
    }

    @Test void missionReservationIsTransactionalAndRollsBackOnShortage() throws Exception {
        DatabaseManager manager = new DatabaseManager(temp.resolve("mission.db"));
        manager.initialize();
        EnumMap<ResourceType, Integer> total = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) total.put(type, 3);
        manager.syncInventory(total, total);
        assertThrows(SQLException.class, () -> manager.createMissionTransactional("m1", "test", "RES",
                Map.of(ResourceType.FIRST_AID, 1, ResourceType.SUPPLY_KITS, 4)));
        try (Connection connection = manager.connection();
             PreparedStatement select = connection.prepareStatement("SELECT available_quantity FROM resource_inventory WHERE resource_type=?")) {
            select.setString(1, ResourceType.FIRST_AID.name());
            try (ResultSet result = select.executeQuery()) { assertTrue(result.next()); assertEquals(3, result.getInt(1)); }
        }
        manager.createMissionTransactional("m2", "test", "RES", Map.of(ResourceType.FIRST_AID, 1));
        manager.updateMissionTransactional("m2", "COMPLETED", 100, Map.of(ResourceType.FIRST_AID, 1));
    }
}
