package com.earthquakeready.domain;

import com.earthquakeready.engine.CityModel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventProcessingTest {
    @Test void specializedEventsApplyInfrastructureAndRecoveryEffects() {
        CityModel city = new CityModel(30);
        city.process(new PowerOutageEvent(1, "RES", 100, "test outage"), 0);
        Zone zone = city.zones().get(0);
        assertEquals(ZoneStatus.POWER_INTERRUPTION.label(), zone.infrastructure());
        int beforeRecovery = zone.hazard();
        city.process(new RecoveryEvent(2, "RES", 8, "test recovery"), 0);
        assertEquals(beforeRecovery - 8, zone.hazard());
        assertEquals("RECOVERY", city.events().get(1).type());
    }

    @Test void operatorActionRaisesReadinessOnceThroughEventProcessing() {
        CityModel city = new CityModel(40);
        int initial = city.zones().get(0).preparedness();
        city.process(new OperatorActionEvent(1, "RES", 5, "test action"), 0);
        assertEquals(initial + 5, city.zones().get(0).preparedness());
    }
}
