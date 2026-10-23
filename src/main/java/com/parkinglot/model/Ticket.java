package com.parkinglot.model;

import com.parkinglot.enums.TicketStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Ticket {
    private final String ticketNumber;
    private final String spotId;
    private final int floorNumber;
    private final Vehicle vehicle;
    private final Instant entryTime;
    private volatile TicketStatus status;

    public Ticket(ParkingSpot spot, Vehicle vehicle, Instant entryTime) {
        Objects.requireNonNull(spot, "Spot cannot be null");
        Objects.requireNonNull(vehicle, "Vehicle cannot be null");
        Objects.requireNonNull(entryTime, "Entry time cannot be null");

        this.ticketNumber = UUID.randomUUID().toString();
        this.spotId = spot.getSpotId();
        this.floorNumber = spot.getFloorNumber();
        this.vehicle = vehicle;
        this.entryTime = entryTime;
        this.status = TicketStatus.ACTIVE;
    }

    public synchronized void markPaid() {
        if (this.status == TicketStatus.PAID) {
            throw new IllegalStateException("Ticket is already paid");
        }
        this.status = TicketStatus.PAID;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public String getSpotId() {
        return spotId;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public TicketStatus getStatus() {
        return status;
    }
}
