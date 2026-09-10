package com.parkinglot.billing;

import java.util.List;

public class BaseParkingCharge implements BillableItem {
    private final double baseAmount;

    public BaseParkingCharge(double baseAmount) {
        if (baseAmount < 0) {
            throw new IllegalArgumentException("Base parking fee cannot be negative");
        }
        this.baseAmount = baseAmount;
    }

    @Override
    public double getCost() {
        return baseAmount;
    }

    @Override
    public String getDescription() {
        return "Base Parking Fee: ₹" + baseAmount;
    }

    @Override
    public List<String> getItemizedLines() {
        return List.of("Base Parking Fee: ₹" + baseAmount);
    }
}
