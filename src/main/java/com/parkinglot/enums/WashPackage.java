package com.parkinglot.enums;

public enum WashPackage {
    BASIC(150.0),
    PREMIUM_FOAM(250.0),
    COMPLETE_DETAILING(500.0);

    private final double basePrice;

    WashPackage(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getBasePrice() {
        return basePrice;
    }
}
