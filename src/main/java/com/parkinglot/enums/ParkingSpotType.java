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
}
