package com.parkinglot.billing;

import com.parkinglot.billing.service.AmenityService;
import com.parkinglot.enums.BillStatus;
import com.parkinglot.model.Ticket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Transactional settlement invoice.
 * Clean, flat data entity designed for database persistence, JSON serialization,
 * and transparent itemized breakdown without recursive Decorator wrapper overhead.
 */
public class Bill {
    private final String billId;
    private final Ticket ticket;
    private final Instant exitTime;
    private final double baseParkingFee;
    private final List<AmenityService> services;
    private final double totalAmount;
    private volatile BillStatus status;

    public Bill(Ticket ticket, Instant exitTime, double baseParkingFee) {
        if (baseParkingFee < 0) {
            throw new IllegalArgumentException("Base parking fee cannot be negative");
        }
        this.billId = "BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.ticket = Objects.requireNonNull(ticket, "Ticket cannot be null");
        this.exitTime = Objects.requireNonNull(exitTime, "Exit time cannot be null");
        this.baseParkingFee = baseParkingFee;
        this.services = List.copyOf(ticket.getConsumedServices());

        // Simple, clean stream summation without recursive decorator nesting
        double servicesTotal = this.services.stream()
                .mapToDouble(AmenityService::calculateCost)
                .sum();
        this.totalAmount = Math.round((baseParkingFee + servicesTotal) * 100.0) / 100.0;
        this.status = BillStatus.UNPAID;
    }

    public synchronized void markPaid() {
        if (this.status == BillStatus.PAID) {
            throw new IllegalStateException("Bill is already paid: " + billId);
        }
        this.status = BillStatus.PAID;
        this.ticket.markPaid();
    }

    public String getBillId() {
        return billId;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public Instant getExitTime() {
        return exitTime;
    }

    public double getBaseParkingFee() {
        return baseParkingFee;
    }

    public List<AmenityService> getServices() {
        return Collections.unmodifiableList(services);
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public BillStatus getStatus() {
        return status;
    }

    public List<String> getItemizedBreakdown() {
        List<String> lines = new ArrayList<>();
        lines.add("Base Parking Fee: ₹" + baseParkingFee);
        for (AmenityService service : services) {
            lines.add(service.getItemizedDescription());
        }
        return lines;
    }
}
