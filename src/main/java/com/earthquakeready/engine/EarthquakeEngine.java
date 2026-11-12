package com.earthquakeready.engine;

import com.earthquakeready.domain.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.Random;

public final class EarthquakeEngine implements AutoCloseable {
    private final ScenarioConfig config;
    private final CityModel city;
    private final ResourceAllocator resources;
    private final List<Consumer<SimulationSnapshot>> listeners = new CopyOnWriteArrayList<>();
    private final List<String> activity = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong processed = new AtomicLong();
    private volatile SimulationState state = SimulationState.READY;
    private volatile int queueSize;
    private volatile boolean servicesClosed;
    private volatile double speedMultiplier = 1.0;
    private volatile int feedPriority = Thread.NORM_PRIORITY;
    private EventBuffer<SimulationEvent> eventBuffer;
    private EventBuffer<ResponseMission> responseBuffer;
    private ScenarioEventFeed feed;
    private Thread intakeThread;
    private Thread simulationThread;
    private Thread responseThread;

    public EarthquakeEngine(ScenarioConfig config) {
        this(config, new CityModel(config.preparedness()), new ResourceAllocator(config.initialResources()));
    }

    public EarthquakeEngine(ScenarioConfig config, CityModel city, ResourceAllocator resources) {
        this.config = Objects.requireNonNull(config);
        this.city = Objects.requireNonNull(city);
        this.resources = Objects.requireNonNull(resources);
    }

    public synchronized void start() throws IOException {
        if (state == SimulationState.RUNNING) return;
        if (state == SimulationState.PAUSED) { resume(); return; }
        if (state == SimulationState.STOPPED) throw new IllegalStateException("Reset the scenario before restarting");
        eventBuffer = new EventBuffer<>(12);
        responseBuffer = new EventBuffer<>(64);
        PipedWriter writer = new PipedWriter();
        PipedReader reader = new PipedReader(writer, 4096);
        feed = new ScenarioEventFeed(config, writer, city.tick() + 1);
        feed.setSpeed(speedMultiplier);
        feed.setPriority(feedPriority);
        intakeThread = new Thread(new EventIntake(new BufferedReader(reader), eventBuffer), "earthquakeready-event-intake");
        simulationThread = new Thread(new SimulationWorker(eventBuffer, city, processed, this::recordEvent,
                this::publish, this::eventStreamCompleted), "earthquakeready-simulation-worker");
        responseThread = new Thread(new ResponseWorker(responseBuffer, resources, this::recoverMission, this::publish), "earthquakeready-response-worker");
        try {
            for (ResponseMission mission : city.missions())
                if (mission.status() == MissionStatus.ACTIVE) responseBuffer.put(mission);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while restoring active response work", ex);
        }
        state = SimulationState.RUNNING;
        simulationThread.start();
        responseThread.start();
        intakeThread.start();
        feed.start();
        record("Simulation started: " + config.name());
        publish();
    }

    public synchronized void pause() {
        if (state != SimulationState.RUNNING) return;
        feed.pauseFeed();
        state = SimulationState.PAUSED;
        record("Simulation paused");
        publish();
    }

    public synchronized void resume() {
        if (state != SimulationState.PAUSED) return;
        feed.resumeFeed();
        state = SimulationState.RUNNING;
        record("Simulation resumed");
        publish();
    }

    public void stop() {
        ScenarioEventFeed feedToStop;
        EventBuffer<SimulationEvent> eventsToClose;
        EventBuffer<ResponseMission> responsesToClose;
        Thread intakeToJoin;
        Thread simulationToJoin;
        Thread responseToJoin;
        synchronized (this) {
            if (servicesClosed || state == SimulationState.READY) return;
            state = SimulationState.STOPPED;
            feedToStop = feed;
            eventsToClose = eventBuffer;
            responsesToClose = responseBuffer;
            intakeToJoin = intakeThread;
            simulationToJoin = simulationThread;
            responseToJoin = responseThread;
            if (eventsToClose != null) eventsToClose.close();
            if (responsesToClose != null) responsesToClose.close();
        }
        if (feedToStop != null) feedToStop.shutdownFeed();
        joinBounded(feedToStop);
        joinBounded(intakeToJoin);
        joinBounded(simulationToJoin);
        joinBounded(responseToJoin);
        servicesClosed = true;
        record("Simulation stopped");
        publish();
    }

    public boolean improvePreparedness(String zoneId, String task, int points) {
        if (task == null || task.isBlank()) throw new IllegalArgumentException("Task name is required");
        Zone zone = city.zones().stream().filter(z -> z.id().equals(zoneId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown zone"));
        if (points <= 0) throw new IllegalArgumentException("Improvement must be positive");
        if (!city.addTask(task)) return false;
        SimulationEvent event = new OperatorActionEvent(city.tick() + 1, zoneId, points,
                "Preparedness activity completed: " + task);
        city.process(event, 0);
        processed.incrementAndGet();
        recordEvent(event);
        publish();
        return true;
    }

    public synchronized void advanceOneStep() {
        if (state != SimulationState.READY) throw new IllegalStateException("Single-step is available before a feed starts");
        long tick = city.tick() + 1;
        if (tick > config.durationTicks()) throw new IllegalStateException("Scenario duration is complete");
        Random random = new Random(config.seed());
        for (long skipped = 1; skipped < tick; skipped++) {
            random.nextInt(config.affectedZones());
            random.nextInt(21);
        }
        String zoneId = city.zones().get(random.nextInt(config.affectedZones())).id();
        int severity = Math.max(1, Math.min(100, config.intensity() + random.nextInt(21) - 10));
        SimulationEvent event = createObservation(tick, zoneId, severity);
        city.process(event, 0);
        processed.incrementAndGet();
        recordEvent(event);
        publish();
    }

    public void setSpeed(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 0.25 || multiplier > 4)
            throw new IllegalArgumentException("Speed must be between 0.25x and 4x");
        speedMultiplier = multiplier;
        ScenarioEventFeed activeFeed = feed;
        if (activeFeed != null) activeFeed.setSpeed(multiplier);
    }

    public void runPriorityExperiment(int priority) {
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY)
            throw new IllegalArgumentException("Priority must be between 1 and 10");
        feedPriority = priority;
        ScenarioEventFeed activeFeed = feed;
        if (activeFeed != null && activeFeed.isAlive()) activeFeed.setPriority(priority);
    }

