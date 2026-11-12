package com.earthquakeready.db;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import com.earthquakeready.domain.ResourceType;

public final class DatabaseService implements AutoCloseable {
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1024), task -> {
                Thread thread = new Thread(task, "earthquakeready-database");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private final AtomicLong operations = new AtomicLong();
    private volatile String lastError = "";
    private volatile boolean available;
    private final DatabaseManager manager;

    public DatabaseService(DatabaseManager manager) {
        this.manager = manager;
        executor.execute(() -> {
            try { manager.initialize(); available = true; operations.incrementAndGet(); }
            catch (SQLException ex) { fail(ex); }
        });
    }
    public CompletableFuture<Void> recordEvent(String id, String type, String zone, int severity,
                                                long tick, String description, String timestamp) {
        return submit(() -> manager.saveEvent(id, type, zone, severity, tick, description, timestamp));
    }
    public CompletableFuture<Void> saveScenario(String name, String type, int intensity, long seed) {
        return submit(() -> manager.saveScenario(name, type, intensity, seed, Instant.now().toString()));
    }
    public CompletableFuture<Void> saveZoneHistory(String zoneId, int hazard, int preparedness,
                                                    String infrastructure, long tick) {
        return submit(() -> manager.saveZoneHistory(zoneId, hazard, preparedness, infrastructure, tick));
    }
    public CompletableFuture<Void> saveOperationalLog(String type, String description, String timestamp) {
        return submit(() -> manager.saveOperationalLog(type, description, timestamp));
    }
    public CompletableFuture<Void> saveTask(String name, String category, boolean complete) {
        return submit(() -> manager.saveTask(name, category, complete, Instant.now().toString()));
    }
    public CompletableFuture<Void> deleteTask(String name) { return submit(() -> manager.deleteTask(name)); }
    public CompletableFuture<Void> syncInventory(Map<ResourceType, Integer> total, Map<ResourceType, Integer> available) {
        return submit(() -> manager.syncInventory(total, available));
    }
    public CompletableFuture<Void> createMission(String id, String type, String zone,
                                                  Map<ResourceType, Integer> allocation) {
        return submit(() -> manager.createMissionTransactional(id, type, zone, allocation));
    }
    public CompletableFuture<Void> updateMission(String id, String status, int progress,
                                                  Map<ResourceType, Integer> allocation) {
        return submit(() -> manager.updateMissionTransactional(id, status, progress, allocation));
    }
    public CompletableFuture<Integer> eventCount() {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try { int result = manager.eventCount(); operations.incrementAndGet(); available = true; return result; }
                catch (SQLException ex) { fail(ex); throw new CompletionException(ex); }
            }, executor);
        } catch (RejectedExecutionException ex) { fail(ex); return CompletableFuture.failedFuture(ex); }
    }
    public CompletableFuture<java.util.List<String>> recentEvents(int limit) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    var result = manager.recentEvents(limit);
                    operations.incrementAndGet();
                    available = true;
                    return result;
                } catch (SQLException ex) { fail(ex); throw new CompletionException(ex); }
            }, executor);
        } catch (RejectedExecutionException ex) { fail(ex); return CompletableFuture.failedFuture(ex); }
    }
    private CompletableFuture<Void> submit(SqlOperation operation) {
        try {
            return CompletableFuture.runAsync(() -> {
                try { operation.run(); operations.incrementAndGet(); available = true; }
                catch (SQLException ex) { fail(ex); throw new CompletionException(ex); }
            }, executor);
        } catch (RejectedExecutionException ex) { fail(ex); return CompletableFuture.failedFuture(ex); }
    }
    private void fail(Throwable ex) {
        lastError = ex.getMessage() == null ? ex.toString() : ex.getMessage();
        available = false;
    }
    public boolean available() { return available; }
    public String lastError() { return lastError; }
    public long operations() { return operations.get(); }
    public String workerStatus() { return "active=" + executor.getActiveCount() + ", queued=" + executor.getQueue().size(); }
    public DatabaseManager manager() { return manager; }
    @Override public void close() {
        executor.shutdown();
        try { if (!executor.awaitTermination(3, TimeUnit.SECONDS)) executor.shutdownNow(); }
        catch (InterruptedException ex) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
    }
    @FunctionalInterface private interface SqlOperation { void run() throws SQLException; }
}
