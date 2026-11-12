package com.earthquakeready.domain;

public enum ZoneStatus {
    OPERATIONAL("Operational"),
    STRAINED("Strained"),
    DISRUPTED("Disrupted"),
    POWER_INTERRUPTION("Power interruption"),
    WATER_SUPPLY_DISRUPTION("Water supply disruption"),
    ROAD_ACCESS_CONSTRAINED("Road access constrained");

    private final String label;
    ZoneStatus(String label) { this.label = label; }
    public String label() { return label; }
}
