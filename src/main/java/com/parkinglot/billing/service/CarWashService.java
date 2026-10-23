package com.parkinglot.billing.service;

import com.parkinglot.enums.VehicleType;
import com.parkinglot.enums.WashPackage;
import java.util.Objects;

public class CarWashService implements AmenityService {
    private final VehicleType vehicleType;
    private final WashPackage washPackage;

    public CarWashService(VehicleType vehicleType, WashPackage washPackage) {
        this.vehicleType = Objects.requireNonNull(vehicleType, "Vehicle type cannot be null");
        this.washPackage = Objects.requireNonNull(washPackage, "Wash package cannot be null");
    }

    @Override
    public double calculateCost() {
        double base = washPackage.getBasePrice();
        double vehicleSurcharge = switch (vehicleType) {
            case BIKE -> 0.0;
            case CAR -> 50.0;
            case TRUCK -> 150.0;
        };
        return base + vehicleSurcharge;
    }

    @Override
    public String getItemizedDescription() {
        return String.format("Car Wash (%s - %s): ₹%.2f",
                vehicleType, washPackage, calculateCost());
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public WashPackage getWashPackage() {
        return washPackage;
    }
}
