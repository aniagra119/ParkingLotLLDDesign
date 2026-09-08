package com.parkinglot.model;

import com.parkinglot.enums.ParkingSpotType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ParkingFloor {
    private final int floorNumber;
    private final List<ParkingSpot> spots = new CopyOnWriteArrayList<>();

    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public void addSpot(ParkingSpot spot) {
        // TODO Phase 1: Validate spot and floor match, add to spots list
        spots.add(spot);
    }

    public List<ParkingSpot> getSpots() {
        return Collections.unmodifiableList(spots);
    }

    // TODO Phase 1: Return list of spots that match spotType and are NOT occupied
    public List<ParkingSpot> getAvailableSpots(ParkingSpotType type) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public int getFloorNumber() {
        return floorNumber;
    }
}
