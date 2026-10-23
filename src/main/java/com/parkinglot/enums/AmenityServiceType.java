package com.parkinglot.enums;

public enum AmenityServiceType {
    EV_CHARGING(150.0, "EV Charging"),
    CAR_WASH(250.0, "Car Wash / Detailing");

    private final double defaultCost;
    private final String displayName;

    AmenityServiceType(double defaultCost, String displayName) {
        this.defaultCost = defaultCost;
        this.displayName = displayName;
    }

    public double getDefaultCost() {
        return defaultCost;
    }

    public String getDisplayName() {
        return displayName;
    }
}
