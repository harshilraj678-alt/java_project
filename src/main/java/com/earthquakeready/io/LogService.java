package com.earthquakeready.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.concurrent.*;

public final class LogService implements AutoCloseable {
    private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "earthquakeready-log-writer");
        thread.setDaemon(true);
        return thread;
    });
    private final Path file;
    public LogService(Path file) { this.file = file; }
    public CompletableFuture<Void> log(String message) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedWriter output = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                output.write(Instant.now() + "  " + message);
                output.newLine();
            } catch (IOException ex) { throw new CompletionException(ex); }
        }, writer);
    }
    @Override public void close() {
        writer.shutdown();
        try { if (!writer.awaitTermination(3, TimeUnit.SECONDS)) writer.shutdownNow(); }
        catch (InterruptedException ex) { writer.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
