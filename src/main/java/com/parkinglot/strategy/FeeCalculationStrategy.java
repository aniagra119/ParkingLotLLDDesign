package com.parkinglot.strategy;

import com.parkinglot.model.Ticket;
import java.time.Instant;

public interface FeeCalculationStrategy {
    double calculateFee(Ticket ticket, Instant exitTime);
}
