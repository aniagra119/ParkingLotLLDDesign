package com.parkinglot.payment;

import com.parkinglot.billing.Bill;
import com.parkinglot.enums.PaymentMode;
import com.parkinglot.model.User;
import java.util.Objects;

/**
 * Coordinates payment execution via factory-selected adapters and records audit transactions on the Bill.
 * Single Responsibility: Payment processing orchestration and invoice settlement.
 */
public class PaymentService {
    private final PaymentProcessorFactory paymentProcessorFactory;

    public PaymentService(PaymentProcessorFactory paymentProcessorFactory) {
        this.paymentProcessorFactory = Objects.requireNonNull(paymentProcessorFactory, "Factory cannot be null");
    }

    public PaymentTransaction processBillPayment(Bill bill, PaymentMode mode, User operator) {
        Objects.requireNonNull(bill, "Bill cannot be null");
        Objects.requireNonNull(mode, "Payment mode cannot be null");
        User actor = operator != null ? operator : User.SYSTEM_KIOSK;

        PaymentProcessor processor = paymentProcessorFactory.getPaymentProcessor(mode);
        PaymentTransaction txn = processor.processPayment(bill.getBillId(), bill.getTotalAmount(), mode, actor);
        bill.recordPayment(txn);
        return txn;
    }

    public PaymentTransaction processBillPayment(Bill bill, PaymentMode mode) {
        return processBillPayment(bill, mode, User.SYSTEM_KIOSK);
    }

    public PaymentProcessorFactory getPaymentProcessorFactory() {
        return paymentProcessorFactory;
    }
}
