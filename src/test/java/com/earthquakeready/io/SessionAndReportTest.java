package com.earthquakeready.io;

import com.earthquakeready.domain.ScenarioConfig;
import com.earthquakeready.engine.EarthquakeEngine;
import com.earthquakeready.domain.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SessionAndReportTest {
    @TempDir Path temp;

    @Test void sessionRoundTripRestoresValidDomainState() throws Exception {
        EarthquakeEngine engine = new EarthquakeEngine(ScenarioConfig.moderate());
        engine.advanceOneStep();
        assertTrue(engine.resources().reserve(Map.of(ResourceType.FIRST_AID, 1)));
        SessionManager manager = new SessionManager(temp);
        Path saved = manager.save("demo-session", engine);
        var restored = manager.load(saved);
        assertEquals(engine.config(), restored.config());
        assertEquals(1, restored.city().events().size());
        assertEquals(engine.city().tick(), restored.city().tick());
        var restoredEngine = restored.createEngine();
        assertEquals(engine.resources().available(ResourceType.FIRST_AID),
                restoredEngine.resources().available(ResourceType.FIRST_AID));
        restoredEngine.resources().release(Map.of(ResourceType.FIRST_AID, 1));
        assertEquals(engine.config().initialResources(), restoredEngine.resources().available(ResourceType.FIRST_AID));
        assertEquals(saved, manager.list().get(0).path());
    }

    @Test void corruptSessionFailsClearly() throws Exception {
        Path corrupt = temp.resolve("broken.ers");
        Files.writeString(corrupt, "not a session");
        assertThrows(IOException.class, () -> new SessionManager(temp).load(corrupt));
    }

    @Test void unsupportedSerializedClassIsRejected() throws Exception {
        Path unexpected = temp.resolve("unexpected.ers");
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(unexpected))) {
            output.writeObject(new java.util.Date());
        }
        assertThrows(IOException.class, () -> new SessionManager(temp).load(unexpected));
    }

    @Test void reportExportsTxtAndCsv() throws Exception {
        EarthquakeEngine engine = new EarthquakeEngine(ScenarioConfig.moderate());
        engine.advanceOneStep();
        Path txt = ReportExporter.exportTxt(temp, engine);
        Path csv = ReportExporter.exportCsv(temp, engine);
        assertTrue(Files.readString(txt).contains("Fictional data only"));
        assertTrue(Files.readString(csv).contains("zone_id,zone_name"));
    }
}
