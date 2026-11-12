package com.earthquakeready.domain;

public final class InfrastructureDisruptionEvent extends SimulationEvent {
    private static final long serialVersionUID = 1L;
    public InfrastructureDisruptionEvent(long tick, String zoneId, int severity, String description) {
        super(tick, zoneId, severity, description);
    }
    @Override public EventType eventType() { return EventType.INFRASTRUCTURE_DISRUPTION; }
    @Override public void applyTo(Zone zone, int randomFactor) {
        zone.applyShaking(severity(), randomFactor);
        zone.disruptInfrastructure(ZoneStatus.DISRUPTED);
    }
}
