package com.parkinglot.payment;

import com.parkinglot.enums.PaymentMode;
import com.parkinglot.payment.adapter.RazorpayPaymentAdapter;
import com.parkinglot.payment.external.ExternalRazorpayClient;
import com.parkinglot.payment.processor.CashPaymentProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProcessorTest {

    @Test
    @DisplayName("RazorpayPaymentAdapter should successfully convert Rupees to Paise and process payment")
    void testRazorpayAdapterSuccess() {
        ExternalRazorpayClient client = new ExternalRazorpayClient("key_123", "secret_456");
        RazorpayPaymentAdapter adapter = new RazorpayPaymentAdapter(client);

        // ₹180.50 -> 18050 paise
        boolean result = adapter.processPayment("BILL-001", 180.50);
        assertTrue(result);
    }

    @Test
    @DisplayName("RazorpayPaymentAdapter should return false for zero or negative amounts")
    void testRazorpayAdapterInvalidAmount() {
        ExternalRazorpayClient client = new ExternalRazorpayClient("key_123", "secret_456");
        RazorpayPaymentAdapter adapter = new RazorpayPaymentAdapter(client);

        assertFalse(adapter.processPayment("BILL-002", 0.0));
        assertFalse(adapter.processPayment("BILL-003", -50.0));
    }

    @Test
    @DisplayName("CashPaymentProcessor should accept valid non-negative cash amounts")
    void testCashPaymentProcessor() {
        CashPaymentProcessor processor = new CashPaymentProcessor();
        assertTrue(processor.processPayment("BILL-100", 250.0));
        assertTrue(processor.processPayment("BILL-101", 0.0));
        assertFalse(processor.processPayment("BILL-102", -10.0));
    }

    @Test
    @DisplayName("PaymentProcessorFactory should instantiate the correct adapter or processor based on PaymentMode")
    void testPaymentProcessorFactory() {
        ExternalRazorpayClient client = new ExternalRazorpayClient("key_123", "secret_456");
        PaymentProcessorFactory factory = new PaymentProcessorFactory(client);

        PaymentProcessor upiProcessor = factory.getPaymentProcessor(PaymentMode.RAZORPAY_UPI);
        assertInstanceOf(RazorpayPaymentAdapter.class, upiProcessor);

        PaymentProcessor cardProcessor = factory.getPaymentProcessor(PaymentMode.RAZORPAY_CARD);
        assertInstanceOf(RazorpayPaymentAdapter.class, cardProcessor);

        PaymentProcessor cashProcessor = factory.getPaymentProcessor(PaymentMode.CASH);
        assertInstanceOf(CashPaymentProcessor.class, cashProcessor);
    }
}
