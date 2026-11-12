package com.earthquakeready.io;

import com.earthquakeready.domain.SimulationEvent;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

public final class ArchiveService implements AutoCloseable {
    private final Path file;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "earthquakeready-archive-writer");
        thread.setDaemon(true);
        return thread;
    });

    public ArchiveService(Path file) { this.file = file; }
    public CompletableFuture<Void> append(SimulationEvent event) {
        return CompletableFuture.runAsync(() -> {
            try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(file.toFile(), true)))) {
                output.writeLong(event.tick());
                output.writeUTF(event.id());
                output.writeUTF(event.type());
                output.writeUTF(event.zoneId());
                output.writeInt(event.severity());
                output.writeUTF(event.description());
                output.writeUTF(event.timestamp().toString());
            } catch (IOException ex) { throw new CompletionException(ex); }
        }, writer);
    }

    public long recordCount() throws IOException {
        long count = 0;
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(file.toFile())))) {
            while (true) {
                input.readLong();
                input.readUTF();
                input.readUTF();
                input.readUTF();
                input.readInt();
                input.readUTF();
                input.readUTF();
                count++;
            }
        } catch (EOFException end) { return count; }
    }
    public Path file() { return file; }
    @Override public void close() {
        writer.shutdown();
        try { if (!writer.awaitTermination(3, TimeUnit.SECONDS)) writer.shutdownNow(); }
        catch (InterruptedException ex) { writer.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
