package com.earthquakeready.io;

import com.earthquakeready.domain.ScenarioConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ScenarioConfigLoaderTest {
    @TempDir Path temp;

    @Test void writesAndReadsSupportedTextScenarioConfiguration() throws Exception {
        ScenarioConfig expected = new ScenarioConfig("Imported demo", 47, 18, 4, 2, 14, 63, 7721L);
        Path file = temp.resolve("scenario.properties");
        ScenarioConfigLoader loader = new ScenarioConfigLoader();
        loader.write(file, expected);
        assertEquals(expected, loader.read(file));
    }

    @Test void invalidTextValuesFailWithAnExplicitConfigurationError() throws Exception {
        Path file = temp.resolve("invalid.properties");
        Files.writeString(file, "name=broken\nintensity=not-a-number\n");
        assertThrows(IOException.class, () -> new ScenarioConfigLoader().read(file));
    }
}
