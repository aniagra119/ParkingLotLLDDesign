package com.parkinglot.payment;

import com.parkinglot.billing.Bill;
import com.parkinglot.enums.PaymentMode;
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

    public PaymentTransaction processBillPayment(Bill bill, PaymentMode mode) {
        Objects.requireNonNull(bill, "Bill cannot be null");
        Objects.requireNonNull(mode, "Payment mode cannot be null");

        PaymentProcessor processor = paymentProcessorFactory.getPaymentProcessor(mode);
        PaymentTransaction txn = processor.processPayment(bill.getBillId(), bill.getTotalAmount(), mode);
        bill.recordPayment(txn);
        return txn;
    }

    public PaymentProcessorFactory getPaymentProcessorFactory() {
        return paymentProcessorFactory;
    }
}
