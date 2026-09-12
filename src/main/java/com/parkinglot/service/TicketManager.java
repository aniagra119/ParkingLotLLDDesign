package com.parkinglot.service;

import com.parkinglot.model.Ticket;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active ticket lifecycle, storage, lookup, and closing.
 * Single Responsibility: In-memory active session ticket management.
 */
public class TicketManager {
    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();

    public void registerTicket(Ticket ticket) {
        Objects.requireNonNull(ticket, "Ticket cannot be null");
        activeTickets.put(ticket.getTicketNumber(), ticket);
    }

    public Optional<Ticket> getActiveTicket(String ticketNumber) {
        if (ticketNumber == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeTickets.get(ticketNumber));
    }

    public Ticket getActiveTicketOrThrow(String ticketNumber) {
        return getActiveTicket(ticketNumber)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive ticket: " + ticketNumber));
    }

    public void closeTicket(String ticketNumber) {
        if (ticketNumber != null) {
            activeTickets.remove(ticketNumber);
        }
    }

    public int getActiveTicketCount() {
        return activeTickets.size();
    }
}
