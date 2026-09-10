package com.parkinglot.payment;

import com.parkinglot.enums.PaymentMode;
import java.util.Objects;

/**
 * Simple Factory Pattern: Centralizes instantiation of the correct
 * PaymentProcessor based on the customer's chosen PaymentMode.
 */
public class PaymentProcessorFactory {
    private final ExternalRazorpayClient sharedRazorpayClient;

    public PaymentProcessorFactory(ExternalRazorpayClient razorpayClient) {
        this.sharedRazorpayClient = Objects.requireNonNull(razorpayClient, "Razorpay client cannot be null");
    }

    public PaymentProcessor getPaymentProcessor(PaymentMode mode) {
        Objects.requireNonNull(mode, "Payment mode cannot be null");
        return switch (mode) {
            case RAZORPAY_UPI, RAZORPAY_CARD -> new RazorpayPaymentAdapter(sharedRazorpayClient);
            case CASH -> new CashPaymentProcessor();
        };
    }
}
