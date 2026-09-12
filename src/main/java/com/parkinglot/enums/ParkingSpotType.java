package com.parkinglot.enums;

import java.util.Set;

public enum ParkingSpotType {
    BIKE(VehicleType.BIKE),
    CAR(VehicleType.CAR),
    TRUCK(VehicleType.TRUCK);

    private final Set<VehicleType> compatibleVehicleType;

    ParkingSpotType(VehicleType... compatibleVehicleType) {
        this.compatibleVehicleType = Set.of(compatibleVehicleType);
    }

    public boolean canAccommodate(VehicleType vehicleType) {
        return compatibleVehicleType.contains(vehicleType);
    }

    public static ParkingSpotType fromVehicleType(VehicleType type) {
        if (type == null) {
            throw new IllegalArgumentException("Vehicle type cannot be null");
        }
        return valueOf(type.name());
    }
}
