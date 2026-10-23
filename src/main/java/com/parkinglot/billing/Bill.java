package com.parkinglot.billing;

import com.parkinglot.enums.BillStatus;
import com.parkinglot.model.Ticket;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Bill {
    private final String billId;
    private final Ticket ticket;
    private final Instant exitTime;
    private final BillableItem billableItem;
    private final double totalAmount;
    private volatile BillStatus status;

    public Bill(Ticket ticket, Instant exitTime, BillableItem billableItem) {
        this.billId = "BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.ticket = Objects.requireNonNull(ticket, "Ticket cannot be null");
        this.exitTime = Objects.requireNonNull(exitTime, "Exit time cannot be null");
        this.billableItem = Objects.requireNonNull(billableItem, "Billable item cannot be null");
        this.totalAmount = billableItem.getCost();
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

    public BillableItem getBillableItem() {
        return billableItem;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public BillStatus getStatus() {
        return status;
    }

    public List<String> getItemizedBreakdown() {
        return billableItem.getItemizedLines();
    }
}
