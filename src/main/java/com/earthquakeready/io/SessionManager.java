package com.earthquakeready.io;

import com.earthquakeready.domain.*;
import com.earthquakeready.engine.CityModel;
import com.earthquakeready.engine.EarthquakeEngine;
import com.earthquakeready.engine.ResourceAllocator;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class SessionManager {
    private static final int VERSION = 1;
    private final Path directory;

    public SessionManager(Path directory) { this.directory = directory; }

    public Path save(String name, EarthquakeEngine engine) throws IOException {
        if (name == null || !name.matches("[\\p{L}\\p{N} _-]{1,60}"))
            throw new IllegalArgumentException("Use 1–60 letters, numbers, spaces, underscores or hyphens");
        Path target = directory.resolve(name + ".ers");
        Path temporary = Files.createTempFile(directory, "session-", ".tmp");
        var inventory = engine.resources().inventorySnapshot();
        Session session = new Session(VERSION, Instant.now().toString(), engine.config(), engine.city(),
                inventory.total(), inventory.available(), engine.state().name());
        try {
            byte[] serialized;
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                 ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                output.writeObject(session);
                output.flush();
                serialized = bytes.toByteArray();
            }
            try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(temporary.toFile()))) {
                output.write(serialized);
            }
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } finally { Files.deleteIfExists(temporary); }
    }

    public Restored load(Path file) throws IOException {
        if (!file.toAbsolutePath().normalize().startsWith(directory.toAbsolutePath().normalize()))
            throw new IOException("Session must be located in the application saves folder");
        if (Files.size(file) > 64L * 1024 * 1024) throw new IOException("Session exceeds the 64 MB safety limit");
        byte[] serialized;
        try (FileInputStream fileInput = new FileInputStream(file.toFile());
             BufferedInputStream buffered = new BufferedInputStream(fileInput)) {
            serialized = buffered.readAllBytes();
        }
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(serialized);
             ObjectInputStream input = new ObjectInputStream(bytes)) {
            input.setObjectInputFilter(info -> {
                if (info.depth() > 30 || info.references() > 20_000 || info.arrayLength() > 100_000)
                    return ObjectInputFilter.Status.REJECTED;
                Class<?> type = info.serialClass();
                if (type == null) return ObjectInputFilter.Status.UNDECIDED;
                while (type.isArray()) type = type.getComponentType();
                String name = type.getName();
                if (type.isPrimitive() || name.startsWith("com.earthquakeready.")
                        || name.startsWith("java.lang.") || name.startsWith("java.util.")
                        || name.startsWith("java.time.")) return ObjectInputFilter.Status.ALLOWED;
                return ObjectInputFilter.Status.REJECTED;
            });
            Object value = input.readObject();
            if (!(value instanceof Session session)) throw new IOException("Unsupported save format");
            if (session.version() != VERSION) throw new IOException("Unsupported session version: " + session.version());
            return new Restored(session);
        } catch (ClassNotFoundException | InvalidClassException ex) {
            throw new IOException("Session is corrupt or uses unsupported classes", ex);
        }
    }

    public List<SaveInfo> list() throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".ers"))
                    .map(path -> {
                        try { return new SaveInfo(path, Files.size(path), Files.getLastModifiedTime(path).toInstant()); }
                        catch (IOException ex) { throw new UncheckedIOException(ex); }
                    }).sorted(Comparator.comparing(SaveInfo::modified).reversed()).toList();
        } catch (UncheckedIOException ex) { throw ex.getCause(); }
    }

    public record SaveInfo(Path path, long size, Instant modified) {}
    public static final class Restored {
        private final Session session;
        private Restored(Session session) { this.session = session; }
        public ScenarioConfig config() { return session.config(); }
        public CityModel city() { return session.city(); }
        public String previousState() { return session.state(); }
        public EarthquakeEngine createEngine() {
            return new EarthquakeEngine(session.config(), session.city(),
                    ResourceAllocator.fromInventory(session.totalResources(), session.resources()));
        }
    }
    private record Session(int version, String savedAt, ScenarioConfig config, CityModel city,
                           Map<ResourceType, Integer> totalResources,
                           Map<ResourceType, Integer> resources, String state) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
