package com.earthquakeready.engine;

import com.earthquakeready.domain.ResourceType;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ResourceAllocatorTest {
    @Test void concurrentReservationsNeverOverAllocateAndConserveInventory() throws Exception {
        ResourceAllocator allocator = new ResourceAllocator(10);
        int attempts = 32;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < attempts; i++) futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (allocator.reserve(Map.of(ResourceType.FIRST_AID, 1))) successes.incrementAndGet();
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }));
            assertTrue(ready.await(2, TimeUnit.SECONDS));
            start.countDown();
            for (Future<?> future : futures) future.get(2, TimeUnit.SECONDS);
            assertEquals(10, successes.get());
            assertEquals(0, allocator.available(ResourceType.FIRST_AID));
            for (int i = 0; i < successes.get(); i++)
                allocator.release(Map.of(ResourceType.FIRST_AID, 1));
            assertEquals(10, allocator.available(ResourceType.FIRST_AID));
            assertThrows(IllegalStateException.class, () -> allocator.release(Map.of(ResourceType.FIRST_AID, 1)));
            assertThrows(IllegalStateException.class, () -> allocator.release(Map.of(
                    ResourceType.SUPPLY_KITS, 1, ResourceType.FIRST_AID, 11)));
            assertEquals(10, allocator.available(ResourceType.SUPPLY_KITS));
        } finally { pool.shutdownNow(); }
    }
}
