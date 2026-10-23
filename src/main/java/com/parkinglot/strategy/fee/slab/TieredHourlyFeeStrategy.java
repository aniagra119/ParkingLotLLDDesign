package com.parkinglot.strategy.fee.slab;

import com.parkinglot.config.ParkingConfiguration;
import com.parkinglot.enums.VehicleType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete Strategy in the Template Method Pattern:
 * Implements {@link #computeSlabFee(long, List)} to progressively accumulate hourly rates
 * across sequential tier slabs.
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
    protected double computeSlabFee(long billableHours, List<PricingSlab> slabs) {
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
