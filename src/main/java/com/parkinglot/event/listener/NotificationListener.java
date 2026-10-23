package com.parkinglot.event.listener;

import com.parkinglot.event.ParkingEvent;
import com.parkinglot.event.ParkingEventListener;
import com.parkinglot.event.VehicleExitedEvent;
import com.parkinglot.event.VehicleParkedEvent;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Concrete Observer: Dispatches SMS or push notifications to vehicle drivers upon entry and exit.
 */
public class NotificationListener implements ParkingEventListener {
    private final List<String> dispatchedMessages = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(ParkingEvent event) {
        if (event instanceof VehicleParkedEvent parked) {
            String phone = parked.vehicle().ownerPhone();
            String recipient = phone != null ? phone : "Driver";
            String msg = String.format("SMS to %s: Vehicle %s parked at spot %s on Floor %d (Ticket: %s)",
                    recipient, parked.vehicle().licensePlate(), parked.spotId(), parked.floorNumber(), parked.ticketNumber());
            dispatchedMessages.add(msg);
        } else if (event instanceof VehicleExitedEvent exited) {
            String msg = String.format("SMS to Driver: Exit confirmed for Ticket %s. Bill %s of ₹%.2f settled.",
                    exited.ticketNumber(), exited.billId(), exited.amount());
            dispatchedMessages.add(msg);
        }
    }

    public List<String> getDispatchedMessages() {
        return Collections.unmodifiableList(dispatchedMessages);
    }
}
