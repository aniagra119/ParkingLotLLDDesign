package com.parkinglot.billing.decorator;

import java.util.List;

/**
 * Concrete Component: The core billable charge computed by the fee calculation strategy.
 */
public class BaseParkingCharge implements BillableItem {
    private final double cost;

    public BaseParkingCharge(double cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }
        this.cost = Math.round(cost * 100.0) / 100.0;
    }

    @Override
    public double getCost() {
        return cost;
    }

    @Override
    public List<String> getItemizedLines() {
        return List.of("Base Parking Fee: ₹" + cost);
    }
}
