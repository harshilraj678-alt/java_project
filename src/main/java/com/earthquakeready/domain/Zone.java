package com.earthquakeready.domain;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectOutputStream;

public final class Zone implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String name;
    private final int vulnerability;
    private final int populationExposure;
    private int preparedness;
    private int hazard;
    private ZoneStatus infrastructure = ZoneStatus.OPERATIONAL;

    public Zone(String id, String name, int vulnerability, int populationExposure, int preparedness) {
        if (id == null || id.isBlank() || name == null || name.isBlank()
                || vulnerability < 0 || vulnerability > 100 || populationExposure < 0
                || populationExposure > 100 || preparedness < 0 || preparedness > 100) {
            throw new IllegalArgumentException("Invalid fictional zone parameters");
        }
        this.id = id;
        this.name = name;
        this.vulnerability = vulnerability;
        this.populationExposure = populationExposure;
        this.preparedness = preparedness;
    }

    public synchronized void applyShaking(int intensity, int randomFactor) {
        hazard = Math.max(0, Math.min(100,
                (int) Math.round(intensity * 0.62 + vulnerability * 0.28
                        + populationExposure * 0.10 + randomFactor - preparedness * 0.22)));
        if (hazard >= 75) infrastructure = ZoneStatus.DISRUPTED;
        else if (hazard >= 48) infrastructure = ZoneStatus.STRAINED;
        else infrastructure = ZoneStatus.OPERATIONAL;
    }

    public synchronized void improvePreparedness(int points) {
        if (points <= 0) throw new IllegalArgumentException("Improvement must be positive");
        preparedness = Math.min(100, preparedness + points);
        hazard = Math.max(0, hazard - points / 2);
    }

    public synchronized void disruptInfrastructure(ZoneStatus condition) {
        infrastructure = java.util.Objects.requireNonNull(condition);
    }

    public synchronized void recover(int points) {
        if (points < 1) throw new IllegalArgumentException("Recovery points must be positive");
        hazard = Math.max(0, hazard - points);
        if (hazard < 48) infrastructure = ZoneStatus.OPERATIONAL;
        else if (hazard < 75) infrastructure = ZoneStatus.STRAINED;
        else infrastructure = ZoneStatus.DISRUPTED;
    }

    public String id() { return id; }
    public String name() { return name; }
    public int vulnerability() { return vulnerability; }
    public int populationExposure() { return populationExposure; }
    public synchronized int preparedness() { return preparedness; }
    public synchronized int hazard() { return hazard; }
    public synchronized String infrastructure() { return infrastructure.label(); }
    public synchronized SeverityLevel risk() { return SeverityLevel.fromScore(hazard); }
    private synchronized void writeObject(ObjectOutputStream output) throws IOException {
        output.defaultWriteObject();
    }
}
