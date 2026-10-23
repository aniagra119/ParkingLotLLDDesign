package com.parkinglot.payment.external;

/**
 * Simulates a vendor proprietary SDK (e.g. com.razorpay.RazorpayClient)
 * with its own distinct method signatures, payload formats (paise), and status codes.
 */
public class ExternalRazorpayClient {
    private final String keyId;
    private final String keySecret;

    public ExternalRazorpayClient(String keyId, String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public String captureTransaction(String orderReference, long amountInPaise, String currency) {
        if (amountInPaise <= 0) {
            return "FAILED_INVALID_AMOUNT";
        }
        // Simulates network call to Razorpay REST API
        return "SUCCESS_TXN_" + System.currentTimeMillis();
    }

    public String getKeyId() {
        return keyId;
    }
}
