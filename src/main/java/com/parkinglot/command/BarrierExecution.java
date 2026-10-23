package com.parkinglot.command;

import com.parkinglot.model.User;
import java.time.Instant;
import java.util.Objects;

/**
 * Audit record of a physical barrier movement command.
 */
public record BarrierExecution(String barrierId, String commandName, User operator, Instant executedAt) {
    public BarrierExecution {
        Objects.requireNonNull(barrierId, "Barrier ID cannot be null");
        Objects.requireNonNull(commandName, "Command name cannot be null");
        Objects.requireNonNull(operator, "Operator cannot be null");
        if (executedAt == null) {
            executedAt = Instant.now();
        }
    }

    public BarrierExecution(String barrierId, String commandName, User operator) {
        this(barrierId, commandName, operator, Instant.now());
    }
}
