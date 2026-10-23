package com.parkinglot.billing.decorator;

import java.util.List;

/**
 * Component interface in the Decorator Pattern for billing.
 * Represents an itemized line item or aggregate invoice charge.
 */
public interface BillableItem {
    double getCost();
    List<String> getItemizedLines();
}
