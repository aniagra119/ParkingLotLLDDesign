package com.parkinglot;

import com.parkinglot.billing.Bill;
import com.parkinglot.billing.service.CarWashService;
import com.parkinglot.billing.service.EvChargingService;
import com.parkinglot.builder.ParkingLotBuilder;
import com.parkinglot.enums.BillStatus;
import com.parkinglot.enums.GateType;
import com.parkinglot.enums.ParkingSpotType;
import com.parkinglot.enums.PaymentMode;
import com.parkinglot.enums.TicketStatus;
import com.parkinglot.enums.VehicleType;
import com.parkinglot.enums.WashPackage;
import com.parkinglot.model.Coordinates;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ParkingLotIntegrationTest {

    private ParkingLot parkingLot;

    @BeforeEach
    void setUp() {
        parkingLot = new ParkingLotBuilder()
                .withLotId("LOT-TEST-BLR")
                .addGate("ENTRY-1", GateType.ENTRY, 1, new Coordinates(0, 0))
                .addGate("EXIT-1", GateType.EXIT, 1, new Coordinates(10, 10))
                .addFloor("F1", 1, 5, 5, 2)  // Floor 1: 5 Bike, 5 Car, 2 Truck
                .addFloor("F2", 2, 5, 5, 2)  // Floor 2: 5 Bike, 5 Car, 2 Truck
                .build();
    }

    @Test
    @DisplayName("Should successfully park a car at the nearest available spot on Floor 1")
    void testSuccessfulCarParking() {
        Vehicle car = new Vehicle("KA-01-MJ-5000", VehicleType.CAR);
        assertEquals(10, parkingLot.getAvailableSpotCount(ParkingSpotType.CAR));

        Ticket ticket = parkingLot.parkVehicle("ENTRY-1", car);

        assertNotNull(ticket);
        assertEquals("F1-C1", ticket.getSpotId());
        assertEquals(1, ticket.getFloorNumber());
        assertEquals("KA-01-MJ-5000", ticket.getVehicle().licensePlate());
        assertEquals(TicketStatus.ACTIVE, ticket.getStatus());
        assertEquals(9, parkingLot.getAvailableSpotCount(ParkingSpotType.CAR));
    }

    @Test
    @DisplayName("The ₹180 Benchmark: 5.5 hours car stay must calculate to exactly ₹180.00 base fee")
    void testProgressiveSlabCarFeeCalculation() {
        Vehicle car = new Vehicle("DL-3C-9999", VehicleType.CAR);
        Ticket ticket = parkingLot.parkVehicle("ENTRY-1", car);

        // 5 hours and 30 minutes duration (6 billable hours)
        Instant exitTime = ticket.getEntryTime().plus(Duration.ofMinutes(330));

        Bill bill = parkingLot.processExit("EXIT-1", ticket.getTicketNumber(), exitTime, PaymentMode.RAZORPAY_CARD);

        assertNotNull(bill);
        assertEquals(180.0, bill.getTotalAmount(), 0.001);
        assertEquals(BillStatus.PAID, bill.getStatus());
        assertEquals(TicketStatus.PAID, ticket.getStatus());
        // Spot must be vacated and available again
        assertEquals(10, parkingLot.getAvailableSpotCount(ParkingSpotType.CAR));
    }

    @Test
    @DisplayName("Polymorphic Amenity Decorator: Base fee (₹180) + EV (18.5 kWh @ ₹12 = ₹222) + CarWash (CAR Premium Foam = ₹300) = ₹702.00")
    void testPolymorphicAmenityBillingPipeline() {
        Vehicle car = new Vehicle("MH-02-EE-1234", VehicleType.CAR);
        Ticket ticket = parkingLot.parkVehicle("ENTRY-1", car);

        // Customer consumes dynamic services during the session:
        ticket.addService(new EvChargingService(18.5, 12.0)); // 18.5 * 12 = ₹222.00
        ticket.addService(new CarWashService(VehicleType.CAR, WashPackage.PREMIUM_FOAM)); // 250 + 50 = ₹300.00

        Instant exitTime = ticket.getEntryTime().plus(Duration.ofMinutes(330)); // 6 billable hours -> ₹180.00 base

        Bill bill = parkingLot.processExit("EXIT-1", ticket.getTicketNumber(), exitTime, PaymentMode.RAZORPAY_UPI);

        assertNotNull(bill);
        assertEquals(702.0, bill.getTotalAmount(), 0.001);
        assertEquals(BillStatus.PAID, bill.getStatus());

        // Verify itemized breakdown lines
        assertEquals(3, bill.getItemizedBreakdown().size());
        assertTrue(bill.getItemizedBreakdown().get(0).contains("Base Parking Fee: ₹180.0"));
        assertTrue(bill.getItemizedBreakdown().get(1).contains("EV Charging (18.5 kWh @ ₹12.00/kWh): ₹222.00"));
        assertTrue(bill.getItemizedBreakdown().get(2).contains("Car Wash (CAR - PREMIUM_FOAM): ₹300.00"));
    }

    @Test
    @DisplayName("Strict 1:1 Spot Compatibility: Truck cannot steal Car or Bike spots when truck spots are full")
    void testSpotCompatibility() {
        Vehicle truck1 = new Vehicle("TRUCK-1", VehicleType.TRUCK);
        Vehicle truck2 = new Vehicle("TRUCK-2", VehicleType.TRUCK);
        Vehicle truck3 = new Vehicle("TRUCK-3", VehicleType.TRUCK);
        Vehicle truck4 = new Vehicle("TRUCK-4", VehicleType.TRUCK);

        parkingLot.parkVehicle("ENTRY-1", truck1);
        parkingLot.parkVehicle("ENTRY-1", truck2);
        parkingLot.parkVehicle("ENTRY-1", truck3);
        parkingLot.parkVehicle("ENTRY-1", truck4);

        assertEquals(0, parkingLot.getAvailableSpotCount(ParkingSpotType.TRUCK));
        // Free car and bike spots must remain unassigned
        assertTrue(parkingLot.getAvailableSpotCount(ParkingSpotType.CAR) > 0);

        Vehicle truck5 = new Vehicle("TRUCK-5", VehicleType.TRUCK);
        assertThrows(IllegalStateException.class, () -> parkingLot.parkVehicle("ENTRY-1", truck5));
    }
}
