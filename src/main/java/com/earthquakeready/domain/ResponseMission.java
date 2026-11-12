package com.earthquakeready.domain;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.UUID;

public final class ResponseMission implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id = UUID.randomUUID().toString();
    private final String type;
    private final String zoneId;
    private final Map<ResourceType, Integer> allocation;
    private int progress;
    private MissionStatus status = MissionStatus.ACTIVE;

    public ResponseMission(String type, String zoneId, Map<ResourceType, Integer> allocation) {
        if (type == null || type.isBlank() || zoneId == null || zoneId.isBlank() || allocation == null || allocation.isEmpty())
            throw new IllegalArgumentException("Mission requires a type, zone and resources");
        allocation.forEach((key, value) -> {
            if (key == null || value == null || value < 1) throw new IllegalArgumentException("Invalid mission allocation");
        });
        this.type = type;
        this.zoneId = zoneId;
        this.allocation = Map.copyOf(allocation);
    }
    public synchronized boolean advance() {
        if (status != MissionStatus.ACTIVE) return false;
        progress = Math.min(100, progress + 20);
        if (progress == 100) status = MissionStatus.COMPLETED;
        return status == MissionStatus.COMPLETED;
    }
    public String id() { return id; }
    public String type() { return type; }
    public String zoneId() { return zoneId; }
    public Map<ResourceType, Integer> allocation() { return allocation; }
    public synchronized int progress() { return progress; }
    public synchronized MissionStatus status() { return status; }
    private synchronized void writeObject(ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
    }
}
