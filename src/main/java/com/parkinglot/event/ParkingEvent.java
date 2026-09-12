package com.parkinglot.event;

import java.time.Instant;

/**
 * Common domain contract for all asynchronous or broadcast events within the parking lot.
 * Supports cross-cutting concerns (LED displays, SMS notifications, audit logs, analytics).
 */
public interface ParkingEvent {
    Instant getTimestamp();
    String getEventType();
}
