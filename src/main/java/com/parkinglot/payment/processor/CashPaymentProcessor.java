package com.parkinglot.payment.processor;

import com.parkinglot.enums.PaymentMode;
import com.parkinglot.model.User;
import com.parkinglot.payment.PaymentProcessor;
import com.parkinglot.payment.PaymentTransaction;
import java.util.UUID;

/**
 * Offline physical cash settlement processor with operator accountability.
 */
public class CashPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentTransaction processPayment(String billId, double amountInRupees, PaymentMode mode, User operator) {
        PaymentTransaction txn = new PaymentTransaction(billId, amountInRupees, mode, operator);
        if (amountInRupees >= 0) {
            txn.markSuccess("CASH-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } else {
            txn.markFailed("Invalid negative cash amount: " + amountInRupees);
        }
        return txn;
    }

    @Override
    public PaymentTransaction processPayment(String billId, double amountInRupees, PaymentMode mode) {
        return processPayment(billId, amountInRupees, mode, User.SYSTEM_KIOSK);
    }
}
