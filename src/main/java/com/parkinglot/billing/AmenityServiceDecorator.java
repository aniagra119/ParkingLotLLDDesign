package com.parkinglot.billing;

import com.parkinglot.billing.service.AmenityService;
import java.util.Objects;

/**
 * Universal Decorator: Dynamically layers any AmenityService onto a BillableItem
 * without subclass proliferation or hardcoded numbers.
 */
public class AmenityServiceDecorator extends BillableItemDecorator {
    private final AmenityService amenityService;

    public AmenityServiceDecorator(BillableItem wrappedItem, AmenityService amenityService) {
        super(wrappedItem);
        this.amenityService = Objects.requireNonNull(amenityService, "Amenity service cannot be null");
    }

    @Override
    public double getCost() {
        return Math.round((wrappedItem.getCost() + amenityService.calculateCost()) * 100.0) / 100.0;
    }

    @Override
    public String getDescription() {
        return wrappedItem.getDescription() + " + " + amenityService.getItemizedDescription();
    }

    @Override
    protected String getServiceLine() {
        return amenityService.getItemizedDescription();
    }

    public AmenityService getAmenityService() {
        return amenityService;
    }
}
