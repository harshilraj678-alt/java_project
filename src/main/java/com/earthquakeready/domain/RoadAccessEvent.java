package com.earthquakeready.domain;

public final class RoadAccessEvent extends SimulationEvent {
    private static final long serialVersionUID = 1L;
    public RoadAccessEvent(long tick, String zoneId, int severity, String description) {
        super(tick, zoneId, severity, description);
    }
    @Override public EventType eventType() { return EventType.ROAD_ACCESS; }
    @Override public void applyTo(Zone zone, int randomFactor) {
        zone.applyShaking(severity(), randomFactor);
        zone.disruptInfrastructure(ZoneStatus.ROAD_ACCESS_CONSTRAINED);
    }
}
