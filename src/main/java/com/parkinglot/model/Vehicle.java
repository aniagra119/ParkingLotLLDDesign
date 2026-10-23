package com.parkinglot.model;

import com.parkinglot.enums.VehicleType;

/**
 * Value Object representing a vehicle.
 * Immutable record with compact constructor validation and an optional Builder for extended metadata.
 */
public record Vehicle(String licensePlate, VehicleType type, String color, String ownerPhone) {

    public Vehicle {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new IllegalArgumentException("License plate cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Vehicle type cannot be null");
        }
    }

    /**
     * Convenience secondary constructor for mandatory fields.
     */
    public Vehicle(String licensePlate, VehicleType type) {
        this(licensePlate, type, null, null);
    }

    public static Builder builder(String licensePlate, VehicleType type) {
        return new Builder(licensePlate, type);
    }

    public static class Builder {
        private final String licensePlate;
        private final VehicleType type;
        private String color;
        private String ownerPhone;

        public Builder(String licensePlate, VehicleType type) {
            this.licensePlate = licensePlate;
            this.type = type;
        }

        public Builder withColor(String color) {
            this.color = color;
            return this;
        }

        public Builder withOwnerPhone(String ownerPhone) {
            this.ownerPhone = ownerPhone;
            return this;
        }

        public Vehicle build() {
            return new Vehicle(licensePlate, type, color, ownerPhone);
        }
    }
}
