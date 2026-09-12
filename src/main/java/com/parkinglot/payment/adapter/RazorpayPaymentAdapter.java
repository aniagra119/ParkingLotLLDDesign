package com.parkinglot.payment.adapter;

import com.parkinglot.enums.PaymentMode;
import com.parkinglot.payment.PaymentProcessor;
import com.parkinglot.payment.PaymentTransaction;
import com.parkinglot.payment.external.ExternalRazorpayClient;
import java.util.Objects;

/**
 * Adapter Pattern: Bridges proprietary ExternalRazorpayClient to domain PaymentProcessor.
 * Converts domain currency (Rupees) into vendor unit (Paise) and records transaction metadata.
 */
public class RazorpayPaymentAdapter implements PaymentProcessor {
    private final ExternalRazorpayClient razorpayClient;

    public RazorpayPaymentAdapter(ExternalRazorpayClient razorpayClient) {
        this.razorpayClient = Objects.requireNonNull(razorpayClient, "Razorpay client cannot be null");
    }

    @Override
    public PaymentTransaction processPayment(String billId, double amountInRupees, PaymentMode mode) {
        PaymentTransaction txn = new PaymentTransaction(billId, amountInRupees, mode);
        if (amountInRupees <= 0) {
            txn.markFailed("Amount must be greater than zero");
            return txn;
        }

        // Convert domain Rupees to Razorpay's required unit: Paise
        long amountInPaise = Math.round(amountInRupees * 100);
        String response = razorpayClient.captureTransaction(billId, amountInPaise, "INR");

        if (response != null && response.startsWith("SUCCESS")) {
            txn.markSuccess(response);
        } else {
            txn.markFailed(response != null ? response : "Unknown gateway failure");
        }
        return txn;
    }

    public ExternalRazorpayClient getRazorpayClient() {
        return razorpayClient;
    }
}
