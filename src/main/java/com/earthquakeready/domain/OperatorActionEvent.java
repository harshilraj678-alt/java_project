package com.earthquakeready.domain;

public final class OperatorActionEvent extends SimulationEvent {
    private static final long serialVersionUID = 1L;
    public OperatorActionEvent(long tick, String zoneId, int severity, String description) {
        super(tick, zoneId, severity, description);
        if (severity < 1) throw new IllegalArgumentException("Preparedness improvement must be positive");
    }
    @Override public EventType eventType() { return EventType.OPERATOR_ACTION; }
    @Override public void applyTo(Zone zone, int randomFactor) { zone.improvePreparedness(severity()); }
}
