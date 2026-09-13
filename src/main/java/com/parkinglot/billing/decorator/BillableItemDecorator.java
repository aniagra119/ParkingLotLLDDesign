package com.parkinglot.billing.decorator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Decorator Base: Wraps a BillableItem and dynamically adds itemized services or surcharges.
 * Demonstrates the Template Method pattern inside Decorator: defines structure for accumulating costs and lines.
 */
public abstract class BillableItemDecorator implements BillableItem {
    protected final BillableItem wrappedItem;

    public BillableItemDecorator(BillableItem wrappedItem) {
        this.wrappedItem = Objects.requireNonNull(wrappedItem, "Wrapped billable item cannot be null");
    }

    @Override
    public double getCost() {
        return Math.round((wrappedItem.getCost() + getAdditionalCost()) * 100.0) / 100.0;
    }

    @Override
    public List<String> getItemizedLines() {
        List<String> lines = new ArrayList<>(wrappedItem.getItemizedLines());
        lines.add(getItemizedLine());
        return Collections.unmodifiableList(lines);
    }

    protected abstract double getAdditionalCost();
    protected abstract String getItemizedLine();

    public BillableItem getWrappedItem() {
        return wrappedItem;
    }
}
