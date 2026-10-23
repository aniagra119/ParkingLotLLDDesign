package com.parkinglot.billing;

import com.parkinglot.model.Ticket;
import com.parkinglot.strategy.fee.FeeCalculationStrategy;
import java.time.Instant;
import java.util.Objects;

/**
 * Generates transactional Bills from active Tickets using injected fee calculation strategies.
 * Single Responsibility: Invoice generation and tariff evaluation.
 */
public class BillingService {
    private final FeeCalculationStrategy feeStrategy;

    public BillingService(FeeCalculationStrategy feeStrategy) {
        this.feeStrategy = Objects.requireNonNull(feeStrategy, "Fee strategy cannot be null");
    }

    public Bill generateBill(Ticket ticket, Instant exitTime) {
        Objects.requireNonNull(ticket, "Ticket cannot be null");
        Objects.requireNonNull(exitTime, "Exit time cannot be null");

        double baseFee = feeStrategy.calculateFee(ticket, exitTime);
        return new Bill(ticket, exitTime, baseFee);
    }

    public FeeCalculationStrategy getFeeStrategy() {
        return feeStrategy;
    }
}
