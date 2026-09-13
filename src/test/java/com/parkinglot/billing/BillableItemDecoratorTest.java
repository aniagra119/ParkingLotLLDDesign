package com.parkinglot.billing;

import com.parkinglot.billing.decorator.AmenityServiceDecorator;
import com.parkinglot.billing.decorator.BaseParkingCharge;
import com.parkinglot.billing.decorator.BillableItem;
import com.parkinglot.billing.service.CarWashService;
import com.parkinglot.billing.service.EvChargingService;
import com.parkinglot.enums.VehicleType;
import com.parkinglot.enums.WashPackage;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Decorator Pattern & BillableItem Tests")
class BillableItemDecoratorTest {

    @Test
    @DisplayName("BaseParkingCharge provides root fee and itemized line")
    void testBaseParkingCharge() {
        BillableItem base = new BaseParkingCharge(100.0);
        assertEquals(100.0, base.getCost());
        assertEquals(List.of("Base Parking Fee: ₹100.0"), base.getItemizedLines());
    }

    @Test
    @DisplayName("AmenityServiceDecorator dynamically decorates BaseParkingCharge with EV charging and Car wash")
    void testDecoratorChaining() {
        BillableItem root = new BaseParkingCharge(80.0);

        EvChargingService evService = new EvChargingService(15.0); // 15 kWh * 12.0 = 180.0
        BillableItem evDecorated = new AmenityServiceDecorator(root, evService);

        assertEquals(260.0, evDecorated.getCost());
        assertEquals(2, evDecorated.getItemizedLines().size());
        assertTrue(evDecorated.getItemizedLines().get(1).contains("EV Charging"));

        CarWashService washService = new CarWashService(VehicleType.CAR, WashPackage.COMPLETE_DETAILING); // 500.0 + 50.0 = 550.0
        BillableItem fullyDecorated = new AmenityServiceDecorator(evDecorated, washService);

        assertEquals(810.0, fullyDecorated.getCost());
        assertEquals(3, fullyDecorated.getItemizedLines().size());
        assertTrue(fullyDecorated.getItemizedLines().get(2).contains("Car Wash (CAR - COMPLETE_DETAILING)"));
    }

    @Test
    @DisplayName("Bill encapsulates decorated BillableItem and transparently exposes total and breakdown")
    void testBillWithBillableItem() {
        Vehicle vehicle = new Vehicle("KA-01-EV-9999", VehicleType.CAR);
        Ticket ticket = new Ticket("S1", 1, vehicle, Instant.now());
        ticket.addService(new EvChargingService(10.0)); // 120.0

        BillableItem item = new BaseParkingCharge(50.0);
        item = new AmenityServiceDecorator(item, ticket.getConsumedServices().get(0));

        Bill bill = new Bill(ticket, Instant.now(), item);

        assertEquals(170.0, bill.getTotalAmount());
        assertEquals(50.0, bill.getBaseParkingFee());
        assertEquals(2, bill.getItemizedBreakdown().size());
        assertNotNull(bill.getBillableItem());
    }
}
