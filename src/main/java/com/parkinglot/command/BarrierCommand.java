package com.parkinglot.command;

import com.parkinglot.model.User;

/**
 * Command Pattern: Abstraction for controlling physical barriers at gates.
 * Encapsulates gate barrier operations into executable objects with operator tracking.
 */
public interface BarrierCommand {
    void execute(User operator);
    String getCommandName();
}
