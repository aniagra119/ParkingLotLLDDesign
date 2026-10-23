package com.parkinglot.billing;

import com.parkinglot.billing.service.AmenityService;
import com.parkinglot.enums.BillStatus;
import com.parkinglot.model.Ticket;
import com.parkinglot.payment.PaymentStatus;
import com.parkinglot.payment.PaymentTransaction;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Transactional settlement invoice.
 * Clean, flat data entity designed for database persistence, JSON serialization,
 * audit trail of payment transactions, and transparent itemized breakdown.
 */
public class Bill {
    private final String billId;
    private final Ticket ticket;
    private final Instant exitTime;
    private final double baseParkingFee;
    private final List<AmenityService> services;
    private final double totalAmount;
    private final List<PaymentTransaction> paymentHistory;
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
        this.paymentHistory = new CopyOnWriteArrayList<>();

        // Linear summation of base fee and polymorphic services
        double servicesTotal = this.services.stream()
                .mapToDouble(AmenityService::calculateCost)
                .sum();
        this.totalAmount = Math.round((baseParkingFee + servicesTotal) * 100.0) / 100.0;
        this.status = BillStatus.UNPAID;
    }

    /**
     * Records a payment attempt in the audit history. If the payment succeeded,
     * transitions the bill and underlying ticket to PAID.
     */
    public synchronized void recordPayment(PaymentTransaction transaction) {
        Objects.requireNonNull(transaction, "Payment transaction cannot be null");
        if (this.status == BillStatus.PAID) {
            throw new IllegalStateException("Bill is already paid: " + billId);
        }
        this.paymentHistory.add(transaction);
        if (transaction.getStatus() == PaymentStatus.SUCCESS) {
            this.status = BillStatus.PAID;
            this.ticket.markPaid();
        }
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

    public List<PaymentTransaction> getPaymentHistory() {
        return Collections.unmodifiableList(paymentHistory);
    }

    public Optional<PaymentTransaction> getSuccessfulPayment() {
        return paymentHistory.stream()
                .filter(t -> t.getStatus() == PaymentStatus.SUCCESS)
                .findFirst();
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
