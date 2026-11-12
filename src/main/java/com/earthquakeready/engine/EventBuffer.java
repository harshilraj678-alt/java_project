package com.earthquakeready.engine;

import java.util.ArrayDeque;
import java.util.Deque;

public final class EventBuffer<E> {
    private final Deque<E> queue = new ArrayDeque<>();
    private final int capacity;
    private boolean closed;

    public EventBuffer(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
    }

    public synchronized void put(E value) throws InterruptedException {
        if (value == null) throw new IllegalArgumentException("Null values are not accepted");
        while (queue.size() == capacity && !closed) wait();
        if (closed) throw new IllegalStateException("Buffer is closed");
        queue.addLast(value);
        notifyAll();
    }

    public synchronized E take() throws InterruptedException {
        while (queue.isEmpty() && !closed) wait();
        if (queue.isEmpty()) return null;
        E value = queue.removeFirst();
        notifyAll();
        return value;
    }

    public synchronized void close() {
        closed = true;
        notifyAll();
    }
    public synchronized int size() { return queue.size(); }
    public int capacity() { return capacity; }
    public synchronized boolean isClosed() { return closed; }
}
