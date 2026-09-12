package com.parkinglot.config;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Operational configuration parameters for the parking lot.
 * Decouples business operational rules (grace period, rounding interval, lost ticket penalty)
 * from core calculation algorithms.
 */
public class ParkingConfiguration {
    private final Duration gracePeriod;
    private final Duration roundingInterval;
    private final double lostTicketPenalty;
    private final Duration maxParkingDuration;

    private ParkingConfiguration(Builder builder) {
        this.gracePeriod = builder.gracePeriod;
        this.roundingInterval = builder.roundingInterval;
        this.lostTicketPenalty = builder.lostTicketPenalty;
        this.maxParkingDuration = builder.maxParkingDuration;
    }

    public static ParkingConfiguration defaultConfig() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Calculates billable duration in hours considering grace period and ceiling rounding.
     */
    public long calculateBillableHours(Instant entryTime, Instant exitTime) {
        if (exitTime.isBefore(entryTime)) {
            throw new IllegalArgumentException("Exit time cannot be before entry time");
        }

        Duration actualDuration = Duration.between(entryTime, exitTime);

        // If stay is within grace period, parking is complimentary
        if (!gracePeriod.isZero() && actualDuration.compareTo(gracePeriod) <= 0) {
            return 0;
        }

        long minutes = actualDuration.toMinutes();
        if (minutes <= 0) {
            minutes = 1; // Minimum charge round-up for non-grace period exits
        }

        long intervalMinutes = roundingInterval.toMinutes();
        long roundedMinutes = ((minutes + intervalMinutes - 1) / intervalMinutes) * intervalMinutes;

        return Math.max(1, (long) Math.ceil((double) roundedMinutes / 60.0));
    }

    public Duration getGracePeriod() {
        return gracePeriod;
    }

    public Duration getRoundingInterval() {
        return roundingInterval;
    }

    public double getLostTicketPenalty() {
        return lostTicketPenalty;
    }

    public Duration getMaxParkingDuration() {
        return maxParkingDuration;
    }

    public static class Builder {
        private Duration gracePeriod = Duration.ZERO;
        private Duration roundingInterval = Duration.ofMinutes(60);
        private double lostTicketPenalty = 500.0;
        private Duration maxParkingDuration = Duration.ofHours(24);

        public Builder withGracePeriod(Duration gracePeriod) {
            this.gracePeriod = Objects.requireNonNull(gracePeriod, "Grace period cannot be null");
            return this;
        }

        public Builder withRoundingInterval(Duration roundingInterval) {
            this.roundingInterval = Objects.requireNonNull(roundingInterval, "Rounding interval cannot be null");
            return this;
        }

        public Builder withLostTicketPenalty(double penalty) {
            if (penalty < 0) {
                throw new IllegalArgumentException("Penalty cannot be negative");
            }
            this.lostTicketPenalty = penalty;
            return this;
        }

        public Builder withMaxParkingDuration(Duration maxDuration) {
            this.maxParkingDuration = Objects.requireNonNull(maxDuration, "Max duration cannot be null");
            return this;
        }

        public ParkingConfiguration build() {
            return new ParkingConfiguration(this);
        }
    }
}
