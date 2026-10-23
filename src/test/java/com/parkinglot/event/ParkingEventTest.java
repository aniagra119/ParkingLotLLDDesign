package com.parkinglot.event;

import com.parkinglot.ParkingLot;
import com.parkinglot.builder.ParkingLotBuilder;
import com.parkinglot.enums.GateType;
import com.parkinglot.enums.PaymentMode;
import com.parkinglot.enums.VehicleType;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParkingEventTest {

    @Test
    @DisplayName("ParkingEventPublisher should manage subscribers and broadcast events safely")
    void testEventPublisher() {
        ParkingEventPublisher publisher = new ParkingEventPublisher();
        assertEquals(0, publisher.getSubscriberCount());

        List<ParkingEvent> receivedEvents = new ArrayList<>();
        ParkingEventListener listener = receivedEvents::add;

        publisher.subscribe(listener);
        assertEquals(1, publisher.getSubscriberCount());

        VehicleParkedEvent parkedEvent = new VehicleParkedEvent("T-1", new Vehicle("KA-01", VehicleType.CAR), "S1", 1, Instant.now());
        publisher.publish(parkedEvent);

        assertEquals(1, receivedEvents.size());
        assertEquals("VEHICLE_PARKED", receivedEvents.get(0).getEventType());

        publisher.unsubscribe(listener);
        assertEquals(0, publisher.getSubscriberCount());

        publisher.publish(parkedEvent);
        assertEquals(1, receivedEvents.size()); // No new events received
    }

    @Test
    @DisplayName("ParkingLot should automatically trigger VehicleParkedEvent and VehicleExitedEvent through observer pattern")
    void testParkingLotEventBroadcasting() {
        List<ParkingEvent> capturedEvents = new ArrayList<>();

        ParkingLot lot = new ParkingLotBuilder()
                .withLotId("LOT-EVENTS")
                .addGate("ENTRY-1", GateType.ENTRY)
                .addGate("EXIT-1", GateType.EXIT)
                .addFloor("F1", 1, 1, 1, 1)
                .withEventListener(capturedEvents::add)
                .build();

        Vehicle car = new Vehicle("KA-02-EVT", VehicleType.CAR);
        Ticket ticket = lot.parkVehicle("ENTRY-1", car);

        assertEquals(1, capturedEvents.size());
        assertInstanceOf(VehicleParkedEvent.class, capturedEvents.get(0));
        VehicleParkedEvent parked = (VehicleParkedEvent) capturedEvents.get(0);
        assertEquals(ticket.getTicketNumber(), parked.ticketNumber());
        assertEquals("KA-02-EVT", parked.vehicle().licensePlate());

        lot.processExit("EXIT-1", ticket.getTicketNumber(), Instant.now().plusSeconds(60), PaymentMode.CASH);

        assertEquals(2, capturedEvents.size());
        assertInstanceOf(VehicleExitedEvent.class, capturedEvents.get(1));
        VehicleExitedEvent exited = (VehicleExitedEvent) capturedEvents.get(1);
        assertEquals(ticket.getTicketNumber(), exited.ticketNumber());
        assertEquals("F1-C1", exited.spotId());
    }
}
