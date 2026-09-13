package com.parkinglot.model;

import com.parkinglot.enums.GateType;
import java.util.Objects;

/**
 * Represents a physical entry or exit checkpoint of the parking lot.
 * Composition: A Gate creates, owns, and controls its physical Barrier hardware.
 */
public class Gate {
    private final String gateId;
    private final GateType gateType;
    private final int floorNumber;
    private final Coordinates coordinates;
    private final Barrier barrier;

    public Gate(String gateId, GateType gateType, int floorNumber, Coordinates coordinates) {
        this.gateId = Objects.requireNonNull(gateId, "Gate ID cannot be null");
        this.gateType = Objects.requireNonNull(gateType, "Gate type cannot be null");
        this.floorNumber = floorNumber;
        this.coordinates = Objects.requireNonNull(coordinates, "Coordinates cannot be null");
        this.barrier = new Barrier("BARRIER-" + gateId);
    }

    public synchronized void open() {
        this.barrier.raise();
    }

    public synchronized void close() {
        this.barrier.lower();
    }

    public boolean isOpen() {
        return this.barrier.isRaised();
    }

    public Barrier getBarrier() {
        return barrier;
    }

    public String getGateId() {
        return gateId;
    }

    public GateType getGateType() {
        return gateType;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }
}
