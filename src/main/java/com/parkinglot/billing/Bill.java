package com.parkinglot.billing;

import com.parkinglot.billing.decorator.AmenityServiceDecorator;
import com.parkinglot.billing.decorator.BaseParkingCharge;
import com.parkinglot.billing.decorator.BillableItem;
import com.parkinglot.billing.service.AmenityService;
import com.parkinglot.enums.BillStatus;
import com.parkinglot.model.Ticket;
import com.parkinglot.payment.PaymentStatus;
import com.parkinglot.payment.PaymentTransaction;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Transactional settlement invoice.
 * Incorporates the Decorator Pattern for itemized charge calculation,
 * maintains an audit trail of payment transactions, and supports persistence.
 */
public class Bill {
    private final String billId;
    private final Ticket ticket;
    private final Instant exitTime;
    private final double baseParkingFee;
    private final List<AmenityService> services;
    private final BillableItem billableItem;
    private final double totalAmount;
    private final List<PaymentTransaction> paymentHistory;
    private volatile BillStatus status;

    public Bill(Ticket ticket, Instant exitTime, BillableItem billableItem) {
        this.billId = "BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.ticket = Objects.requireNonNull(ticket, "Ticket cannot be null");
        this.exitTime = Objects.requireNonNull(exitTime, "Exit time cannot be null");
        this.billableItem = Objects.requireNonNull(billableItem, "Billable item cannot be null");
        this.services = List.copyOf(ticket.getConsumedServices());
        this.paymentHistory = new CopyOnWriteArrayList<>();

        // Extract base parking fee from root or calculate directly
        double servicesTotal = this.services.stream()
                .mapToDouble(AmenityService::calculateCost)
                .sum();
        this.totalAmount = Math.round(billableItem.getCost() * 100.0) / 100.0;
        this.baseParkingFee = Math.max(0.0, Math.round((this.totalAmount - servicesTotal) * 100.0) / 100.0);
        this.status = BillStatus.UNPAID;
    }

    public Bill(Ticket ticket, Instant exitTime, double baseParkingFee) {
        this(ticket, exitTime, buildDecoratedItem(ticket, baseParkingFee));
    }

    private static BillableItem buildDecoratedItem(Ticket ticket, double baseParkingFee) {
        BillableItem item = new BaseParkingCharge(baseParkingFee);
        for (AmenityService service : ticket.getConsumedServices()) {
            item = new AmenityServiceDecorator(item, service);
        }
        return item;
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

    public BillableItem getBillableItem() {
        return billableItem;
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
        return billableItem.getItemizedLines();
    }
}
