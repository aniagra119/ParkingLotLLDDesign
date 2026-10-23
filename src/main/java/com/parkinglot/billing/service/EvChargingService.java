package com.parkinglot.billing.service;

public class EvChargingService implements AmenityService {
    private final double kwhConsumed;
    private final double ratePerKwh;

    public EvChargingService(double kwhConsumed, double ratePerKwh) {
        if (kwhConsumed < 0 || ratePerKwh < 0) {
            throw new IllegalArgumentException("kWh consumed and rate cannot be negative");
        }
        this.kwhConsumed = kwhConsumed;
        this.ratePerKwh = ratePerKwh;
    }

    public EvChargingService(double kwhConsumed) {
        this(kwhConsumed, 12.0); // Default ₹12/kWh
    }

    @Override
    public double calculateCost() {
        return Math.round(kwhConsumed * ratePerKwh * 100.0) / 100.0;
    }

    @Override
    public String getItemizedDescription() {
        return String.format("EV Charging (%.1f kWh @ ₹%.2f/kWh): ₹%.2f",
                kwhConsumed, ratePerKwh, calculateCost());
    }

    public double getKwhConsumed() {
        return kwhConsumed;
    }

    public double getRatePerKwh() {
        return ratePerKwh;
    }
}
