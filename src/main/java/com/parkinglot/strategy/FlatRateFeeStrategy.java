package com.parkinglot.strategy;

import com.parkinglot.enums.VehicleType;
import com.parkinglot.model.Ticket;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Example alternative non-slab strategy: Charges a flat parking fee per stay.
 */
public class FlatRateFeeStrategy implements FeeCalculationStrategy {
    private final Map<VehicleType, Double> flatRatesByVehicleType;

    public FlatRateFeeStrategy() {
        Map<VehicleType, Double> map = new EnumMap<>(VehicleType.class);
        map.put(VehicleType.BIKE, 30.0);
        map.put(VehicleType.CAR, 50.0);
        map.put(VehicleType.TRUCK, 100.0);
        this.flatRatesByVehicleType = Collections.unmodifiableMap(map);
    }

    public FlatRateFeeStrategy(Map<VehicleType, Double> customRates) {
        this.flatRatesByVehicleType = new EnumMap<>(Objects.requireNonNull(customRates, "Rates cannot be null"));
    }

    @Override
    public double calculateFee(Ticket ticket, Instant exitTime) {
        Objects.requireNonNull(ticket, "Ticket cannot be null");
        VehicleType type = ticket.getVehicle().type();
        Double rate = flatRatesByVehicleType.get(type);
        if (rate == null) {
            throw new IllegalStateException("No flat rate configured for vehicle type: " + type);
        }
        return rate;
    }
}
