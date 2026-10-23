package com.parkinglot.billing;

public class CarWashDecorator extends BillableItemDecorator {
    private final double washFee;

    public CarWashDecorator(BillableItem wrappedItem, double washFee) {
        super(wrappedItem);
        if (washFee < 0) {
            throw new IllegalArgumentException("Wash fee cannot be negative");
        }
        this.washFee = washFee;
    }

    public CarWashDecorator(BillableItem wrappedItem) {
        this(wrappedItem, 250.0);
    }

    @Override
    public double getCost() {
        return wrappedItem.getCost() + washFee;
    }

    @Override
    public String getDescription() {
        return wrappedItem.getDescription() + " + Car Wash (₹" + washFee + ")";
    }

    @Override
    protected String getServiceLine() {
        return "Car Wash / Detailing: ₹" + washFee;
    }
}
