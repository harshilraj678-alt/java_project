package com.earthquakeready.domain;

public final class RecoveryEvent extends SimulationEvent {
    private static final long serialVersionUID = 1L;
    public RecoveryEvent(long tick, String zoneId, int severity, String description) {
        super(tick, zoneId, severity, description);
        if (severity < 1) throw new IllegalArgumentException("Recovery improvement must be positive");
    }
    @Override public EventType eventType() { return EventType.RECOVERY; }
    @Override public void applyTo(Zone zone, int randomFactor) { zone.recover(severity()); }
}
