package com.parkinglot.payment.adapter;

import com.parkinglot.payment.PaymentProcessor;
import com.parkinglot.payment.external.ExternalRazorpayClient;
import java.util.Objects;

/**
 * Adapter Pattern: Bridges proprietary ExternalRazorpayClient to domain PaymentProcessor.
 * Converts domain currency (Rupees) into vendor unit (Paise).
 */
public class RazorpayPaymentAdapter implements PaymentProcessor {
    private final ExternalRazorpayClient razorpayClient;

    public RazorpayPaymentAdapter(ExternalRazorpayClient razorpayClient) {
        this.razorpayClient = Objects.requireNonNull(razorpayClient, "Razorpay client cannot be null");
    }

    @Override
    public boolean processPayment(String billId, double amountInRupees) {
        if (amountInRupees <= 0) {
            return false;
        }
        // Convert domain Rupees to Razorpay's required unit: Paise
        long amountInPaise = Math.round(amountInRupees * 100);
        String response = razorpayClient.captureTransaction(billId, amountInPaise, "INR");
        return response != null && response.startsWith("SUCCESS");
    }

    public ExternalRazorpayClient getRazorpayClient() {
        return razorpayClient;
    }
}
