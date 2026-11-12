package com.earthquakeready.domain;

public final class ObservationEvent extends SimulationEvent {
    private static final long serialVersionUID = 1L;
    public ObservationEvent(long tick, String zoneId, int severity, String description) {
        super(tick, zoneId, severity, description);
    }
    @Override public EventType eventType() { return EventType.OBSERVATION; }
    @Override public void applyTo(Zone zone, int randomFactor) { zone.applyShaking(severity(), randomFactor); }
}
