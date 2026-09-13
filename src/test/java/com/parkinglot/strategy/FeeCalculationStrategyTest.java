package com.parkinglot.strategy;

import com.parkinglot.enums.VehicleType;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.Vehicle;
import com.parkinglot.strategy.fee.FlatRateFeeStrategy;
import com.parkinglot.strategy.fee.slab.PricingSlab;
import com.parkinglot.strategy.fee.slab.TieredHourlyFeeStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class FeeCalculationStrategyTest {

    private final TieredHourlyFeeStrategy tieredStrategy = new TieredHourlyFeeStrategy();
    private final FlatRateFeeStrategy flatStrategy = new FlatRateFeeStrategy();

    @Test
    @DisplayName("Zero duration stay should incur minimum 1-hour charge; negative duration must throw")
    void testZeroOrNegativeDuration() {
        Ticket ticket = new Ticket("S1", 1, new Vehicle("C1", VehicleType.CAR), Instant.now());
        // Immediate exit incurs minimum 1-hour fee (25.0)
        assertEquals(25.0, tieredStrategy.calculateFee(ticket, ticket.getEntryTime()), 0.001);

        // Exit before entry must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                tieredStrategy.calculateFee(ticket, ticket.getEntryTime().minusSeconds(100))
        );
    }

    @ParameterizedTest(name = "Car parked for {0} minutes should cost ₹{1}")
    @CsvSource({
            "1, 25.0",      // Ceiled to 1 hr: 1 * 25 = 25
            "60, 25.0",     // Exactly 1 hr: 1 * 25 = 25
            "61, 50.0",     // Ceiled to 2 hrs: 2 * 25 = 50
            "120, 50.0",    // Exactly 2 hrs: 2 * 25 = 50
            "121, 80.0",    // Ceiled to 3 hrs: 50 + (1 * 30) = 80
            "240, 110.0",   // Exactly 4 hrs: 50 + (2 * 30) = 110
            "241, 145.0",   // Ceiled to 5 hrs: 110 + (1 * 35) = 145
            "330, 180.0",   // Exactly 5.5 hrs (6 hrs ceiled): 50 + 60 + 70 = 180
            "360, 180.0",   // Exactly 6 hrs: 50 + 60 + 70 = 180
            "420, 225.0"    // Exactly 7 hrs: 180 + (1 * 45) = 225
    })
    @DisplayName("Verify progressive slab pricing across all boundary hours for CAR")
    void testCarProgressiveSlabs(long minutes, double expectedFee) {
        Ticket ticket = new Ticket("S1", 1, new Vehicle("C1", VehicleType.CAR), Instant.now());
        Instant exitTime = ticket.getEntryTime().plus(Duration.ofMinutes(minutes));
        assertEquals(expectedFee, tieredStrategy.calculateFee(ticket, exitTime), 0.001);
    }

    @ParameterizedTest(name = "Bike parked for {0} minutes should cost ₹{1}")
    @CsvSource({
            "30, 20.0",     // 1 hr: 1 * 20 = 20
            "120, 40.0",    // 2 hrs: 2 * 20 = 40
            "180, 65.0",    // 3 hrs: 40 + (1 * 25) = 65
            "240, 90.0",    // 4 hrs: 40 + (2 * 25) = 90
            "360, 150.0",   // 6 hrs: 40 + 50 + (2 * 30) = 150
            "420, 190.0"    // 7 hrs: 150 + (1 * 40) = 190
    })
    @DisplayName("Verify progressive slab pricing across all boundary hours for BIKE")
    void testBikeProgressiveSlabs(long minutes, double expectedFee) {
        Ticket ticket = new Ticket("S1", 1, new Vehicle("B1", VehicleType.BIKE), Instant.now());
        Instant exitTime = ticket.getEntryTime().plus(Duration.ofMinutes(minutes));
        assertEquals(expectedFee, tieredStrategy.calculateFee(ticket, exitTime), 0.001);
    }

    @ParameterizedTest(name = "Truck parked for {0} minutes should cost ₹{1}")
    @CsvSource({
            "60, 50.0",     // 1 hr: 1 * 50 = 50
            "120, 100.0",   // 2 hrs: 2 * 50 = 100
            "240, 220.0",   // 4 hrs: 100 + (2 * 60) = 220
            "360, 350.0",   // 6 hrs: 220 + (2 * 65) = 350
            "420, 430.0"    // 7 hrs: 350 + (1 * 80) = 430
    })
    @DisplayName("Verify progressive slab pricing across all boundary hours for TRUCK")
    void testTruckProgressiveSlabs(long minutes, double expectedFee) {
        Ticket ticket = new Ticket("S1", 1, new Vehicle("T1", VehicleType.TRUCK), Instant.now());
        Instant exitTime = ticket.getEntryTime().plus(Duration.ofMinutes(minutes));
        assertEquals(expectedFee, tieredStrategy.calculateFee(ticket, exitTime), 0.001);
    }

    @Test
    @DisplayName("PricingSlab should throw IllegalArgumentException for invalid bounds or negative rates")
    void testPricingSlabValidations() {
        assertThrows(IllegalArgumentException.class, () -> new PricingSlab(-1, 2, 20.0));
        assertThrows(IllegalArgumentException.class, () -> new PricingSlab(4, 2, 20.0));
        assertThrows(IllegalArgumentException.class, () -> new PricingSlab(2, 2, 20.0));
        assertThrows(IllegalArgumentException.class, () -> new PricingSlab(0, 2, -10.0));

        PricingSlab slab = new PricingSlab(2, 4, 30.0);
        assertFalse(slab.appliesTo(1));
        assertTrue(slab.appliesTo(2));
        assertTrue(slab.appliesTo(3));
        assertFalse(slab.appliesTo(4));
    }

    @Test
    @DisplayName("FlatRateFeeStrategy should return fixed amounts by vehicle type regardless of stay")
    void testFlatRateFeeStrategy() {
        Ticket bikeTicket = new Ticket("B1", 1, new Vehicle("BIKE-1", VehicleType.BIKE), Instant.now());
        Ticket carTicket = new Ticket("C1", 1, new Vehicle("CAR-1", VehicleType.CAR), Instant.now());
        Ticket truckTicket = new Ticket("T1", 1, new Vehicle("TRUCK-1", VehicleType.TRUCK), Instant.now());

        Instant future = Instant.now().plus(Duration.ofHours(100));

        assertEquals(30.0, flatStrategy.calculateFee(bikeTicket, future));
        assertEquals(50.0, flatStrategy.calculateFee(carTicket, future));
        assertEquals(100.0, flatStrategy.calculateFee(truckTicket, future));
    }
    @Test
    @DisplayName("SlabBasedFeeStrategy enforces Template Method: null checks and delegated hook calculation")
    void testTemplateMethodEnforcement() {
        assertThrows(NullPointerException.class, () -> tieredStrategy.calculateFee(null, Instant.now()));
        Ticket ticket = new Ticket("S1", 1, new Vehicle("C1", VehicleType.CAR), Instant.now());
        assertThrows(NullPointerException.class, () -> tieredStrategy.calculateFee(ticket, null));
    }
}