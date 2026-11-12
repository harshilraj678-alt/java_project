package com.earthquakeready.domain;

public final class AftershockEvent extends SimulationEvent {
    private static final long serialVersionUID = 1L;
    public AftershockEvent(long tick, String zoneId, int severity, String description) {
        super(tick, zoneId, severity, description);
    }
    @Override public EventType eventType() { return EventType.AFTERSHOCK; }
    @Override public void applyTo(Zone zone, int randomFactor) { zone.applyShaking(severity(), randomFactor); }
}
