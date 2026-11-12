package com.earthquakeready.engine;

import com.earthquakeready.domain.*;
import java.io.BufferedReader;
import java.io.IOException;

public final class EventIntake implements Runnable {
    private final BufferedReader input;
    private final EventBuffer<SimulationEvent> buffer;

    public EventIntake(BufferedReader input, EventBuffer<SimulationEvent> buffer) {
        this.input = input;
        this.buffer = buffer;
    }

    @Override public void run() {
        try (input) {
            String line;
            while ((line = input.readLine()) != null) {
                String[] fields = line.split("\\|", -1);
                if (fields.length != 4) throw new IOException("Malformed event-feed record");
                long tick = Long.parseLong(fields[0]);
                int severity = Integer.parseInt(fields[2]);
                SimulationEvent event = switch (fields[3]) {
                    case "EARTHQUAKE" -> new EarthquakeEvent(tick, fields[1], severity, "Initial simulated shaking observation");
                    case "AFTERSHOCK" -> new AftershockEvent(tick, fields[1], severity, "Simulated aftershock observation");
                    case "OBSERVATION" -> new ObservationEvent(tick, fields[1], severity, "Simulated ground-shaking observation");
                    case "INFRASTRUCTURE_DISRUPTION" -> new InfrastructureDisruptionEvent(tick, fields[1], severity, "Simulated infrastructure disruption");
                    case "POWER_OUTAGE" -> new PowerOutageEvent(tick, fields[1], severity, "Simulated power interruption");
                    case "WATER_SUPPLY_DISRUPTION" -> new WaterSupplyDisruptionEvent(tick, fields[1], severity, "Simulated water-supply disruption");
                    case "ROAD_ACCESS" -> new RoadAccessEvent(tick, fields[1], severity, "Simulated road-access constraint");
                    default -> throw new IOException("Unsupported event type: " + fields[3]);
                };
                buffer.put(event);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IllegalStateException ex) {
            if (!buffer.isClosed()) throw ex;
        } catch (IOException | NumberFormatException ex) {
            throw new IllegalStateException("Event intake failed", ex);
        } finally {
            buffer.close();
        }
    }
}
