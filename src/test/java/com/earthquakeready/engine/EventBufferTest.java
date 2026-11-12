package com.earthquakeready.engine;

import com.earthquakeready.domain.EarthquakeEvent;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class EventBufferTest {
    @Test void blocksProducerWhenFullAndWakesItWhenItemIsTaken() throws Exception {
        EventBuffer<Integer> buffer = new EventBuffer<>(1);
        buffer.put(1);
        CountDownLatch entered = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> producer = executor.submit(() -> {
                entered.countDown();
                try { buffer.put(2); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            });
            assertTrue(entered.await(1, TimeUnit.SECONDS));
            assertFalse(producer.isDone());
            assertEquals(1, buffer.take());
            producer.get(1, TimeUnit.SECONDS);
            assertEquals(2, buffer.take());
        } finally {
            buffer.close();
            executor.shutdownNow();
        }
    }

    @Test void closeDrainsAcceptedEventsThenWakesWaitingConsumer() throws Exception {
        EventBuffer<String> buffer = new EventBuffer<>(2);
        buffer.put("accepted");
        buffer.close();
        assertEquals("accepted", buffer.take());
        assertNull(buffer.take());
    }

    @Test void simulationWorkerWakesOnNewEventAndDrainsOnClose() throws Exception {
        EventBuffer<com.earthquakeready.domain.SimulationEvent> buffer = new EventBuffer<>(2);
        CountDownLatch processed = new CountDownLatch(1);
        CityModel city = new CityModel(40);
        Thread worker = new Thread(new SimulationWorker(buffer, city, new java.util.concurrent.atomic.AtomicLong(),
                event -> processed.countDown(), () -> {}, () -> {}));
        worker.start();
        buffer.put(new EarthquakeEvent(1, "RES", 60, "test observation"));
        assertTrue(processed.await(1, TimeUnit.SECONDS));
        buffer.close();
        worker.join(1000);
        assertFalse(worker.isAlive());
        assertEquals(1, city.events().size());
    }
}
