package com.parkinglot.model;

import com.parkinglot.enums.VehicleType;

public record Vehicle(String licensePlate, VehicleType type) {
    public Vehicle {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("License plate cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Vehicle type cannot be null");
        }
    }
}
