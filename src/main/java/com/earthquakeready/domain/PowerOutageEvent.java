package com.earthquakeready.domain;

public final class PowerOutageEvent extends SimulationEvent {
    private static final long serialVersionUID = 1L;
    public PowerOutageEvent(long tick, String zoneId, int severity, String description) {
        super(tick, zoneId, severity, description);
    }
    @Override public EventType eventType() { return EventType.POWER_OUTAGE; }
    @Override public void applyTo(Zone zone, int randomFactor) {
        zone.applyShaking(severity(), randomFactor);
        zone.disruptInfrastructure(ZoneStatus.POWER_INTERRUPTION);
    }
}
