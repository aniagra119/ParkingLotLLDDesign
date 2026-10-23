package com.parkinglot.model;

import com.parkinglot.enums.UserRole;
import java.time.Instant;
import java.util.Objects;

/**
 * Value Object representing a human operator or system actor.
 * Enables operational auditing, role-based accountability, and disputed transaction resolution.
 */
public record User(String userId, String name, UserRole role, Instant createdAt) {

    public static final User SYSTEM_KIOSK = new User("SYS-AUTO-01", "Automated Barrier Kiosk", UserRole.SYSTEM, Instant.EPOCH);

    public User {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name cannot be null or blank");
        }
        Objects.requireNonNull(role, "User role cannot be null");
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public User(String userId, String name, UserRole role) {
        this(userId, name, role, Instant.now());
    }
}
