package com.earthquakeready.engine;

import com.earthquakeready.domain.ResourceType;
import java.util.EnumMap;
import java.util.Map;

/** All inventory mutations share this allocator's single monitor; never call I/O or UI code while holding it. */
public final class ResourceAllocator {
    private final EnumMap<ResourceType, Integer> total = new EnumMap<>(ResourceType.class);
    private final EnumMap<ResourceType, Integer> available = new EnumMap<>(ResourceType.class);

    public ResourceAllocator(int units) {
        if (units < 1) throw new IllegalArgumentException("Resource units must be positive");
        for (ResourceType type : ResourceType.values()) {
            int amount = type == ResourceType.SHELTER_BEDS ? units * 5 : units;
            total.put(type, amount);
            available.put(type, amount);
        }
    }

    private ResourceAllocator(Map<ResourceType, Integer> totalInventory, Map<ResourceType, Integer> inventory) {
        for (ResourceType type : ResourceType.values()) {
            int amount = inventory.getOrDefault(type, 0);
            int capacity = totalInventory.getOrDefault(type, 0);
            if (amount < 0 || capacity < amount) throw new IllegalArgumentException("Invalid saved resource inventory");
            available.put(type, amount);
            total.put(type, capacity);
        }
    }

    public static ResourceAllocator fromInventory(int initialUnits, Map<ResourceType, Integer> inventory) {
        if (initialUnits < 1 || inventory == null) throw new IllegalArgumentException("Invalid saved inventory");
        EnumMap<ResourceType, Integer> total = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values())
            total.put(type, type == ResourceType.SHELTER_BEDS ? initialUnits * 5 : initialUnits);
        return new ResourceAllocator(total, inventory);
    }

    public static ResourceAllocator fromInventory(Map<ResourceType, Integer> total,
                                                  Map<ResourceType, Integer> available) {
        if (total == null || available == null) throw new IllegalArgumentException("Saved inventory is required");
        return new ResourceAllocator(total, available);
    }

    public synchronized boolean reserve(Map<ResourceType, Integer> request) {
        if (request == null || request.isEmpty()) return false;
        for (var entry : request.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() < 1
                    || available.get(entry.getKey()) < entry.getValue()) return false;
        }
        request.forEach((type, quantity) -> available.put(type, available.get(type) - quantity));
        return true;
    }

    public synchronized void release(Map<ResourceType, Integer> allocation) {
        if (allocation == null || allocation.isEmpty()) throw new IllegalArgumentException("Allocation is required");
        for (var entry : allocation.entrySet()) {
            ResourceType type = entry.getKey();
            Integer quantity = entry.getValue();
            if (type == null || quantity == null || quantity < 1)
                throw new IllegalArgumentException("Invalid resource release");
            if (available.get(type) + quantity > total.get(type))
                throw new IllegalStateException("Resource conservation violated");
        }
        allocation.forEach((type, quantity) -> available.put(type, available.get(type) + quantity));
    }
    public synchronized int available(ResourceType type) { return available.get(type); }
    public synchronized Map<ResourceType, Integer> inventory() { return Map.copyOf(available); }
    public synchronized Map<ResourceType, Integer> totalInventory() { return Map.copyOf(total); }
    public synchronized InventorySnapshot inventorySnapshot() {
        return new InventorySnapshot(Map.copyOf(total), Map.copyOf(available));
    }
    public record InventorySnapshot(Map<ResourceType, Integer> total, Map<ResourceType, Integer> available) {}
}
