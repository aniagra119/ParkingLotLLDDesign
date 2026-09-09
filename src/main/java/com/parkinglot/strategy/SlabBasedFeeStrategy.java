package com.parkinglot.strategy;

import com.parkinglot.enums.VehicleType;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Template / Abstract base strategy for all pricing algorithms that calculate
 * fees based on time slabs and vehicle types.
 */
public abstract class SlabBasedFeeStrategy implements FeeCalculationStrategy {
    protected final Map<VehicleType, List<PricingSlab>> slabsByVehicleType;

    public SlabBasedFeeStrategy(Map<VehicleType, List<PricingSlab>> slabs) {
        Objects.requireNonNull(slabs, "Pricing slabs map cannot be null");
        this.slabsByVehicleType = new EnumMap<>(slabs);
    }

    /**
     * Standard duration helper: computes ceiling rounding for billable hours.
     */
    protected long calculateBillableHours(Instant entryTime, Instant exitTime) {
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("Exit time cannot be before entry time");
        }
        long minutes = Duration.between(entryTime, exitTime).toMinutes();
        if (minutes <= 0) {
            minutes = 1; // Minimum charge round-up
        }
        return (long) Math.ceil((double) minutes / 60.0);
    }

    public Map<VehicleType, List<PricingSlab>> getSlabsByVehicleType() {
        return Collections.unmodifiableMap(slabsByVehicleType);
    }
}
