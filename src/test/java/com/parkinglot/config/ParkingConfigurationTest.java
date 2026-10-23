package com.parkinglot.config;

import com.parkinglot.enums.VehicleType;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.Vehicle;
import com.parkinglot.strategy.fee.slab.TieredHourlyFeeStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ParkingConfigurationTest {

    @Test
    @DisplayName("Default configuration should enforce strict immediate minimum 1-hour billing")
    void testDefaultConfiguration() {
        ParkingConfiguration config = ParkingConfiguration.defaultConfig();
        assertEquals(Duration.ZERO, config.getGracePeriod());
        assertEquals(Duration.ofMinutes(60), config.getRoundingInterval());
        assertEquals(500.0, config.getLostTicketPenalty());

        Instant now = Instant.now();
        assertEquals(1, config.calculateBillableHours(now, now)); // Immediate exit is 1 hour
        assertEquals(1, config.calculateBillableHours(now, now.plus(Duration.ofMinutes(45)))); // 45m ceiled to 1 hr
        assertEquals(2, config.calculateBillableHours(now, now.plus(Duration.ofMinutes(65)))); // 65m ceiled to 2 hrs
    }

    @Test
    @DisplayName("Grace Period: Stays within grace period should calculate to 0 billable hours and 0.0 fee")
    void testGracePeriodCalculation() {
        ParkingConfiguration airportConfig = ParkingConfiguration.builder()
                .withGracePeriod(Duration.ofMinutes(15))
                .withRoundingInterval(Duration.ofMinutes(60))
                .build();

        TieredHourlyFeeStrategy strategy = new TieredHourlyFeeStrategy(airportConfig);
        Ticket carTicket = new Ticket("S1", 1, new Vehicle("AIRPORT-1", VehicleType.CAR), Instant.now());

        // 10-minute stay is within 15-minute grace period -> ₹0.00
        Instant exitTimeWithinGrace = carTicket.getEntryTime().plus(Duration.ofMinutes(10));
        assertEquals(0.0, strategy.calculateFee(carTicket, exitTimeWithinGrace), 0.001);

        // Exactly 15 minutes is within grace period -> ₹0.00
        Instant exitAtGrace = carTicket.getEntryTime().plus(Duration.ofMinutes(15));
        assertEquals(0.0, strategy.calculateFee(carTicket, exitAtGrace), 0.001);

        // 16 minutes exceeds grace period -> ceiled to 1 hour (₹25.00)
        Instant exitPastGrace = carTicket.getEntryTime().plus(Duration.ofMinutes(16));
        assertEquals(25.0, strategy.calculateFee(carTicket, exitPastGrace), 0.001);
    }

    @Test
    @DisplayName("Vehicle.Builder should create rich immutable Vehicle with optional color and owner contact")
    void testVehicleBuilder() {
        Vehicle car = Vehicle.builder("KA-05-AB-9999", VehicleType.CAR)
                .withColor("Midnight Blue")
                .withOwnerPhone("+91-9876543210")
                .build();

        assertEquals("KA-05-AB-9999", car.licensePlate());
        assertEquals(VehicleType.CAR, car.type());
        assertEquals("Midnight Blue", car.color());
        assertEquals("+91-9876543210", car.ownerPhone());

        // Legacy 2-arg constructor still works
        Vehicle bike = new Vehicle("KA-05-BIKE", VehicleType.BIKE);
        assertNull(bike.color());
        assertNull(bike.ownerPhone());
    }
}
