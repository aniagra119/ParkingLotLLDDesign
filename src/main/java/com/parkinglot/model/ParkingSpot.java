package com.parkinglot.model;

import com.parkinglot.enums.ParkingSpotType;

public class ParkingSpot {
    private final String spotId;
    private final ParkingSpotType spotType;
    private volatile Vehicle currentVehicle;
    private final Coordinates coordinates;

    public ParkingSpot(String spotId, ParkingSpotType spotType, Coordinates coordinates) {
        if (spotId == null || spotId.isBlank()) {
            throw new IllegalArgumentException("Spot ID cannot be null or empty");
        }
        if (spotType == null) {
            throw new IllegalArgumentException("Spot type cannot be null");
        }
        if (coordinates == null) {
            throw new IllegalArgumentException("Coordinates cannot be null");
        }
        this.spotId = spotId;
        this.spotType = spotType;
        this.coordinates = coordinates;
    }

    public Coordinates getCoordinates() {
        return this.coordinates;
    }

    public synchronized void assignVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }
        if (this.currentVehicle != null) {
            throw new IllegalStateException("Parking spot is already occupied");
        }
        if (!this.spotType.canAccommodate(vehicle.type())) {
            throw new IllegalArgumentException("Vehicle type not compatible with parking spot type");
        }
        this.currentVehicle = vehicle;
    }

    public synchronized void vacate() {
        if (this.currentVehicle == null) {
            throw new IllegalStateException("Parking spot is already empty");
        }
        this.currentVehicle = null;
    }

    public String getSpotId() {
        return spotId;
    }

    public ParkingSpotType getSpotType() {
        return spotType;
    }

    public boolean isOccupied() {
        return this.currentVehicle != null;
    }

    public Vehicle getCurrentVehicle() {
        return currentVehicle;
    }
}
