package com.parkinglot.billing.decorator;

import com.parkinglot.billing.service.AmenityService;
import java.util.Objects;

/**
 * Concrete Decorator: Dynamically decorates an invoice with consumed amenities (EV Charging, Car Wash).
 */
public class AmenityServiceDecorator extends BillableItemDecorator {
    private final AmenityService amenityService;

    public AmenityServiceDecorator(BillableItem wrappedItem, AmenityService amenityService) {
        super(wrappedItem);
        this.amenityService = Objects.requireNonNull(amenityService, "Amenity service cannot be null");
    }

    @Override
    protected double getAdditionalCost() {
        return amenityService.calculateCost();
    }

    @Override
    protected String getItemizedLine() {
        return amenityService.getItemizedDescription();
    }

    public AmenityService getAmenityService() {
        return amenityService;
    }
}
