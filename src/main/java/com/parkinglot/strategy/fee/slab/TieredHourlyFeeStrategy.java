package com.parkinglot.strategy.fee.slab;

import com.parkinglot.config.ParkingConfiguration;
import com.parkinglot.enums.VehicleType;
import com.parkinglot.model.Ticket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Progressive tiered hourly fee calculation strategy.
 * Progressively sums rates across hourly slabs with ceiling round-up.
 */
public class TieredHourlyFeeStrategy extends SlabBasedFeeStrategy {

    public TieredHourlyFeeStrategy() {
        super(createDefaultSlabs(), ParkingConfiguration.defaultConfig());
    }

    public TieredHourlyFeeStrategy(ParkingConfiguration configuration) {
        super(createDefaultSlabs(), configuration);
    }

    public TieredHourlyFeeStrategy(Map<VehicleType, List<PricingSlab>> customSlabs) {
        super(customSlabs, ParkingConfiguration.defaultConfig());
    }

    public TieredHourlyFeeStrategy(Map<VehicleType, List<PricingSlab>> customSlabs, ParkingConfiguration configuration) {
        super(customSlabs, configuration);
    }

    private static Map<VehicleType, List<PricingSlab>> createDefaultSlabs() {
        Map<VehicleType, List<PricingSlab>> map = new EnumMap<>(VehicleType.class);

        // BIKE slabs
        List<PricingSlab> bikeSlabs = new ArrayList<>();
        bikeSlabs.add(new PricingSlab(0, 2, 20.0));
        bikeSlabs.add(new PricingSlab(2, 4, 25.0));
        bikeSlabs.add(new PricingSlab(4, 6, 30.0));
        bikeSlabs.add(new PricingSlab(6, Integer.MAX_VALUE, 40.0));
        map.put(VehicleType.BIKE, Collections.unmodifiableList(bikeSlabs));

        // CAR slabs
        List<PricingSlab> carSlabs = new ArrayList<>();
        carSlabs.add(new PricingSlab(0, 2, 25.0));
        carSlabs.add(new PricingSlab(2, 4, 30.0));
        carSlabs.add(new PricingSlab(4, 6, 35.0));
        carSlabs.add(new PricingSlab(6, Integer.MAX_VALUE, 45.0));
        map.put(VehicleType.CAR, Collections.unmodifiableList(carSlabs));

        // TRUCK slabs
        List<PricingSlab> truckSlabs = new ArrayList<>();
        truckSlabs.add(new PricingSlab(0, 2, 50.0));
        truckSlabs.add(new PricingSlab(2, 4, 60.0));
        truckSlabs.add(new PricingSlab(4, 6, 65.0));
        truckSlabs.add(new PricingSlab(6, Integer.MAX_VALUE, 80.0));
        map.put(VehicleType.TRUCK, Collections.unmodifiableList(truckSlabs));

        return Collections.unmodifiableMap(map);
    }

    @Override
    public double calculateFee(Ticket ticket, Instant exitTime) {
        Objects.requireNonNull(ticket, "Ticket cannot be null");
        Objects.requireNonNull(exitTime, "Exit time cannot be null");

        long billableHours = calculateBillableHours(ticket.getEntryTime(), exitTime);
        if (billableHours <= 0) {
            return 0.0;
        }

        VehicleType vType = ticket.getVehicle().type();
        List<PricingSlab> slabs = slabsByVehicleType.get(vType);
        if (slabs == null || slabs.isEmpty()) {
            throw new IllegalStateException("No pricing slabs configured for vehicle type: " + vType);
        }

        double totalFee = 0.0;
        for (long hour = 0; hour < billableHours; hour++) {
            final long currentHour = hour;
            PricingSlab applicableSlab = slabs.stream()
                    .filter(s -> s.appliesTo((int) Math.min(currentHour, Integer.MAX_VALUE - 1)))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No applicable slab found for hour: " + currentHour));

            totalFee += applicableSlab.hourlyRate();
        }

        return totalFee;
    }
}
