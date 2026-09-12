package com.parkinglot;

import com.parkinglot.billing.Bill;
import com.parkinglot.billing.BillingService;
import com.parkinglot.enums.GateType;
import com.parkinglot.enums.ParkingSpotType;
import com.parkinglot.enums.PaymentMode;
import com.parkinglot.event.ParkingEventListener;
import com.parkinglot.event.ParkingEventPublisher;
import com.parkinglot.event.VehicleExitedEvent;
import com.parkinglot.event.VehicleParkedEvent;
import com.parkinglot.model.Gate;
import com.parkinglot.model.ParkingFloor;
import com.parkinglot.model.ParkingSpot;
import com.parkinglot.model.Ticket;
import com.parkinglot.model.Vehicle;
import com.parkinglot.payment.PaymentProcessorFactory;
import com.parkinglot.payment.PaymentService;
import com.parkinglot.payment.PaymentStatus;
import com.parkinglot.payment.PaymentTransaction;
import com.parkinglot.service.GateController;
import com.parkinglot.service.TicketManager;
import com.parkinglot.strategy.assignment.ParkingAssignmentStrategy;
import com.parkinglot.strategy.fee.FeeCalculationStrategy;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Facade Pattern: High-level entry point orchestrating parking operations.
 * Adheres strictly to the Single Responsibility Principle (SRP) by delegating to
 * dedicated subsystem services:
 * - {@link GateController}: Physical gate access and passage validation.
 * - {@link TicketManager}: In-memory active session ticket management.
 * - {@link BillingService}: Base fee calculation and Bill generation.
 * - {@link PaymentService}: Payment adapter orchestration and audit trail recording.
 * - {@link ParkingEventPublisher}: Event-driven broadcasts (Observer Pattern) for IoT displays, SMS, and analytics.
 */
public class ParkingLot {
    private final String lotId;
    private final List<ParkingFloor> floors;
    private final ParkingAssignmentStrategy assignmentStrategy;
    private final TicketManager ticketManager;
    private final GateController gateController;
    private final BillingService billingService;
    private final PaymentService paymentService;
    private final ParkingEventPublisher eventPublisher;

