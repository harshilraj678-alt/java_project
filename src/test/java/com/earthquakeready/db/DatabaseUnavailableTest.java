package com.earthquakeready.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseUnavailableTest {
    @TempDir Path temp;

    @Test void databaseFailureIsReportedWithoutDisablingFileSessionPersistence() throws Exception {
        Path occupied = Files.createDirectory(temp.resolve("sqlite-is-a-directory"));
        DatabaseService service = new DatabaseService(new DatabaseManager(occupied));
        try {
            assertThrows(Exception.class, () -> service.eventCount().get(2, TimeUnit.SECONDS));
            assertFalse(service.available());
            Path saves = Files.createDirectory(temp.resolve("saves"));
            var file = new com.earthquakeready.io.SessionManager(saves)
                    .save("offline", new com.earthquakeready.engine.EarthquakeEngine(
                            com.earthquakeready.domain.ScenarioConfig.moderate()));
            assertTrue(Files.exists(file));
        } finally { service.close(); }
    }
}
