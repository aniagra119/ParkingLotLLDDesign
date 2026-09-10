package com.parkinglot.payment;

/**
 * Domain interface for payment execution.
 * Completely decoupled from specific vendor SDKs, protocols, or wire formats.
 */
public interface PaymentProcessor {
    boolean processPayment(String billId, double amountInRupees);
}
