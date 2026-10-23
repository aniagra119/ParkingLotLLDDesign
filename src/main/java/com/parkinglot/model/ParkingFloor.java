package com.parkinglot.model;

import com.parkinglot.enums.ParkingSpotType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParkingFloor {
    private final String floorId;
    private final int floorNumber;
    private final Map<String, ParkingSpot> spots;

    public ParkingFloor(String floorId, int floorNumber) {
        this.floorId = floorId;
        this.floorNumber = floorNumber;
        this.spots = new ConcurrentHashMap<>();

    }

    public ParkingSpot getSpot(String spotId) {
        return spots.get(spotId);
    }

    public String getFloorId() {
        return floorId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void addSpot(ParkingSpot spot) {
        spots.put(spot.getSpotId(), spot);
    }

    public List<ParkingSpot> getSpots() {
        return new ArrayList<>(spots.values());
    }

    public List<ParkingSpot> getAvailableSpots(ParkingSpotType type) {
        List<ParkingSpot> availableSpots = new ArrayList<>();
        for (ParkingSpot spot : spots.values()) {
            if (spot.getSpotType() == type && !spot.isOccupied()) {
                availableSpots.add(spot);
            }
        }
        return availableSpots;
    }
}
