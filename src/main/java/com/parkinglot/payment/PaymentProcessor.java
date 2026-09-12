package com.parkinglot.payment;

import com.parkinglot.enums.PaymentMode;

/**
 * Domain interface (Port) for payment execution.
 * Completely decoupled from specific vendor SDKs, wire formats, or external APIs.
 */
public interface PaymentProcessor {
    PaymentTransaction processPayment(String billId, double amountInRupees, PaymentMode mode);
}
