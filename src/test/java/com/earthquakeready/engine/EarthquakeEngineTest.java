package com.earthquakeready.engine;

import com.earthquakeready.domain.ScenarioConfig;
import com.earthquakeready.domain.Zone;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class EarthquakeEngineTest {
    @Test void oneStepIsReproducibleForFixedSeed() {
        ScenarioConfig config = new ScenarioConfig("Test exercise", 60, 10, 6, 2, 20, 50, 9981);
        EarthquakeEngine left = new EarthquakeEngine(config);
        EarthquakeEngine right = new EarthquakeEngine(config);
        left.advanceOneStep();
        right.advanceOneStep();
        assertEquals(left.city().events().get(0).zoneId(), right.city().events().get(0).zoneId());
        assertEquals(left.city().events().get(0).severity(), right.city().events().get(0).severity());
        assertEquals(left.city().zones().stream().map(Zone::hazard).toList(),
                right.city().zones().stream().map(Zone::hazard).toList());
    }

    @Test void engineStartsAndStopsWorkersGracefully() throws Exception {
        ScenarioConfig config = new ScenarioConfig("Short exercise", 35, 20, 6, 2, 10, 50, 25);
        EarthquakeEngine engine = new EarthquakeEngine(config);
        engine.start();
        engine.pause();
        engine.resume();
        engine.stop();
        assertEquals(com.earthquakeready.domain.SimulationState.STOPPED, engine.state());
        assertTrue(engine.workerStates().stream().allMatch(line ->
                line.equals("not started") || line.endsWith("TERMINATED")));
    }

    @Test void pipedFeedProducesEventsOnBackgroundWorkers() throws Exception {
        ScenarioConfig config = new ScenarioConfig("Feed test", 60, 10, 6, 2, 10, 50, 25);
        EarthquakeEngine engine = new EarthquakeEngine(config);
        CountDownLatch processed = new CountDownLatch(1);
        engine.addListener(snapshot -> { if (snapshot.processedEvents() > 0) processed.countDown(); });
        engine.start();
        engine.setSpeed(4);
        try {
            assertTrue(processed.await(3, TimeUnit.SECONDS));
            assertFalse(engine.city().events().isEmpty());
        } finally { engine.stop(); }
    }

    @Test void liveSimulationProcessesConfiguredTicksAndCompletes() throws Exception {
        ScenarioConfig config = new ScenarioConfig("Completion test", 60, 2, 6, 1, 10, 50, 25);
        EarthquakeEngine engine = new EarthquakeEngine(config);
        CountDownLatch completed = new CountDownLatch(1);
        engine.addListener(snapshot -> {
            if (snapshot.state() == com.earthquakeready.domain.SimulationState.STOPPED) completed.countDown();
        });
        engine.start();
        engine.setSpeed(4);
        try {
            assertTrue(completed.await(5, TimeUnit.SECONDS));
            assertEquals(2, engine.snapshot().tick());
            assertEquals(2, engine.processedEvents());
            assertEquals(com.earthquakeready.domain.SimulationState.STOPPED, engine.state());
            engine.stop();
            assertTrue(engine.workerStates().stream().allMatch(line ->
                    line.equals("not started") || line.endsWith("TERMINATED")));
        } finally { engine.stop(); }
    }
}
