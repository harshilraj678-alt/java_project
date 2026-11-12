package com.earthquakeready.domain;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioConfigTest {
    @Test void acceptsSupportedConfigurationAndRejectsInvalidBounds() {
        assertEquals(52, ScenarioConfig.moderate().intensity());
        assertEquals(6, new ScenarioConfig("Quick exercise", 35, 42L).affectedZones());
        assertThrows(IllegalArgumentException.class, () ->
                new ScenarioConfig("bad", 101, 10, 6, 0, 10, 50, 1));
        assertThrows(IllegalArgumentException.class, () ->
                new ScenarioConfig("bad", 50, 10, 0, 0, 10, 50, 1));
    }

    @Test void hazardBucketsCoverBoundaries() {
        assertEquals(SeverityLevel.LOW, SeverityLevel.fromScore(24));
        assertEquals(SeverityLevel.MODERATE, SeverityLevel.fromScore(25));
        assertEquals(SeverityLevel.HIGH, SeverityLevel.fromScore(50));
        assertEquals(SeverityLevel.CRITICAL, SeverityLevel.fromScore(75));
    }

    @Test void missionProgressTransitionsOnceToCompleted() {
        ResponseMission mission = new ResponseMission("Assess", "RES", Map.of(ResourceType.INSPECTION_TEAMS, 1));
        for (int step = 0; step < 4; step++) {
            assertFalse(mission.advance());
            assertEquals(MissionStatus.ACTIVE, mission.status());
        }
        assertTrue(mission.advance());
        assertEquals(MissionStatus.COMPLETED, mission.status());
        assertFalse(mission.advance());
        assertEquals(100, mission.progress());
    }
}
