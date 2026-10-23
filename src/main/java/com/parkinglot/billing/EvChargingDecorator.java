package com.parkinglot.billing;

public class EvChargingDecorator extends BillableItemDecorator {
    private final double chargingFee;

    public EvChargingDecorator(BillableItem wrappedItem, double chargingFee) {
        super(wrappedItem);
        if (chargingFee < 0) {
            throw new IllegalArgumentException("Charging fee cannot be negative");
        }
        this.chargingFee = chargingFee;
    }

    public EvChargingDecorator(BillableItem wrappedItem) {
        this(wrappedItem, 150.0);
    }

    @Override
    public double getCost() {
        return wrappedItem.getCost() + chargingFee;
    }

    @Override
    public String getDescription() {
        return wrappedItem.getDescription() + " + EV Charging (₹" + chargingFee + ")";
    }

    @Override
    protected String getServiceLine() {
        return "EV Charging Service: ₹" + chargingFee;
    }
}
