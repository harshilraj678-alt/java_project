package com.earthquakeready.engine;

import com.earthquakeready.domain.*;
import java.util.List;
import java.util.Map;

public record SimulationSnapshot(SimulationState state, long tick, int preparedness,
                                 List<ZoneView> zones, List<String> recentEvents,
                                 List<MissionView> missions, Map<ResourceType, Integer> resources,
                                 int queueSize, long processedEvents) {
    public SimulationSnapshot {
        zones = List.copyOf(zones);
        recentEvents = List.copyOf(recentEvents);
        missions = List.copyOf(missions);
        resources = Map.copyOf(resources);
    }
    public record ZoneView(String id, String name, int vulnerability, int exposure,
                           int preparedness, int hazard, String infrastructure) {}
    public record MissionView(String id, String type, String zone, int progress, MissionStatus status,
                              Map<ResourceType, Integer> allocation) {
        public MissionView { allocation = Map.copyOf(allocation); }
    }
}
