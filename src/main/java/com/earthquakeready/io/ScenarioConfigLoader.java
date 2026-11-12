package com.earthquakeready.io;

import com.earthquakeready.domain.ScenarioConfig;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

public final class ScenarioConfigLoader {
    public ScenarioConfig read(Path file) throws IOException {
        Properties properties = new Properties();
        try (FileReader fileReader = new FileReader(file.toFile(), StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(fileReader)) {
            properties.load(reader);
        }
        try {
            return new ScenarioConfig(properties.getProperty("name", "Imported custom scenario"),
                    integer(properties, "intensity", 52),
                    integer(properties, "durationTicks", 30),
                    integer(properties, "affectedZones", 6),
                    integer(properties, "aftershocks", 3),
                    integer(properties, "initialResources", 24),
                    integer(properties, "preparedness", 55),
                    Long.parseLong(properties.getProperty("seed", "20260415").trim()));
        } catch (IllegalArgumentException ex) {
            throw new IOException("Invalid scenario configuration: " + ex.getMessage(), ex);
        }
    }

    public void write(Path file, ScenarioConfig config) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("name", config.name());
        properties.setProperty("intensity", Integer.toString(config.intensity()));
        properties.setProperty("durationTicks", Integer.toString(config.durationTicks()));
        properties.setProperty("affectedZones", Integer.toString(config.affectedZones()));
        properties.setProperty("aftershocks", Integer.toString(config.aftershocks()));
        properties.setProperty("initialResources", Integer.toString(config.initialResources()));
        properties.setProperty("preparedness", Integer.toString(config.preparedness()));
        properties.setProperty("seed", Long.toString(config.seed()));
        try (FileWriter fileWriter = new FileWriter(file.toFile(), StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(fileWriter)) {
            properties.store(writer, "EarthquakeReady fictional educational scenario");
        }
    }

    private static int integer(Properties properties, String key, int fallback) {
        return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
    }
}
