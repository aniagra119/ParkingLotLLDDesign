package com.parkinglot.payment;

import com.parkinglot.enums.PaymentMode;
import com.parkinglot.payment.adapter.RazorpayPaymentAdapter;
import com.parkinglot.payment.external.ExternalRazorpayClient;
import com.parkinglot.payment.processor.CashPaymentProcessor;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Simple Factory & Registry Pattern:
 * Decoupled from vendor-specific SDK clients. Deals strictly with domain {@link PaymentProcessor} abstractions.
 * Specific vendor adapters (e.g. RazorpayPaymentAdapter, StripePaymentAdapter) are composed outside the factory.
 */
public class PaymentProcessorFactory {
    private final Map<PaymentMode, PaymentProcessor> registry = new EnumMap<>(PaymentMode.class);

    public PaymentProcessorFactory(Map<PaymentMode, PaymentProcessor> processors) {
        Objects.requireNonNull(processors, "Processors map cannot be null");
        this.registry.putAll(processors);
    }

    /**
     * Primary constructor: associates offline cash with CashPaymentProcessor and online modes with the provided adapter.
     */
    public PaymentProcessorFactory(PaymentProcessor onlineProcessor) {
        Objects.requireNonNull(onlineProcessor, "Online payment processor cannot be null");
        this.registry.put(PaymentMode.CASH, new CashPaymentProcessor());
        this.registry.put(PaymentMode.RAZORPAY_UPI, onlineProcessor);
        this.registry.put(PaymentMode.RAZORPAY_CARD, onlineProcessor);
    }

    /**
     * Convenience backward-compatible constructor: constructs the RazorpayPaymentAdapter on caller's behalf.
     */
    public PaymentProcessorFactory(ExternalRazorpayClient razorpayClient) {
        this(new RazorpayPaymentAdapter(razorpayClient));
    }

    public void registerProcessor(PaymentMode mode, PaymentProcessor processor) {
        Objects.requireNonNull(mode, "Payment mode cannot be null");
        Objects.requireNonNull(processor, "Payment processor cannot be null");
        this.registry.put(mode, processor);
    }

    public PaymentProcessor getPaymentProcessor(PaymentMode mode) {
        Objects.requireNonNull(mode, "Payment mode cannot be null");
        PaymentProcessor processor = registry.get(mode);
        if (processor == null) {
            throw new IllegalArgumentException("No payment processor registered for mode: " + mode);
        }
        return processor;
    }
}
