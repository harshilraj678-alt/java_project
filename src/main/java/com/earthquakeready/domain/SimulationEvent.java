package com.earthquakeready.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public abstract class SimulationEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id = UUID.randomUUID().toString();
    private final Instant timestamp = Instant.now();
    private final long tick;
    private final String zoneId;
    private final int severity;
    private final String description;

    protected SimulationEvent(long tick, String zoneId, int severity, String description) {
        if (tick < 0 || zoneId == null || zoneId.isBlank() || severity < 0 || severity > 100
                || description == null || description.isBlank()) throw new IllegalArgumentException("Invalid event");
        this.tick = tick;
        this.zoneId = zoneId;
        this.severity = severity;
        this.description = description;
    }
    public abstract EventType eventType();
    public String type() { return eventType().name(); }
    public abstract void applyTo(Zone zone, int randomFactor);
    public String id() { return id; }
    public Instant timestamp() { return timestamp; }
    public long tick() { return tick; }
    public String zoneId() { return zoneId; }
    public int severity() { return severity; }
    public String description() { return description; }
}
