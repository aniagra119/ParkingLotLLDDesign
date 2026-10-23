package com.parkinglot.payment;

import com.parkinglot.enums.PaymentMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an individual payment attempt and audit record.
 * Captures external gateway transaction IDs, timestamps, failure reasons, and status.
 */
public class PaymentTransaction {
    private final String paymentId;
    private final String billId;
    private final double amount;
    private final PaymentMode paymentMode;
    private final Instant attemptedAt;
    private volatile PaymentStatus status;
    private volatile String transactionReference;
    private volatile String failureReason;

    public PaymentTransaction(String billId, double amount, PaymentMode paymentMode) {
        if (amount < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative");
        }
        this.paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.billId = Objects.requireNonNull(billId, "Bill ID cannot be null");
        this.amount = amount;
        this.paymentMode = Objects.requireNonNull(paymentMode, "Payment mode cannot be null");
        this.attemptedAt = Instant.now();
        this.status = PaymentStatus.PENDING;
    }

    public synchronized void markSuccess(String transactionReference) {
        this.status = PaymentStatus.SUCCESS;
        this.transactionReference = Objects.requireNonNull(transactionReference, "Transaction reference cannot be null");
    }

    public synchronized void markFailed(String failureReason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = Objects.requireNonNull(failureReason, "Failure reason cannot be null");
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getBillId() {
        return billId;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
