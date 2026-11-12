package com.earthquakeready.domain;

public enum ResourceType {
    FIRST_AID("First-aid units"), SUPPLY_KITS("Emergency supply kits"),
    INSPECTION_TEAMS("Inspection teams"), UTILITY_TEAMS("Utility teams"),
    SHELTER_BEDS("Shelter beds"), COMMUNITY_TEAMS("Community assistance teams");

    private final String label;
    ResourceType(String label) { this.label = label; }
    public String label() { return label; }
}
