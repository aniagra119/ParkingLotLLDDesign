package com.parkinglot.billing;

import com.parkinglot.billing.decorator.AmenityServiceDecorator;
import com.parkinglot.billing.decorator.BaseParkingCharge;
import com.parkinglot.billing.decorator.BillableItem;
import com.parkinglot.billing.service.AmenityService;
import com.parkinglot.model.Ticket;
import com.parkinglot.strategy.fee.FeeCalculationStrategy;
import java.time.Instant;
import java.util.Objects;

/**
 * Generates transactional Bills from active Tickets using injected fee calculation strategies
 * and dynamically composes itemized charges using the Decorator Pattern.
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
        BillableItem item = new BaseParkingCharge(baseFee);
        for (AmenityService service : ticket.getConsumedServices()) {
            item = new AmenityServiceDecorator(item, service);
        }
        return new Bill(ticket, exitTime, item);
    }

    public FeeCalculationStrategy getFeeStrategy() {
        return feeStrategy;
    }
}
