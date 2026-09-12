package com.parkinglot.model;

import com.parkinglot.enums.GateType;
import com.parkinglot.enums.ParkingSpotType;
import com.parkinglot.enums.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParkingModelTest {

    @Test
    @DisplayName("ParkingFloor should enforce validation on floorId and prevent spot floor mismatches")
    void testParkingFloorValidations() {
        assertThrows(IllegalArgumentException.class, () -> new ParkingFloor(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new ParkingFloor("   ", 1));

        ParkingFloor floor1 = new ParkingFloor("F1", 1);
        ParkingSpot validSpot = new ParkingSpot("S1", 1, ParkingSpotType.CAR, new Coordinates(0, 0));
        floor1.addSpot(validSpot);

        // Cannot add spot belonging to another floor
        ParkingSpot wrongFloorSpot = new ParkingSpot("S2", 2, ParkingSpotType.CAR, new Coordinates(1, 1));
        assertThrows(IllegalArgumentException.class, () -> floor1.addSpot(wrongFloorSpot));

        // Cannot add duplicate spot ID
        ParkingSpot duplicateSpot = new ParkingSpot("S1", 1, ParkingSpotType.CAR, new Coordinates(2, 2));
        assertThrows(IllegalArgumentException.class, () -> floor1.addSpot(duplicateSpot));
    }

    @Test
    @DisplayName("ParkingSpot should correctly manage vehicle occupancy lifecycle")
    void testParkingSpotLifecycle() {
        ParkingSpot spot = new ParkingSpot("S-CAR", 1, ParkingSpotType.CAR, new Coordinates(5, 5));
        assertFalse(spot.isOccupied());
        assertNull(spot.getCurrentVehicle());

        Vehicle car = new Vehicle("KA-01-1234", VehicleType.CAR);
        spot.assignVehicle(car);
        assertTrue(spot.isOccupied());
        assertEquals(car, spot.getCurrentVehicle());

        // Assigning to already occupied spot must throw
        Vehicle car2 = new Vehicle("KA-01-5678", VehicleType.CAR);
        assertThrows(IllegalStateException.class, () -> spot.assignVehicle(car2));

        spot.vacate();
        assertFalse(spot.isOccupied());
        assertNull(spot.getCurrentVehicle());
    }

    @Test
    @DisplayName("Gate should manage open and close cycle correctly")
    void testGateLifecycle() {
        Gate gate = new Gate("G1", GateType.ENTRY, 1, new Coordinates(0, 0));
        assertFalse(gate.isOpen());

        gate.open();
        assertTrue(gate.isOpen());

        gate.close();
        assertFalse(gate.isOpen());
    }

    @Test
    @DisplayName("Coordinates Manhattan distance should measure orthogonal aisle distance")
    void testCoordinatesManhattanDistance() {
        Coordinates c1 = new Coordinates(0, 0);
        Coordinates c2 = new Coordinates(3, 4);

        // |3 - 0| + |4 - 0| = 7
        assertEquals(7, c1.manhattanDistance(c2));
        assertEquals(7, c2.manhattanDistance(c1));
    }

    @Test
    @DisplayName("Vehicle should validate license plate and reject null/blank values")
    void testVehicleValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Vehicle(null, VehicleType.CAR));
        assertThrows(IllegalArgumentException.class, () -> new Vehicle("   ", VehicleType.CAR));
        assertThrows(IllegalArgumentException.class, () -> new Vehicle("KA-01", null));
    }
}
