package com.parkinglot.payment;

import com.parkinglot.enums.PaymentMode;
import com.parkinglot.model.User;

/**
 * Domain interface (Port) for payment execution.
 * Completely decoupled from specific vendor SDKs, wire formats, or external APIs.
 */
public interface PaymentProcessor {

    PaymentTransaction processPayment(String billId, double amountInRupees, PaymentMode mode, User operator);

    default PaymentTransaction processPayment(String billId, double amountInRupees, PaymentMode mode) {
        return processPayment(billId, amountInRupees, mode, User.SYSTEM_KIOSK);
    }
}
