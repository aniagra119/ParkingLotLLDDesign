package com.parkinglot.strategy.fee.slab;

import com.parkinglot.config.ParkingConfiguration;
import com.parkinglot.enums.VehicleType;
import com.parkinglot.strategy.fee.FeeCalculationStrategy;
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
    protected final ParkingConfiguration configuration;

    public SlabBasedFeeStrategy(Map<VehicleType, List<PricingSlab>> slabs, ParkingConfiguration configuration) {
        Objects.requireNonNull(slabs, "Pricing slabs map cannot be null");
        this.slabsByVehicleType = new EnumMap<>(slabs);
        this.configuration = configuration != null ? configuration : ParkingConfiguration.defaultConfig();
    }

    public SlabBasedFeeStrategy(Map<VehicleType, List<PricingSlab>> slabs) {
        this(slabs, ParkingConfiguration.defaultConfig());
    }

    /**
     * Duration helper: delegates to ParkingConfiguration for grace period and ceiling calculations.
     */
    protected long calculateBillableHours(Instant entryTime, Instant exitTime) {
        return configuration.calculateBillableHours(entryTime, exitTime);
    }

    public Map<VehicleType, List<PricingSlab>> getSlabsByVehicleType() {
        return Collections.unmodifiableMap(slabsByVehicleType);
    }

    public ParkingConfiguration getConfiguration() {
        return configuration;
    }
}
