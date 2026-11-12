package com.earthquakeready.domain;

public final class WaterSupplyDisruptionEvent extends SimulationEvent {
    private static final long serialVersionUID = 1L;
    public WaterSupplyDisruptionEvent(long tick, String zoneId, int severity, String description) {
        super(tick, zoneId, severity, description);
    }
    @Override public EventType eventType() { return EventType.WATER_SUPPLY_DISRUPTION; }
    @Override public void applyTo(Zone zone, int randomFactor) {
        zone.applyShaking(severity(), randomFactor);
        zone.disruptInfrastructure(ZoneStatus.WATER_SUPPLY_DISRUPTION);
    }
}
