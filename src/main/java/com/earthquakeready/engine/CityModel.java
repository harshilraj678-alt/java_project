package com.earthquakeready.engine;

import com.earthquakeready.domain.*;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;

public final class CityModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<Zone> zones = new ArrayList<>();
    private final List<SimulationEvent> events = new ArrayList<>();
    private final List<ResponseMission> missions = new ArrayList<>();
    private final Set<String> completedTasks = new HashSet<>();
    private long tick;

    public CityModel(int preparedness) {
        zones.add(new Zone("RES", "Residential District", 66, 83, preparedness));
        zones.add(new Zone("COM", "Commercial District", 58, 76, preparedness));
        zones.add(new Zone("SCH", "School Zone", 44, 71, preparedness));
        zones.add(new Zone("HOS", "Hospital Zone", 37, 64, preparedness));
        zones.add(new Zone("IND", "Industrial Area", 73, 52, preparedness));
        zones.add(new Zone("SHE", "Community Shelter Zone", 30, 39, preparedness));
    }
    public synchronized void process(SimulationEvent event, int randomFactor) {
        Zone zone = zones.stream().filter(z -> z.id().equals(event.zoneId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown zone: " + event.zoneId()));
        event.applyTo(zone, randomFactor);
        events.add(event);
        tick = Math.max(tick, event.tick());
    }
    public synchronized List<Zone> zones() { return List.copyOf(zones); }
    public synchronized List<SimulationEvent> events() { return List.copyOf(events); }
    public synchronized List<ResponseMission> missions() { return List.copyOf(missions); }
    public synchronized long tick() { return tick; }
    public synchronized void addMission(ResponseMission mission) { missions.add(mission); }
    public synchronized boolean addTask(String task) { return completedTasks.add(task); }
    public synchronized Set<String> completedTasks() { return Set.copyOf(completedTasks); }
    public synchronized int preparednessScore() {
        return (int) Math.round(zones.stream().mapToInt(Zone::preparedness).average().orElse(0));
    }
    private synchronized void writeObject(ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
    }
}
