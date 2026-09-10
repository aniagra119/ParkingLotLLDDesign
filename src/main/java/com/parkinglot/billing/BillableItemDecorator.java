package com.parkinglot.billing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class BillableItemDecorator implements BillableItem {
    protected final BillableItem wrappedItem;

    public BillableItemDecorator(BillableItem wrappedItem) {
        this.wrappedItem = Objects.requireNonNull(wrappedItem, "Wrapped billable item cannot be null");
    }

    @Override
    public List<String> getItemizedLines() {
        List<String> lines = new ArrayList<>(wrappedItem.getItemizedLines());
        lines.add(getServiceLine());
        return lines;
    }

    protected abstract String getServiceLine();
}
