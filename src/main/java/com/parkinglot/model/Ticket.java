package com.parkinglot.model;

import com.parkinglot.billing.service.AmenityService;
import com.parkinglot.enums.TicketStatus;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class Ticket {
    private final String ticketNumber;
    private final String spotId;
    private final int floorNumber;
    private final Vehicle vehicle;
    private final Instant entryTime;
    private final List<AmenityService> consumedServices;
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
        this.consumedServices = new CopyOnWriteArrayList<>();
        this.status = TicketStatus.ACTIVE;
    }

    public void addService(AmenityService service) {
        Objects.requireNonNull(service, "Amenity service cannot be null");
        if (this.status != TicketStatus.ACTIVE) {
            throw new IllegalStateException("Cannot add services to inactive ticket: " + ticketNumber);
        }
        consumedServices.add(service);
    }

    public List<AmenityService> getConsumedServices() {
        return Collections.unmodifiableList(consumedServices);
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
