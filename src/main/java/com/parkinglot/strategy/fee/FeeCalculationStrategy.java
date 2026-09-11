package com.parkinglot.strategy.fee;

import com.parkinglot.model.Ticket;
import java.time.Instant;

public interface FeeCalculationStrategy {
    double calculateFee(Ticket ticket, Instant exitTime);
}
