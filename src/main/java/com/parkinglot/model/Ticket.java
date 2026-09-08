package com.parkinglot.model;

import com.parkinglot.enums.TicketStatus;
import java.time.Instant;
import java.util.UUID;

public class Ticket {
    private final String ticketNumber;
    private final String spotId;
    private final String floorNumber;
    private final Vehicle vehicle;
    private final Instant entryTime;
    private volatile TicketStatus status;

    public Ticket(String spotId, String floorNumber, Vehicle vehicle, Instant entryTime) {
        this.ticketNumber = UUID.randomUUID().toString();
        if (spotId == null || spotId.isBlank()) {
            throw new IllegalArgumentException("Spot ID cannot be null or empty");
        }
        if (vehicle == null) {
            throw new IllegalArgumentException("Vehicle cannot be null");
        }
        if (entryTime == null) {
            throw new IllegalArgumentException("Entry time cannot be null");
        }
        this.spotId = spotId;
        this.floorNumber = floorNumber;
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

    public Vehicle getVehicle() {
        return vehicle;
    }

    public Instant getEntryTime() {
        return entryTime;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getFloorNumber() {
        return floorNumber;
    }
}
