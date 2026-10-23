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
    @DisplayName("RazorpayPaymentAdapter should successfully convert Rupees to Paise and record successful transaction")
    void testRazorpayAdapterSuccess() {
        ExternalRazorpayClient client = new ExternalRazorpayClient("key_123", "secret_456");
        RazorpayPaymentAdapter adapter = new RazorpayPaymentAdapter(client);

        // ₹180.50 -> 18050 paise
        PaymentTransaction txn = adapter.processPayment("BILL-001", 180.50, PaymentMode.RAZORPAY_CARD);
        assertNotNull(txn);
        assertEquals(PaymentStatus.SUCCESS, txn.getStatus());
        assertEquals("BILL-001", txn.getBillId());
        assertEquals(180.50, txn.getAmount());
        assertEquals(PaymentMode.RAZORPAY_CARD, txn.getPaymentMode());
        assertNotNull(txn.getTransactionReference());
        assertTrue(txn.getTransactionReference().startsWith("SUCCESS_TXN_"));
    }

    @Test
    @DisplayName("RazorpayPaymentAdapter should reject zero or negative amounts appropriately")
    void testRazorpayAdapterInvalidAmount() {
        ExternalRazorpayClient client = new ExternalRazorpayClient("key_123", "secret_456");
        RazorpayPaymentAdapter adapter = new RazorpayPaymentAdapter(client);

        // Zero amount results in FAILED transaction
        PaymentTransaction txnZero = adapter.processPayment("BILL-002", 0.0, PaymentMode.RAZORPAY_UPI);
        assertEquals(PaymentStatus.FAILED, txnZero.getStatus());
        assertEquals("Amount must be greater than zero", txnZero.getFailureReason());

        // Negative amount throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                adapter.processPayment("BILL-003", -50.0, PaymentMode.RAZORPAY_UPI)
        );
    }

    @Test
    @DisplayName("CashPaymentProcessor should create successful transaction for non-negative cash amounts")
    void testCashPaymentProcessor() {
        CashPaymentProcessor processor = new CashPaymentProcessor();

        PaymentTransaction txn = processor.processPayment("BILL-100", 250.0, PaymentMode.CASH);
        assertEquals(PaymentStatus.SUCCESS, txn.getStatus());
        assertNotNull(txn.getTransactionReference());
        assertTrue(txn.getTransactionReference().startsWith("CASH-REF-"));

        // Negative cash amount throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () ->
                processor.processPayment("BILL-101", -10.0, PaymentMode.CASH)
        );
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
