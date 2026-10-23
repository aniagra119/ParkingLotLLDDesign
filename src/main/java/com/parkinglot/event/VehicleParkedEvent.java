package com.parkinglot.event;

import com.parkinglot.model.Vehicle;
import java.time.Instant;

public record VehicleParkedEvent(
        String ticketNumber,
        Vehicle vehicle,
        String spotId,
        int floorNumber,
        Instant timestamp
) implements ParkingEvent {
    public VehicleParkedEvent {
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
        return "VEHICLE_PARKED";
    }
}
