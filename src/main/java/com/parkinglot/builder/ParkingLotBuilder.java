package com.parkinglot.builder;

import com.parkinglot.ParkingLot;
import com.parkinglot.billing.BillingService;
import com.parkinglot.enums.GateType;
import com.parkinglot.enums.ParkingSpotType;
import com.parkinglot.event.ParkingEventListener;
import com.parkinglot.event.ParkingEventPublisher;
import com.parkinglot.model.Coordinates;
import com.parkinglot.model.Gate;
import com.parkinglot.model.ParkingFloor;
import com.parkinglot.model.ParkingSpot;
import com.parkinglot.payment.PaymentProcessorFactory;
import com.parkinglot.payment.PaymentService;
import com.parkinglot.payment.external.ExternalRazorpayClient;
import com.parkinglot.service.GateController;
import com.parkinglot.service.TicketManager;
import com.parkinglot.strategy.assignment.NearestAvailableSpotStrategy;
import com.parkinglot.strategy.assignment.ParkingAssignmentStrategy;
import com.parkinglot.strategy.fee.FeeCalculationStrategy;
import com.parkinglot.strategy.fee.slab.TieredHourlyFeeStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder Pattern: Fluent construction of multi-floor, multi-gate parking lots.
 * Assembles specialized SRP delegates and event listeners into a cohesive ParkingLot Facade.
 */
public class ParkingLotBuilder {
    private String lotId = "LOT-MAIN";
    private final List<ParkingFloor> floors = new ArrayList<>();
    private final List<Gate> gates = new ArrayList<>();
    private final List<ParkingEventListener> eventListeners = new ArrayList<>();
    private ParkingAssignmentStrategy assignmentStrategy = new NearestAvailableSpotStrategy();
    private FeeCalculationStrategy feeStrategy = new TieredHourlyFeeStrategy();
    private PaymentProcessorFactory paymentProcessorFactory;

    public ParkingLotBuilder() {
        ExternalRazorpayClient defaultRazorpay = new ExternalRazorpayClient("rzp_test_key", "rzp_test_secret");
        this.paymentProcessorFactory = new PaymentProcessorFactory(defaultRazorpay);
    }

    public ParkingLotBuilder withLotId(String lotId) {
        this.lotId = Objects.requireNonNull(lotId, "Lot ID cannot be null");
        return this;
    }

    public ParkingLotBuilder withAssignmentStrategy(ParkingAssignmentStrategy strategy) {
        this.assignmentStrategy = Objects.requireNonNull(strategy, "Assignment strategy cannot be null");
        return this;
    }

    public ParkingLotBuilder withFeeStrategy(FeeCalculationStrategy strategy) {
        this.feeStrategy = Objects.requireNonNull(strategy, "Fee strategy cannot be null");
        return this;
    }

    public ParkingLotBuilder withPaymentProcessorFactory(PaymentProcessorFactory factory) {
        this.paymentProcessorFactory = Objects.requireNonNull(factory, "Payment processor factory cannot be null");
        return this;
    }

    public ParkingLotBuilder withEventListener(ParkingEventListener listener) {
        this.eventListeners.add(Objects.requireNonNull(listener, "Listener cannot be null"));
        return this;
    }

    public ParkingLotBuilder addGate(String gateId, GateType type, int floorNumber, Coordinates coordinates) {
        gates.add(new Gate(gateId, type, floorNumber, coordinates));
        return this;
    }

    public ParkingLotBuilder addGate(String gateId, GateType type) {
        return addGate(gateId, type, 1, new Coordinates(0, 0));
    }

    public ParkingLotBuilder addFloor(String floorId, int floorNumber, int bikeSpots, int carSpots, int truckSpots) {
        ParkingFloor floor = new ParkingFloor(floorId, floorNumber);
        int coordinateCounter = 0;

        for (int i = 1; i <= bikeSpots; i++) {
            floor.addSpot(new ParkingSpot(floorId + "-B" + i, floorNumber, ParkingSpotType.BIKE, new Coordinates(coordinateCounter++, 0)));
        }
        for (int i = 1; i <= carSpots; i++) {
            floor.addSpot(new ParkingSpot(floorId + "-C" + i, floorNumber, ParkingSpotType.CAR, new Coordinates(coordinateCounter++, 0)));
        }
        for (int i = 1; i <= truckSpots; i++) {
            floor.addSpot(new ParkingSpot(floorId + "-T" + i, floorNumber, ParkingSpotType.TRUCK, new Coordinates(coordinateCounter++, 0)));
        }

        floors.add(floor);
        return this;
    }

    public ParkingLot build() {
        if (floors.isEmpty()) {
            throw new IllegalStateException("Parking lot must have at least one floor");
        }
        if (gates.isEmpty()) {
            throw new IllegalStateException("Parking lot must have at least one gate");
        }

        TicketManager ticketManager = new TicketManager();
        GateController gateController = new GateController(gates);
        BillingService billingService = new BillingService(feeStrategy);
        PaymentService paymentService = new PaymentService(paymentProcessorFactory);
        ParkingEventPublisher eventPublisher = new ParkingEventPublisher();

        for (ParkingEventListener listener : eventListeners) {
            eventPublisher.subscribe(listener);
        }

        return new ParkingLot(
                lotId,
                floors,
                assignmentStrategy,
                ticketManager,
                gateController,
                billingService,
                paymentService,
                eventPublisher
        );
    }
}
