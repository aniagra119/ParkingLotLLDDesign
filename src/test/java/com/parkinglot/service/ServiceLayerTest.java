package com.parkinglot.service;

import com.parkinglot.billing.Bill;
import com.parkinglot.billing.BillingService;
import com.parkinglot.enums.BillStatus;
import com.parkinglot.enums.GateType;
import com.parkinglot.enums.PaymentMode;
import com.parkinglot.enums.VehicleType;
import com.parkinglot.model.Coordinates;
import com.parkinglot.model.Gate;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.Vehicle;
import com.parkinglot.payment.external.ExternalRazorpayClient;
import com.parkinglot.payment.PaymentProcessorFactory;
import com.parkinglot.payment.PaymentService;
import com.parkinglot.payment.PaymentStatus;
import com.parkinglot.payment.PaymentTransaction;
import com.parkinglot.strategy.fee.FlatRateFeeStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceLayerTest {

    @Test
    @DisplayName("TicketManager should register, retrieve, and close active tickets")
    void testTicketManager() {
        TicketManager manager = new TicketManager();
        assertEquals(0, manager.getActiveTicketCount());

        Ticket ticket = new Ticket("S1", 1, new Vehicle("KA-01-T1", VehicleType.CAR), Instant.now());
        manager.registerTicket(ticket);

        assertEquals(1, manager.getActiveTicketCount());
        assertTrue(manager.getActiveTicket(ticket.getTicketNumber()).isPresent());
        assertEquals(ticket, manager.getActiveTicketOrThrow(ticket.getTicketNumber()));

        manager.closeTicket(ticket.getTicketNumber());
        assertEquals(0, manager.getActiveTicketCount());
        assertFalse(manager.getActiveTicket(ticket.getTicketNumber()).isPresent());
        assertThrows(IllegalArgumentException.class, () -> manager.getActiveTicketOrThrow(ticket.getTicketNumber()));
    }

    @Test
    @DisplayName("GateController should validate gate IDs and enforce correct entry/exit gate types")
    void testGateController() {
        Gate entryGate = new Gate("ENTRY-1", GateType.ENTRY, 1, new Coordinates(0, 0));
        Gate exitGate = new Gate("EXIT-1", GateType.EXIT, 1, new Coordinates(10, 10));

        GateController controller = new GateController(List.of(entryGate, exitGate));

        assertEquals(entryGate, controller.getValidatedGate("ENTRY-1", GateType.ENTRY));
        assertEquals(exitGate, controller.getValidatedGate("EXIT-1", GateType.EXIT));

        // Gate type mismatch
        assertThrows(IllegalStateException.class, () -> controller.getValidatedGate("ENTRY-1", GateType.EXIT));
        assertThrows(IllegalStateException.class, () -> controller.getValidatedGate("EXIT-1", GateType.ENTRY));

        // Unknown gate
        assertThrows(IllegalArgumentException.class, () -> controller.getValidatedGate("UNKNOWN", GateType.ENTRY));

        // Cycle gate
        controller.cycleGate("ENTRY-1");
        assertFalse(entryGate.isOpen()); // Opens then closes
    }

    @Test
    @DisplayName("BillingService should generate Bill using injected fee strategy")
    void testBillingService() {
        BillingService billingService = new BillingService(new FlatRateFeeStrategy());
        Ticket ticket = new Ticket("S1", 1, new Vehicle("C1", VehicleType.CAR), Instant.now());

        Bill bill = billingService.generateBill(ticket, ticket.getEntryTime().plusSeconds(3600));

        assertNotNull(bill);
        assertEquals(50.0, bill.getTotalAmount(), 0.001); // Flat car rate = 50.0
        assertEquals(BillStatus.UNPAID, bill.getStatus());
    }

    @Test
    @DisplayName("PaymentService should orchestrate payment settlement and record audit transaction on Bill")
    void testPaymentService() {
        PaymentProcessorFactory factory = new PaymentProcessorFactory(
                new ExternalRazorpayClient("key", "secret")
        );
        PaymentService paymentService = new PaymentService(factory);

        Ticket ticket = new Ticket("S1", 1, new Vehicle("C1", VehicleType.CAR), Instant.now());
        Bill bill = new Bill(ticket, ticket.getEntryTime().plusSeconds(3600), 50.0);

        PaymentTransaction txn = paymentService.processBillPayment(bill, PaymentMode.CASH);

        assertNotNull(txn);
        assertEquals(PaymentStatus.SUCCESS, txn.getStatus());
        assertEquals(BillStatus.PAID, bill.getStatus());
        assertEquals(1, bill.getPaymentHistory().size());
    }
}
