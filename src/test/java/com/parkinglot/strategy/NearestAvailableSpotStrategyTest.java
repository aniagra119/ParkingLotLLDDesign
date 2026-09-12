package com.parkinglot.strategy;

import com.parkinglot.enums.ParkingSpotType;
import com.parkinglot.model.Coordinates;
import com.parkinglot.model.ParkingFloor;
import com.parkinglot.model.ParkingSpot;
import com.parkinglot.strategy.assignment.NearestAvailableSpotStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NearestAvailableSpotStrategyTest {

    private NearestAvailableSpotStrategy strategy;
    private List<ParkingFloor> floors;

    @BeforeEach
    void setUp() {
        strategy = new NearestAvailableSpotStrategy();
        floors = new ArrayList<>();

        ParkingFloor f1 = new ParkingFloor("F1", 1);
        // Floor 1 spots at coordinates (2, 2) and (8, 8)
        f1.addSpot(new ParkingSpot("F1-C1", 1, ParkingSpotType.CAR, new Coordinates(2, 2)));
        f1.addSpot(new ParkingSpot("F1-C2", 1, ParkingSpotType.CAR, new Coordinates(8, 8)));

        ParkingFloor f2 = new ParkingFloor("F2", 2);
        // Floor 2 spot at coordinate (0, 1) - closer in 2D than (2, 2) to gate (0, 0),
        // but floor elevation penalty should prioritize Floor 1 when entering on Floor 1!
        f2.addSpot(new ParkingSpot("F2-C1", 2, ParkingSpotType.CAR, new Coordinates(0, 1)));

        floors.add(f1);
        floors.add(f2);
    }

    @Test
    @DisplayName("Should prioritize closest 2D Manhattan distance spot on the same floor")
    void testSameFloorNearestSpotSelection() {
        Coordinates gateCoord = new Coordinates(0, 0);

        Optional<ParkingSpot> spot = strategy.findSpot(floors, ParkingSpotType.CAR, 1, gateCoord);

        assertTrue(spot.isPresent());
        // F1-C1 (distance |2-0| + |2-0| = 4) vs F1-C2 (|8-0| + |8-0| = 16)
        assertEquals("F1-C1", spot.get().getSpotId());
    }

    @Test
    @DisplayName("Elevation Penalty: Floor 1 spot must be preferred over Floor 2 even if Floor 2 spot is closer in 2D")
    void testElevationDeltaPrioritization() {
        Coordinates gateCoord = new Coordinates(0, 0);

        Optional<ParkingSpot> spot = strategy.findSpot(floors, ParkingSpotType.CAR, 1, gateCoord);

        assertTrue(spot.isPresent());
        // Floor 1 elevation delta = 0 * 100 + 4 = 4
        // Floor 2 elevation delta = 1 * 100 + 1 = 101
        assertEquals("F1-C1", spot.get().getSpotId());
    }

    @Test
    @DisplayName("Should pick Floor 2 spot when all Floor 1 spots are occupied")
    void testFallbackToHigherFloorWhenCurrentFloorFull() {
        Coordinates gateCoord = new Coordinates(0, 0);

        // Occupy all floor 1 spots
        floors.get(0).getSpot("F1-C1").assignVehicle(new com.parkinglot.model.Vehicle("C1", com.parkinglot.enums.VehicleType.CAR));
        floors.get(0).getSpot("F1-C2").assignVehicle(new com.parkinglot.model.Vehicle("C2", com.parkinglot.enums.VehicleType.CAR));

        Optional<ParkingSpot> spot = strategy.findSpot(floors, ParkingSpotType.CAR, 1, gateCoord);

        assertTrue(spot.isPresent());
        assertEquals("F2-C1", spot.get().getSpotId());
        assertEquals(2, spot.get().getFloorNumber());
    }

    @Test
    @DisplayName("Should return empty optional when no matching spots are available")
    void testNoAvailableSpotsFound() {
        Coordinates gateCoord = new Coordinates(0, 0);

        // Asking for TRUCK spots when only CAR spots exist
        Optional<ParkingSpot> spot = strategy.findSpot(floors, ParkingSpotType.TRUCK, 1, gateCoord);

        assertFalse(spot.isPresent());
    }

    @Test
    @DisplayName("Should safely handle null or empty arguments")
    void testNullSafeGuards() {
        assertFalse(strategy.findSpot(null, ParkingSpotType.CAR, 1, new Coordinates(0, 0)).isPresent());
        assertFalse(strategy.findSpot(Collections.emptyList(), ParkingSpotType.CAR, 1, new Coordinates(0, 0)).isPresent());
        assertFalse(strategy.findSpot(floors, null, 1, new Coordinates(0, 0)).isPresent());
        assertFalse(strategy.findSpot(floors, ParkingSpotType.CAR, 1, null).isPresent());
    }
}
