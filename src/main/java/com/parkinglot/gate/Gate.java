package com.parkinglot.gate;

import com.parkinglot.enums.GateType;
import com.parkinglot.model.Coordinates;
import java.util.Objects;

public class Gate {
    private final String gateId;
    private final GateType gateType;
    private final Barrier barrier;
    private final int floorNumber;
    private final Coordinates coordinates;

    public Gate(String gateId, GateType gateType, Barrier barrier, int floorNumber, Coordinates coordinates) {
        this.gateId = Objects.requireNonNull(gateId, "Gate ID cannot be null");
        this.gateType = Objects.requireNonNull(gateType, "Gate type cannot be null");
        this.barrier = Objects.requireNonNull(barrier, "Barrier cannot be null");
        this.floorNumber = floorNumber;
        this.coordinates = Objects.requireNonNull(coordinates, "Coordinates cannot be null");
    }

    public String getGateId() {
        return gateId;
    }

    public GateType getGateType() {
        return gateType;
    }

    public Barrier getBarrier() {
        return barrier;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }
}
