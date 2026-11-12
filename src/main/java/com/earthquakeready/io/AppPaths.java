package com.earthquakeready.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppPaths {
    private final Path root;
    public AppPaths() throws IOException {
        String base = System.getenv("APPDATA");
        root = (base == null || base.isBlank()
                ? Path.of(System.getProperty("user.home"), ".earthquakeready")
                : Path.of(base, "EarthquakeReady")).toAbsolutePath();
        for (String folder : new String[]{"data", "data/logs", "data/archive", "data/saves",
                "data/reports", "data/database"}) Files.createDirectories(root.resolve(folder));
    }
    public Path root() { return root; }
    public Path logs() { return root.resolve("data/logs"); }
    public Path saves() { return root.resolve("data/saves"); }
    public Path reports() { return root.resolve("data/reports"); }
    public Path database() { return root.resolve("data/database/earthquakeready.db"); }
    public Path archive() { return root.resolve("data/archive/sensor-records.bin"); }
}
