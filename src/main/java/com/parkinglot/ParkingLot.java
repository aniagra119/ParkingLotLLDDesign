package com.parkinglot;

import com.parkinglot.billing.AmenityServiceDecorator;
import com.parkinglot.billing.BaseParkingCharge;
import com.parkinglot.billing.Bill;
import com.parkinglot.billing.BillableItem;
import com.parkinglot.billing.service.AmenityService;
import com.parkinglot.enums.GateType;
import com.parkinglot.enums.ParkingSpotType;
import com.parkinglot.enums.PaymentMode;
import com.parkinglot.gate.Gate;
import com.parkinglot.model.ParkingFloor;
import com.parkinglot.model.ParkingSpot;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.Vehicle;
import com.parkinglot.payment.PaymentProcessor;
import com.parkinglot.payment.PaymentProcessorFactory;
import com.parkinglot.strategy.FeeCalculationStrategy;
import com.parkinglot.strategy.ParkingAssignmentStrategy;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Facade Pattern: Unified, thread-safe entry point coordinating spot allocation,
 * ticketing, billing, dynamic amenity decorators, payments, and gate barriers.
 */
public class ParkingLot {
    private final String lotId;
    private final List<ParkingFloor> floors;
    private final Map<String, Gate> gates;
    private final ParkingAssignmentStrategy assignmentStrategy;
    private final FeeCalculationStrategy feeStrategy;
    private final PaymentProcessorFactory paymentProcessorFactory;

    private final Map<String, Ticket> activeTickets;
    private final Map<String, ParkingSpot> spotIndex;

    public ParkingLot(
            String lotId,
            List<ParkingFloor> floors,
            List<Gate> gates,
            ParkingAssignmentStrategy assignmentStrategy,
            FeeCalculationStrategy feeStrategy,
            PaymentProcessorFactory paymentProcessorFactory) {
        this.lotId = Objects.requireNonNull(lotId, "Lot ID cannot be null");
        this.floors = List.copyOf(Objects.requireNonNull(floors, "Floors cannot be null"));
        this.assignmentStrategy = Objects.requireNonNull(assignmentStrategy, "Assignment strategy cannot be null");
        this.feeStrategy = Objects.requireNonNull(feeStrategy, "Fee strategy cannot be null");
        this.paymentProcessorFactory = Objects.requireNonNull(paymentProcessorFactory, "Payment processor factory cannot be null");

        this.gates = new ConcurrentHashMap<>();
        for (Gate gate : gates) {
            this.gates.put(gate.getGateId(), gate);
        }

        this.activeTickets = new ConcurrentHashMap<>();
        this.spotIndex = new ConcurrentHashMap<>();
        for (ParkingFloor floor : this.floors) {
            for (ParkingSpot spot : floor.getSpots()) {
                this.spotIndex.put(spot.getSpotId(), spot);
            }
        }
    }

    /**
     * Entry flow: Finds nearest available spot using gate's coordinates and elevation,
     * locks spot, cycles barrier, and issues an immutable Ticket.
     */
    public synchronized Ticket parkVehicle(String gateId, Vehicle vehicle) {
        Gate gate = getValidatedGate(gateId, GateType.ENTRY);
        Objects.requireNonNull(vehicle, "Vehicle cannot be null");

        ParkingSpotType requiredSpotType = switch (vehicle.type()) {
            case BIKE -> ParkingSpotType.BIKE;
            case CAR -> ParkingSpotType.CAR;
            case TRUCK -> ParkingSpotType.TRUCK;
        };

        ParkingSpot assignedSpot = assignmentStrategy.findSpot(
                floors,
                requiredSpotType,
                gate.getFloorNumber(),
                gate.getCoordinates()
        ).orElseThrow(() -> new IllegalStateException("Parking Lot Full: No available spot for " + vehicle.type()));

        assignedSpot.assignVehicle(vehicle);

        gate.getBarrier().open();
        Ticket ticket = new Ticket(assignedSpot.getSpotId(), assignedSpot.getFloorNumber(), vehicle, Instant.now());
        activeTickets.put(ticket.getTicketNumber(), ticket);
        gate.getBarrier().close();

        return ticket;
    }

    /**
     * Exit settlement flow:
     * 1. Validates active ticket.
     * 2. Computes base fee via Strategy.
     * 3. Wraps all consumed amenity services dynamically via universal Decorator.
     * 4. Processes payment via Adapter Factory.
     * 5. Vacates physical spot, cycles barrier, and closes session.
     */
    public synchronized Bill processExit(String gateId, String ticketNumber, Instant exitTime, PaymentMode paymentMode) {
        Gate gate = getValidatedGate(gateId, GateType.EXIT);

        Ticket ticket = activeTickets.get(ticketNumber);
        if (ticket == null) {
            throw new IllegalArgumentException("Invalid or inactive ticket: " + ticketNumber);
        }

        // 1. Calculate base fee via Strategy
        double baseFee = feeStrategy.calculateFee(ticket, exitTime);
        BillableItem billableItem = new BaseParkingCharge(baseFee);

        // 2. Wrap all services consumed during this session via universal Decorator
        for (AmenityService service : ticket.getConsumedServices()) {
            billableItem = new AmenityServiceDecorator(billableItem, service);
        }

        Bill bill = new Bill(ticket, exitTime, billableItem);

        // 3. Process payment via Adapter
        PaymentProcessor processor = paymentProcessorFactory.getPaymentProcessor(paymentMode);
        boolean paymentSuccess = processor.processPayment(bill.getBillId(), bill.getTotalAmount());
        if (!paymentSuccess) {
            throw new IllegalStateException("Payment processing failed via " + paymentMode);
        }

        bill.markPaid();

        // 4. Vacate physical spot and unregister active ticket
        ParkingSpot spot = spotIndex.get(ticket.getSpotId());
        if (spot != null) {
            spot.vacate();
        }
        activeTickets.remove(ticketNumber);

        // 5. Open barrier for departure
        gate.getBarrier().open();
        gate.getBarrier().close();

        return bill;
    }

    private Gate getValidatedGate(String gateId, GateType expectedType) {
        Gate gate = gates.get(gateId);
        if (gate == null) {
            throw new IllegalArgumentException("Gate not found: " + gateId);
        }
        if (gate.getGateType() != expectedType) {
            throw new IllegalStateException("Gate " + gateId + " is not an " + expectedType + " gate");
        }
        return gate;
    }

    public String getLotId() {
        return lotId;
    }

    public List<ParkingFloor> getFloors() {
        return Collections.unmodifiableList(floors);
    }

    public int getAvailableSpotCount(ParkingSpotType type) {
        return floors.stream()
                .mapToInt(f -> f.getAvailableSpots(type).size())
                .sum();
    }

    public Ticket getActiveTicket(String ticketNumber) {
        return activeTickets.get(ticketNumber);
    }
}
