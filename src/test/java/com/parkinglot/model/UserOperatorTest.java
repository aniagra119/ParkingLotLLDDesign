package com.parkinglot.model;

import com.parkinglot.ParkingLot;
import com.parkinglot.billing.Bill;
import com.parkinglot.enums.GateType;
import com.parkinglot.enums.PaymentMode;
import com.parkinglot.enums.ParkingSpotType;
import com.parkinglot.enums.UserRole;
import com.parkinglot.enums.VehicleType;
import com.parkinglot.payment.PaymentProcessorFactory;
import com.parkinglot.payment.PaymentStatus;
import com.parkinglot.payment.PaymentTransaction;
import com.parkinglot.payment.external.ExternalRazorpayClient;
import com.parkinglot.strategy.assignment.NearestAvailableSpotStrategy;
import com.parkinglot.strategy.fee.slab.TieredHourlyFeeStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User and Operator Accountability Tests")
class UserOperatorTest {

    @Test
    @DisplayName("User validation enforces non-empty fields and validates defaults")
    void testUserRecord() {
        assertThrows(IllegalArgumentException.class, () -> new User("", "Name", UserRole.PARKING_ATTENDANT));
        assertThrows(IllegalArgumentException.class, () -> new User("U1", "", UserRole.PARKING_ATTENDANT));
        assertThrows(NullPointerException.class, () -> new User("U1", "Name", null));

        User user = new User("USR-1", "Bob Smith", UserRole.PARKING_ATTENDANT);
        assertEquals("USR-1", user.userId());
        assertEquals("Bob Smith", user.name());
        assertEquals(UserRole.PARKING_ATTENDANT, user.role());
        assertNotNull(user.createdAt());

        assertNotNull(User.SYSTEM_KIOSK);
        assertEquals(UserRole.SYSTEM, User.SYSTEM_KIOSK.role());
    }

    @Test
    @DisplayName("Ticket and PaymentTransaction record operator who performed the action")
    void testTicketAndPaymentAuditing() {
        User attendant = new User("ATT-10", "Carlos Attendant", UserRole.PARKING_ATTENDANT);
        Vehicle vehicle = new Vehicle("DL-01-AB-1234", VehicleType.CAR);

        Ticket ticket = new Ticket("1-CAR-1", 1, vehicle, Instant.now(), attendant);
        assertEquals(attendant, ticket.getIssuedBy());

        Ticket autoTicket = new Ticket("1-CAR-1", 1, vehicle, Instant.now());
        assertEquals(User.SYSTEM_KIOSK, autoTicket.getIssuedBy());

        PaymentTransaction txn = new PaymentTransaction("BILL-100", 250.0, PaymentMode.CASH, attendant);
        assertEquals(attendant, txn.getProcessedBy());

        PaymentTransaction defaultTxn = new PaymentTransaction("BILL-100", 250.0, PaymentMode.CASH);
        assertEquals(User.SYSTEM_KIOSK, defaultTxn.getProcessedBy());
    }

    @Test
    @DisplayName("End-to-End ParkingLot operations record operator on Ticket and Bill transactions")
    void testEndToEndOperatorFlow() {
        ParkingFloor floor = new ParkingFloor("F1", 1);
        floor.addSpot(new ParkingSpot("F1-CAR-1", 1, ParkingSpotType.CAR, new Coordinates(10, 10)));

        Gate entryGate = new Gate("E1", GateType.ENTRY, 1, new Coordinates(0, 0));
        Gate exitGate = new Gate("X1", GateType.EXIT, 1, new Coordinates(100, 0));

        ParkingLot lot = new ParkingLot(
                "LOT-OPERATOR",
                List.of(floor),
                List.of(entryGate, exitGate),
                new NearestAvailableSpotStrategy(),
                new TieredHourlyFeeStrategy(),
                new PaymentProcessorFactory(new ExternalRazorpayClient("rzp_test_123", "secret_abc"))
        );

        User entryAttendant = new User("ATT-ENTRY", "Sam Entry", UserRole.PARKING_ATTENDANT);
        User exitAttendant = new User("ATT-EXIT", "Diana Exit", UserRole.PARKING_ATTENDANT);

        Vehicle vehicle = Vehicle.builder("MH-02-CD-5678", VehicleType.CAR)
                .withColor("White")
                .withOwnerPhone("+91-9123456789")
                .build();

        // Entry with operator
        Ticket ticket = lot.parkVehicle("E1", vehicle, entryAttendant);
        assertNotNull(ticket);
        assertEquals(entryAttendant, ticket.getIssuedBy());
        assertTrue(entryGate.getBarrier().getAuditHistory().size() >= 2);
        assertEquals(entryAttendant, entryGate.getBarrier().getAuditHistory().get(0).operator());

        // Exit with operator
        Instant exitTime = ticket.getEntryTime().plusSeconds(3600);
        Bill bill = lot.processExit("X1", ticket.getTicketNumber(), exitTime, PaymentMode.CASH, exitAttendant);

        assertNotNull(bill);
        assertTrue(bill.getSuccessfulPayment().isPresent());
        PaymentTransaction successfulPayment = bill.getSuccessfulPayment().get();
        assertEquals(exitAttendant, successfulPayment.getProcessedBy());
        assertEquals(PaymentStatus.SUCCESS, successfulPayment.getStatus());
        assertTrue(exitGate.getBarrier().getAuditHistory().size() >= 2);
        assertEquals(exitAttendant, exitGate.getBarrier().getAuditHistory().get(0).operator());
    }
}
