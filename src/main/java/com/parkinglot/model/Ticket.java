package com.parkinglot.model;

import com.parkinglot.enums.TicketStatus;
import java.time.Instant;

public class Ticket {
    private final String ticketNumber;
    private final String spotId;
    private final Vehicle vehicle;
    private final Instant entryTime;
    private volatile TicketStatus status;

    public Ticket(String spotId, Vehicle vehicle, Instant entryTime) {
        // TODO Phase 1: Initialize fields, generate ticketNumber (UUID), set status to ACTIVE
        this.ticketNumber = null;
        this.spotId = spotId;
        this.vehicle = vehicle;
        this.entryTime = entryTime;
        this.status = TicketStatus.ACTIVE;
    }

    public synchronized void markPaid() {
        this.status = TicketStatus.PAID;
    }

    public String getTicketNumber() { return ticketNumber; }
    public String getSpotId() { return spotId; }
    public Vehicle getVehicle() { return vehicle; }
    public Instant getEntryTime() { return entryTime; }
    public TicketStatus getStatus() { return status; }
}
