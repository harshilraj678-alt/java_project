package com.earthquakeready.engine;

import com.earthquakeready.domain.SimulationEvent;
import java.util.concurrent.atomic.AtomicLong;

public final class SimulationWorker implements Runnable {
    private final EventBuffer<SimulationEvent> events;
    private final CityModel city;
    private final AtomicLong processed;
    private final SimulationEventHandler eventListener;
    private final Runnable snapshotPublisher;
    private final Runnable completion;

    public SimulationWorker(EventBuffer<SimulationEvent> events, CityModel city, AtomicLong processed,
                            SimulationEventHandler eventListener, Runnable snapshotPublisher, Runnable completion) {
        this.events = events;
        this.city = city;
        this.processed = processed;
        this.eventListener = eventListener;
        this.snapshotPublisher = snapshotPublisher;
        this.completion = completion;
    }

    @Override public void run() {
        try {
            SimulationEvent event;
            while ((event = events.take()) != null) {
                city.process(event, 0);
                processed.incrementAndGet();
                eventListener.handle(event);
                snapshotPublisher.run();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            completion.run();
        }
    }
}
