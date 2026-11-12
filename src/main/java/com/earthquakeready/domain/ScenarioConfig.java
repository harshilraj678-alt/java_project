package com.earthquakeready.domain;

import java.io.Serializable;

public record ScenarioConfig(String name, int intensity, int durationTicks, int affectedZones,
                             int aftershocks, int initialResources, int preparedness, long seed)
        implements Serializable {
    private static final long serialVersionUID = 1L;

    public ScenarioConfig {
        if (name == null || name.isBlank() || intensity < 1 || intensity > 100
                || durationTicks < 1 || durationTicks > 120 || affectedZones < 1 || affectedZones > 6
                || aftershocks < 0 || aftershocks > 20 || initialResources < 1 || initialResources > 100
                || preparedness < 0 || preparedness > 100) {
            throw new IllegalArgumentException("Scenario values are outside the supported educational range");
        }
    }

    public ScenarioConfig(String name, int intensity, long seed) {
        this(name, intensity, 30, 6, 3, 24, 55, seed);
    }

    public static ScenarioConfig moderate() {
        return new ScenarioConfig("Moderate shaking exercise", 52, 30, 6, 3, 24, 55, 20260415L);
    }
}
