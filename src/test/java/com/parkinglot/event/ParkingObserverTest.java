package com.parkinglot.event;

import com.parkinglot.event.listener.DisplayBoardListener;
import com.parkinglot.event.listener.NotificationListener;
import com.parkinglot.enums.VehicleType;
import com.parkinglot.model.Vehicle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Observer Pattern - Real-time Display and Notification Listeners")
class ParkingObserverTest {

    @Test
    @DisplayName("DisplayBoardListener tracks total and per-floor occupancy dynamically")
    void testDisplayBoardListener() {
        DisplayBoardListener displayBoard = new DisplayBoardListener();
        ParkingEventPublisher publisher = new ParkingEventPublisher();
        publisher.subscribe(displayBoard);

        assertEquals(0, displayBoard.getTotalOccupied());

        Vehicle v1 = new Vehicle("KA-01-A-1", VehicleType.CAR);
        VehicleParkedEvent e1 = new VehicleParkedEvent("TCK-1", v1, "1-CAR-1", 1, Instant.now());
        publisher.publish(e1);

        assertEquals(1, displayBoard.getTotalOccupied());
        assertEquals(1, displayBoard.getOccupiedOnFloor(1));
        assertEquals(0, displayBoard.getOccupiedOnFloor(2));

        Vehicle v2 = new Vehicle("KA-01-B-2", VehicleType.BIKE);
        VehicleParkedEvent e2 = new VehicleParkedEvent("TCK-2", v2, "2-BIKE-1", 2, Instant.now());
        publisher.publish(e2);

        assertEquals(2, displayBoard.getTotalOccupied());
        assertEquals(1, displayBoard.getOccupiedOnFloor(2));

        VehicleExitedEvent exit1 = new VehicleExitedEvent("TCK-1", "BILL-1", 100.0, "1-CAR-1", 1, Instant.now());
        publisher.publish(exit1);

        assertEquals(1, displayBoard.getTotalOccupied());
        assertEquals(0, displayBoard.getOccupiedOnFloor(1));
        assertEquals(1, displayBoard.getOccupiedOnFloor(2));
    }

    @Test
    @DisplayName("NotificationListener sends SMS alerts to vehicle driver on park and exit")
    void testNotificationListener() {
        NotificationListener notifier = new NotificationListener();
        ParkingEventPublisher publisher = new ParkingEventPublisher();
        publisher.subscribe(notifier);

        Vehicle vehicle = Vehicle.builder("KA-01-XYZ-1234", VehicleType.CAR)
                .withOwnerPhone("+91-9999888877")
                .build();

        VehicleParkedEvent parkEvent = new VehicleParkedEvent("TCK-99", vehicle, "1-CAR-5", 1, Instant.now());
        publisher.publish(parkEvent);

        assertEquals(1, notifier.getDispatchedMessages().size());
        String parkMsg = notifier.getDispatchedMessages().get(0);
        assertTrue(parkMsg.contains("+91-9999888877"));
        assertTrue(parkMsg.contains("KA-01-XYZ-1234"));
        assertTrue(parkMsg.contains("1-CAR-5"));

        VehicleExitedEvent exitEvent = new VehicleExitedEvent("TCK-99", "BILL-99", 150.0, "1-CAR-5", 1, Instant.now());
        publisher.publish(exitEvent);

        assertEquals(2, notifier.getDispatchedMessages().size());
        String exitMsg = notifier.getDispatchedMessages().get(1);
        assertTrue(exitMsg.contains("TCK-99"));
        assertTrue(exitMsg.contains("BILL-99"));
        assertTrue(exitMsg.contains("150.00"));
    }
}
