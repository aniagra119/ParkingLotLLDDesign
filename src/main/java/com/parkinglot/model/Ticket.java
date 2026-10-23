package com.parkinglot.model;

import com.parkinglot.enums.AmenityServiceType;
import com.parkinglot.enums.TicketStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Ticket {
    private final String ticketNumber;
    private final String spotId;
    private final int floorNumber;
    private final Vehicle vehicle;
    private final Instant entryTime;
    private final Set<AmenityServiceType> consumedServices;
    private volatile TicketStatus status;

    public Ticket(String spotId, int floorNumber, Vehicle vehicle, Instant entryTime) {
        if (spotId == null || spotId.isBlank()) {
            throw new IllegalArgumentException("Spot ID cannot be null or empty");
        }
        Objects.requireNonNull(vehicle, "Vehicle cannot be null");
        Objects.requireNonNull(entryTime, "Entry time cannot be null");

        this.ticketNumber = UUID.randomUUID().toString();
        this.spotId = spotId;
        this.floorNumber = floorNumber;
        this.vehicle = vehicle;
        this.entryTime = entryTime;
        this.consumedServices = ConcurrentHashMap.newKeySet();
        this.status = TicketStatus.ACTIVE;
    }

    public void addService(AmenityServiceType service) {
        Objects.requireNonNull(service, "Service cannot be null");
        if (this.status != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Cannot add services to inactive ticket: " + ticketNumber);
        }
        consumedServices.add(service);
    }

    public Set<AmenityServiceType> getConsumedServices() {
        return Collections.unmodifiableSet(consumedServices);
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
