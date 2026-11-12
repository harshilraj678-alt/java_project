package com.earthquakeready.engine;

import com.earthquakeready.domain.ScenarioConfig;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PipedWriter;
import java.util.List;
import java.util.Random;

public final class ScenarioEventFeed extends Thread {
    private static final List<String> ZONES = List.of("RES", "COM", "SCH", "HOS", "IND", "SHE");
    private final ScenarioConfig config;
    private final BufferedWriter output;
    private final Object lifecycle = new Object();
    private final long firstTick;
    private volatile long delayMillis = 800;
    private volatile boolean shutdown;
    private boolean paused;

    public ScenarioEventFeed(ScenarioConfig config, PipedWriter writer, long firstTick) {
        super("earthquakeready-event-feed");
        this.config = config;
        this.output = new BufferedWriter(writer);
        this.firstTick = firstTick;
        setDaemon(true);
    }

    public void pauseFeed() { synchronized (lifecycle) { paused = true; } }
    public void resumeFeed() { synchronized (lifecycle) { paused = false; lifecycle.notifyAll(); } }
    public void shutdownFeed() { synchronized (lifecycle) { shutdown = true; lifecycle.notifyAll(); } interrupt(); }
    public void setSpeed(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 0.25 || multiplier > 4)
            throw new IllegalArgumentException("Speed must be between 0.25x and 4x");
        delayMillis = Math.max(50, Math.round(800 / multiplier));
    }

    @Override public void run() {
        Random random = new Random(config.seed());
        try {
            for (int skipped = 1; skipped < firstTick; skipped++) { random.nextInt(config.affectedZones()); random.nextInt(21); }
            for (long tick = firstTick; tick <= config.durationTicks() && !shutdown; tick++) {
                synchronized (lifecycle) {
                    while (paused && !shutdown) lifecycle.wait();
                }
                if (shutdown) break;
                String zone = ZONES.get(random.nextInt(config.affectedZones()));
                int severity = Math.max(1, Math.min(100, config.intensity() + random.nextInt(21) - 10));
                String type;
                if (tick == 1) type = "EARTHQUAKE";
                else if (tick <= config.aftershocks() + 1) type = "AFTERSHOCK";
                else if (severity >= 82) type = switch ((int) (tick % 4)) {
                    case 0 -> "POWER_OUTAGE";
                    case 1 -> "WATER_SUPPLY_DISRUPTION";
                    case 2 -> "ROAD_ACCESS";
                    default -> "INFRASTRUCTURE_DISRUPTION";
                };
                else type = "OBSERVATION";
                output.write(tick + "|" + zone + "|" + severity + "|" + type);
                output.newLine();
                output.flush();
                Thread.sleep(delayMillis);
            }
        } catch (InterruptedException ex) {
            if (!shutdown) Thread.currentThread().interrupt();
        } catch (IOException ex) {
            if (!shutdown) throw new IllegalStateException("Could not write simulated observation feed", ex);
        } finally {
            try { output.close(); }
            catch (IOException ex) { if (!shutdown) throw new IllegalStateException("Could not close event feed", ex); }
        }
    }
}
