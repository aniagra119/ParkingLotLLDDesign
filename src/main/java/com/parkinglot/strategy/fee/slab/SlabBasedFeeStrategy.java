package com.parkinglot.strategy.fee.slab;

import com.parkinglot.config.ParkingConfiguration;
import com.parkinglot.enums.VehicleType;
import com.parkinglot.model.Ticket;
import com.parkinglot.strategy.fee.FeeCalculationStrategy;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Template Method Pattern (GoF Behavioral):
 * Defines the invariant algorithm skeleton for all slab-based pricing strategies.
 * Subclasses implement the primitive operation {@link #computeSlabFee(long, List)}
 * to customize the mathematical formula across pricing tiers.
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
     * Invariant Template Method:
     * Enforces parameter validation, billable duration calculation with grace periods
     * and ceiling round-up, vehicle type slab resolution, and delegates calculation to subclasses.
     */
    @Override
    public final double calculateFee(Ticket ticket, Instant exitTime) {
        Objects.requireNonNull(ticket, "Ticket cannot be null");
        Objects.requireNonNull(exitTime, "Exit time cannot be null");

        long billableHours = configuration.calculateBillableHours(ticket.getEntryTime(), exitTime);
        if (billableHours <= 0) {
            return 0.0;
        }

        VehicleType vehicleType = ticket.getVehicle().type();
        List<PricingSlab> slabs = slabsByVehicleType.get(vehicleType);
        if (slabs == null || slabs.isEmpty()) {
            throw new IllegalStateException("No pricing slabs configured for vehicle type: " + vehicleType);
        }

        return computeSlabFee(billableHours, slabs);
    }

    /**
     * Primitive hook method: subclasses implement the pricing formula across the resolved slabs.
     */
    protected abstract double computeSlabFee(long billableHours, List<PricingSlab> slabs);

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
