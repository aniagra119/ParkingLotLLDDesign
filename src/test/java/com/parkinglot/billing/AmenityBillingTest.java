package com.parkinglot.billing;

import com.parkinglot.billing.service.CarWashService;
import com.parkinglot.billing.service.EvChargingService;
import com.parkinglot.enums.BillStatus;
import com.parkinglot.enums.PaymentMode;
import com.parkinglot.enums.VehicleType;
import com.parkinglot.enums.WashPackage;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.Vehicle;
import com.parkinglot.payment.PaymentTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AmenityBillingTest {

    @Test
    @DisplayName("EvChargingService should calculate cost strictly based on kWh * rate")
    void testEvChargingCalculation() {
        EvChargingService evService = new EvChargingService(25.0, 15.0);
        assertEquals(375.0, evService.calculateCost(), 0.001);
        assertEquals("EV Charging (25.0 kWh @ ₹15.00/kWh): ₹375.00", evService.getItemizedDescription());

        // Negative kWh or rate should be rejected
        assertThrows(IllegalArgumentException.class, () -> new EvChargingService(-5.0, 10.0));
        assertThrows(IllegalArgumentException.class, () -> new EvChargingService(5.0, -10.0));
    }

    @Test
    @DisplayName("CarWashService should apply vehicle size surcharges on package rates")
    void testCarWashCalculation() {
        // BASIC wash (150) + BIKE surcharge (0) = 150
        CarWashService bikeWash = new CarWashService(VehicleType.BIKE, WashPackage.BASIC);
        assertEquals(150.0, bikeWash.calculateCost(), 0.001);

        // PREMIUM_FOAM wash (250) + CAR surcharge (50) = 300
        CarWashService carWash = new CarWashService(VehicleType.CAR, WashPackage.PREMIUM_FOAM);
        assertEquals(300.0, carWash.calculateCost(), 0.001);

        // COMPLETE_DETAILING wash (500) + TRUCK surcharge (150) = 650
        CarWashService truckWash = new CarWashService(VehicleType.TRUCK, WashPackage.COMPLETE_DETAILING);
        assertEquals(650.0, truckWash.calculateCost(), 0.001);
    }

    @Test
    @DisplayName("Bill should aggregate base fee and services into itemized lines and update paid status")
    void testBillAggregationAndPaymentLifecycle() {
        Ticket ticket = new Ticket("S1", 1, new Vehicle("KA-01-9999", VehicleType.CAR), Instant.now());
        ticket.addService(new EvChargingService(10.0, 12.0)); // 120
        ticket.addService(new CarWashService(VehicleType.CAR, WashPackage.BASIC)); // 150 + 50 = 200

        Bill bill = new Bill(ticket, ticket.getEntryTime().plusSeconds(3600), 100.0);

        // 100 base + 120 EV + 200 Wash = 420
        assertEquals(420.0, bill.getTotalAmount(), 0.001);
        assertEquals(BillStatus.UNPAID, bill.getStatus());

        List<String> lines = bill.getItemizedBreakdown();
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("Base Parking Fee: ₹100.0"));
        assertTrue(lines.get(1).contains("EV Charging"));
        assertTrue(lines.get(2).contains("Car Wash"));

        bill.markPaid();
        assertEquals(BillStatus.PAID, bill.getStatus());

        // Marking paid twice must throw IllegalStateException
        assertThrows(IllegalStateException.class, bill::markPaid);
    }

    @Test
    @DisplayName("Bill should track audit history of failed and successful payment transactions")
    void testBillPaymentHistoryAuditTrail() {
        Ticket ticket = new Ticket("S2", 1, new Vehicle("DL-01-1111", VehicleType.CAR), Instant.now());
        Bill bill = new Bill(ticket, ticket.getEntryTime().plusSeconds(3600), 100.0);

        // 1. Record failed transaction
        PaymentTransaction failedTxn = new PaymentTransaction(bill.getBillId(), bill.getTotalAmount(), PaymentMode.RAZORPAY_UPI);
        failedTxn.markFailed("NETWORK_TIMEOUT");
        bill.recordPayment(failedTxn);

        assertEquals(BillStatus.UNPAID, bill.getStatus());
        assertEquals(1, bill.getPaymentHistory().size());
        assertFalse(bill.getSuccessfulPayment().isPresent());

        // 2. Retry with Cash and succeed
        PaymentTransaction successTxn = new PaymentTransaction(bill.getBillId(), bill.getTotalAmount(), PaymentMode.CASH);
        successTxn.markSuccess("CASH-SETTLED-001");
        bill.recordPayment(successTxn);

        assertEquals(BillStatus.PAID, bill.getStatus());
        assertEquals(2, bill.getPaymentHistory().size());
        assertTrue(bill.getSuccessfulPayment().isPresent());
        assertEquals("CASH-SETTLED-001", bill.getSuccessfulPayment().get().getTransactionReference());
    }
}