    public ParkingLot(
            String lotId,
            List<ParkingFloor> floors,
            ParkingAssignmentStrategy assignmentStrategy,
            TicketManager ticketManager,
            GateController gateController,
            BillingService billingService,
            PaymentService paymentService,
            ParkingEventPublisher eventPublisher) {
        this.lotId = Objects.requireNonNull(lotId, "Lot ID cannot be null");
        this.floors = List.copyOf(Objects.requireNonNull(floors, "Floors cannot be null"));
        this.assignmentStrategy = Objects.requireNonNull(assignmentStrategy, "Assignment strategy cannot be null");
        this.ticketManager = Objects.requireNonNull(ticketManager, "Ticket manager cannot be null");
        this.gateController = Objects.requireNonNull(gateController, "Gate controller cannot be null");
        this.billingService = Objects.requireNonNull(billingService, "Billing service cannot be null");
        this.paymentService = Objects.requireNonNull(paymentService, "Payment service cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "Event publisher cannot be null");
    }

    public ParkingLot(
            String lotId,
            List<ParkingFloor> floors,
            List<Gate> gates,
            ParkingAssignmentStrategy assignmentStrategy,
            FeeCalculationStrategy feeStrategy,
            PaymentProcessorFactory paymentProcessorFactory) {
        this(
                lotId,
                floors,
                assignmentStrategy,
                new TicketManager(),
                new GateController(gates),
                new BillingService(feeStrategy),
                new PaymentService(paymentProcessorFactory),
                new ParkingEventPublisher()
        );
    }

    /**
     * Entry flow: Finds nearest available spot using gate's coordinates and elevation,
     * allocates spot, registers active ticket, cycles entry gate passage, and publishes VehicleParkedEvent.
     */
    public synchronized Ticket parkVehicle(String gateId, Vehicle vehicle) {
        Objects.requireNonNull(vehicle, "Vehicle cannot be null");
        Gate gate = gateController.getValidatedGate(gateId, GateType.ENTRY);

        ParkingSpotType requiredSpotType = ParkingSpotType.fromVehicleType(vehicle.type());

        ParkingSpot assignedSpot = assignmentStrategy.findSpot(
                floors,
                requiredSpotType,
                gate.getFloorNumber(),
                gate.getCoordinates()
        ).orElseThrow(() -> new IllegalStateException("Parking Lot Full: No available spot for " + vehicle.type()));

        assignedSpot.assignVehicle(vehicle);

        Ticket ticket = new Ticket(assignedSpot.getSpotId(), assignedSpot.getFloorNumber(), vehicle, Instant.now());
        ticketManager.registerTicket(ticket);
        gateController.cycleGate(gateId);

        // Broadcast event for notifications, display boards, analytics
        eventPublisher.publish(new VehicleParkedEvent(
                ticket.getTicketNumber(),
                vehicle,
                assignedSpot.getSpotId(),
                assignedSpot.getFloorNumber(),
                ticket.getEntryTime()
        ));

        return ticket;
    }

    /**
     * Exit settlement flow:
     * 1. Validates exit gate.
     * 2. Retrieves active ticket from TicketManager.
     * 3. Generates itemized Bill via BillingService.
     * 4. Settles invoice via PaymentService and logs audit PaymentTransaction.
     * 5. Releases spot on the appropriate ParkingFloor.
     * 6. Closes ticket in TicketManager, cycles exit gate, and publishes VehicleExitedEvent.
     */
    public synchronized Bill processExit(String gateId, String ticketNumber, Instant exitTime, PaymentMode paymentMode) {
        gateController.getValidatedGate(gateId, GateType.EXIT);
        Ticket ticket = ticketManager.getActiveTicketOrThrow(ticketNumber);

        // Generate bill & settle payment via specialized delegates
        Bill bill = billingService.generateBill(ticket, exitTime);
        PaymentTransaction txn = paymentService.processBillPayment(bill, paymentMode);

        if (txn.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Payment processing failed via " + paymentMode + ": " + txn.getFailureReason());
        }

        // Delegate to appropriate ParkingFloor to vacate physical spot
        ParkingFloor targetFloor = floors.stream()
                .filter(f -> f.getFloorNumber() == ticket.getFloorNumber())
                .findFirst()
                .orElse(null);

        if (targetFloor != null) {
            ParkingSpot spot = targetFloor.getSpot(ticket.getSpotId());
            if (spot != null) {
                spot.vacate();
            }
        }

        ticketManager.closeTicket(ticketNumber);
        gateController.cycleGate(gateId);

        // Broadcast event for external notification/display updates
        eventPublisher.publish(new VehicleExitedEvent(
                ticket.getTicketNumber(),
                bill.getBillId(),
                bill.getTotalAmount(),
                ticket.getSpotId(),
                ticket.getFloorNumber(),
                exitTime
        ));

        return bill;
    }

    public void subscribeToEvents(ParkingEventListener listener) {
        eventPublisher.subscribe(listener);
    }

    public int getAvailableSpotCount(ParkingSpotType type) {
        return floors.stream()
                .mapToInt(floor -> floor.getAvailableSpots(type).size())
                .sum();
    }

    public String getLotId() {
        return lotId;
    }

    public List<ParkingFloor> getFloors() {
        return Collections.unmodifiableList(floors);
    }

    public TicketManager getTicketManager() {
        return ticketManager;
    }

    public GateController getGateController() {
        return gateController;
    }

    public BillingService getBillingService() {
        return billingService;
    }

    public PaymentService getPaymentService() {
        return paymentService;
    }

    public ParkingEventPublisher getEventPublisher() {
        return eventPublisher;
    }
}