    public boolean launchMission(String type, String zoneId, Map<ResourceType, Integer> allocation) {
        if (responseBuffer == null || responseBuffer.isClosed())
            throw new IllegalStateException("Start the simulation before launching a mission");
        ResponseMission mission = new ResponseMission(type, zoneId, allocation);
        if (!resources.reserve(mission.allocation())) return false;
        city.addMission(mission);
        try {
            if (responseBuffer.isClosed()) throw new IllegalStateException("Simulation is shutting down");
            responseBuffer.put(mission);
        } catch (InterruptedException ex) {
            resources.release(mission.allocation());
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Mission dispatch interrupted", ex);
        } catch (RuntimeException ex) {
            resources.release(mission.allocation());
            throw ex;
        }
        record("Mission launched: " + type + " → " + zoneId);
        publish();
        return true;
    }

    public SimulationSnapshot snapshot() {
        List<SimulationSnapshot.ZoneView> zones = city.zones().stream().map(z ->
                new SimulationSnapshot.ZoneView(z.id(), z.name(), z.vulnerability(), z.populationExposure(),
                        z.preparedness(), z.hazard(), z.infrastructure())).toList();
        List<SimulationSnapshot.MissionView> missions = city.missions().stream().map(m ->
                new SimulationSnapshot.MissionView(m.id(), m.type(), m.zoneId(), m.progress(), m.status(), m.allocation())).toList();
        List<String> recent;
        synchronized (activity) { recent = activity.stream().skip(Math.max(0, activity.size() - 40L)).toList(); }
        return new SimulationSnapshot(state, city.tick(), city.preparednessScore(), zones, recent, missions,
                resources.inventory(), eventBuffer == null ? 0 : eventBuffer.size(), processed.get());
    }

    public void addListener(Consumer<SimulationSnapshot> listener) { listeners.add(listener); }
    public ScenarioConfig config() { return config; }
    public CityModel city() { return city; }
    public ResourceAllocator resources() { return resources; }
    public SimulationState state() { return state; }
    public long processedEvents() { return processed.get(); }
    public int queueSize() { return eventBuffer == null ? 0 : eventBuffer.size(); }
    public List<String> workerStates() {
        return List.of(threadState(feed), threadState(intakeThread), threadState(simulationThread), threadState(responseThread));
    }

    private static String threadState(Thread thread) { return thread == null ? "not started" : thread.getName() + ": " + thread.getState(); }
    private void publish() {
        SimulationSnapshot snapshot = snapshot();
        listeners.forEach(listener -> listener.accept(snapshot));
    }
    private void recordEvent(SimulationEvent event) { record(event.type() + " · " + event.description() + " (" + event.zoneId() + ")"); }
    private SimulationEvent createObservation(long tick, String zoneId, int severity) {
        if (tick == 1) return new EarthquakeEvent(tick, zoneId, severity, "Initial simulated shaking observation");
        if (tick <= config.aftershocks() + 1)
            return new AftershockEvent(tick, zoneId, severity, "Simulated aftershock observation");
        if (severity >= 82) return switch ((int) (tick % 4)) {
            case 0 -> new PowerOutageEvent(tick, zoneId, severity, "Simulated power interruption");
            case 1 -> new WaterSupplyDisruptionEvent(tick, zoneId, severity, "Simulated water-supply disruption");
            case 2 -> new RoadAccessEvent(tick, zoneId, severity, "Simulated road-access constraint");
            default -> new InfrastructureDisruptionEvent(tick, zoneId, severity, "Simulated infrastructure disruption");
        };
        return new ObservationEvent(tick, zoneId, severity, "Simulated ground-shaking observation");
    }
    private void recoverMission(ResponseMission mission) {
        RecoveryEvent event = new RecoveryEvent(city.tick() + 1, mission.zoneId(), 8,
                "Illustrative recovery action completed: " + mission.type());
        city.process(event, 0);
        processed.incrementAndGet();
        recordEvent(event);
    }
    private void record(String message) { activity.add(message); }
    private synchronized void eventStreamCompleted() {
        if (state == SimulationState.RUNNING || state == SimulationState.PAUSED) {
            state = SimulationState.STOPPED;
            record("Simulation event feed completed");
            publish();
        }
    }
    private static void joinBounded(Thread thread) {
        if (thread == null || thread == Thread.currentThread()) return;
        try { thread.join(2000); if (thread.isAlive()) thread.interrupt(); }
        catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
    }
    @Override public void close() { stop(); }
}
