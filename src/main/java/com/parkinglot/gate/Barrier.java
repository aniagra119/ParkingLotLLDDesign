package com.parkinglot.gate;

import java.util.Objects;

/**
 * Encapsulates the physical boom barrier actuator.
 */
public class Barrier {
    private final String barrierId;
    private volatile boolean open;

    public Barrier(String barrierId) {
        this.barrierId = Objects.requireNonNull(barrierId, "Barrier ID cannot be null");
        this.open = false;
    }

    public synchronized void open() {
        this.open = true;
    }

    public synchronized void close() {
        this.open = false;
    }

    public boolean isOpen() {
        return open;
    }

    public String getBarrierId() {
        return barrierId;
    }
}
