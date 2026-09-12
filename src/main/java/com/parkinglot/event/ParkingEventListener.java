package com.parkinglot.event;

/**
 * Observer Pattern: Subscriber interface for receiving domain events.
 */
@FunctionalInterface
public interface ParkingEventListener {
    void onEvent(ParkingEvent event);
}
