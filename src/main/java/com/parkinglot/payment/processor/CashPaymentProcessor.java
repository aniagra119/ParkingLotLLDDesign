package com.parkinglot.payment.processor;

import com.parkinglot.payment.PaymentProcessor;

/**
 * Offline physical cash settlement processor.
 */
public class CashPaymentProcessor implements PaymentProcessor {
    @Override
    public boolean processPayment(String billId, double amountInRupees) {
        return amountInRupees >= 0;
    }
}
