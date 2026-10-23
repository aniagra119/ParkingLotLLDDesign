package com.parkinglot.builder;

import com.parkinglot.ParkingLot;
import com.parkinglot.enums.GateType;
import com.parkinglot.enums.ParkingSpotType;
import com.parkinglot.model.Gate;
import com.parkinglot.model.Coordinates;
import com.parkinglot.model.ParkingFloor;
import com.parkinglot.model.ParkingSpot;
import com.parkinglot.payment.ExternalRazorpayClient;
import com.parkinglot.payment.PaymentProcessorFactory;
import com.parkinglot.strategy.assignment.NearestAvailableSpotStrategy;
import com.parkinglot.strategy.assignment.ParkingAssignmentStrategy;
import com.parkinglot.strategy.fee.FeeCalculationStrategy;
import com.parkinglot.strategy.fee.TieredHourlyFeeStrategy;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder Pattern: Fluent construction of multi-floor, multi-gate parking lots.
 */
public class ParkingLotBuilder {
    private String lotId = "LOT-MAIN";
    private final List<ParkingFloor> floors = new ArrayList<>();
    private final List<Gate> gates = new ArrayList<>();
    private ParkingAssignmentStrategy assignmentStrategy = new NearestAvailableSpotStrategy();
    private FeeCalculationStrategy feeStrategy = new TieredHourlyFeeStrategy();
    private PaymentProcessorFactory paymentProcessorFactory;

    public ParkingLotBuilder() {
        ExternalRazorpayClient defaultRazorpay = new ExternalRazorpayClient("rzp_test_key", "rzp_test_secret");
        this.paymentProcessorFactory = new PaymentProcessorFactory(defaultRazorpay);
    }

    public ParkingLotBuilder withLotId(String lotId) {
        this.lotId = lotId;
        return this;
    }

    public ParkingLotBuilder withAssignmentStrategy(ParkingAssignmentStrategy strategy) {
        this.assignmentStrategy = strategy;
        return this;
    }

    public ParkingLotBuilder withFeeStrategy(FeeCalculationStrategy feeStrategy) {
        this.feeStrategy = feeStrategy;
        return this;
    }

    public ParkingLotBuilder withPaymentProcessorFactory(PaymentProcessorFactory factory) {
        this.paymentProcessorFactory = factory;
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
        int coordinateCounter = 1;

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
        return new ParkingLot(lotId, floors, gates, assignmentStrategy, feeStrategy, paymentProcessorFactory);
    }
}
