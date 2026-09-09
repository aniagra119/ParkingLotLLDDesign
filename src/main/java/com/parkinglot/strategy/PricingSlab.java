package com.parkinglot.strategy;

public record PricingSlab(int startHourInclusive, int endHourExclusive, double hourlyRate) {
    public PricingSlab {
        if (startHourInclusive < 0 || endHourExclusive <= startHourInclusive || hourlyRate < 0) {
            throw new IllegalArgumentException("Invalid pricing slab bounds or rate");
        }
    }

    public boolean appliesTo(int hour) {
        return hour >= startHourInclusive && hour < endHourExclusive;
    }
}
