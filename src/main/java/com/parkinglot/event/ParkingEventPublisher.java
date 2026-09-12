package com.parkinglot.event;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer Pattern: Thread-safe event bus that broadcasts domain events to registered subscribers.
 */
public class ParkingEventPublisher {
    private final List<ParkingEventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(ParkingEventListener listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        listeners.add(listener);
    }

    public void unsubscribe(ParkingEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(ParkingEvent event) {
        Objects.requireNonNull(event, "Event cannot be null");
        for (ParkingEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                // Safeguard publisher from subscriber failures
                System.err.println("Error notifying listener for event: " + event.getEventType() + " - " + e.getMessage());
            }
        }
    }

    public int getSubscriberCount() {
        return listeners.size();
    }
}
