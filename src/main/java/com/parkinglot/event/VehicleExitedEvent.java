package com.parkinglot.event;

import java.time.Instant;

public record VehicleExitedEvent(
        String ticketNumber,
        String billId,
        double amount,
        String spotId,
        int floorNumber,
        Instant timestamp
) implements ParkingEvent {
    public VehicleExitedEvent {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return "VEHICLE_EXITED";
    }
}
