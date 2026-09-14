// ============================================================================
// RELATIONSHIPS & STRUCTURAL CONTRACTS
// ============================================================================
// - ParkingLot has-a List<ParkingFloor> (Composition)
// - ParkingLot has-a List<Gate> (Composition, multiple Entry and Exit gates)
// - ParkingLot has-a ParkingAssignmentStrategy (Aggregation via Strategy Pattern)
// - ParkingLot has-a FeeCalculationStrategy (Aggregation via Strategy Pattern)
// - ParkingLot has-a TicketManager (Composition, active session management)
// - ParkingLot has-a GateController (Composition, physical access & barrier control)
// - ParkingLot has-a BillingService (Composition, invoice generation & amenity decoration)
// - ParkingLot has-a PaymentService (Composition, financial settlement orchestration)
// - ParkingLot has-a ParkingEventPublisher (Composition via Observer Pattern)
// - ParkingFloor has-a List<ParkingSpot> (Composition)
// - ParkingSpot has-a ParkingSpotType (Composition, Enum: CAR, BIKE, TRUCK)
// - ParkingSpot has-a ParkingSpotStatus (Composition, Enum: AVAILABLE, OCCUPIED, OUT_OF_SERVICE)
// - ParkingSpot has-a Coordinates (Composition, spatial location for distance calculation)
// - ParkingSpot has-a Vehicle (Aggregation, null if spot is available)
// - Vehicle has-a VehicleType (Composition, Enum: CAR, BIKE, TRUCK)
// - Gate has-a GateType (Composition, Enum: ENTRY, EXIT)
// - Gate has-a Coordinates (Composition, physical location of gate)
// - Gate has-a Barrier (Composition, physical boom barrier hardware)
// - Gate has-a Operator (Aggregation, currently assigned gate operator)
// - Barrier has-a BarrierStatus (Composition, Enum: RAISED, LOWERED)
// - Barrier has-a List<BarrierExecution> (Composition, audit log of hardware actuations)
// - BarrierCommand is interface implemented by OpenBarrierCommand and CloseBarrierCommand (Command Pattern)
// - OpenBarrierCommand has-a Barrier (Aggregation via Command Pattern)
// - CloseBarrierCommand has-a Barrier (Aggregation via Command Pattern)
// - User is base class for Operator (Inheritance / Polymorphism)
// - Operator has-a String employeeId (Composition)
// - Ticket has-a Vehicle (Aggregation)
// - Ticket has-a ParkingSpot (Aggregation, allocated parking spot)
// - Ticket has-a Gate (Aggregation, entry gate where ticket was issued)
// - Ticket has-a Operator (Aggregation, operator who generated ticket)
// - Ticket has-a TicketStatus (Composition, Enum: ACTIVE, PAID, CANCELLED)
// - Bill has-a Ticket (Aggregation)
// - Bill has-a Gate (Aggregation, exit gate where bill is calculated)
// - Bill has-a Operator (Aggregation, operator who calculated and collected bill)
// - Bill has-a BillStatus (Composition, Enum: UNPAID, PAID)
// - Bill has-a BillableItem (Composition via Decorator Pattern)
// - Bill has-a List<PaymentTransaction> (Composition, payment settlement audit trail)
// - BillableItem is interface implemented by BaseParkingCharge and BillableItemDecorator (Decorator Pattern)
// - BillableItemDecorator is abstract base class for AmenityServiceDecorator (Decorator / Template Method Pattern)
// - AmenityServiceDecorator has-a AmenityService (Aggregation: EvChargingService, CarWashService)
// - PaymentTransaction has-a PaymentMode (Composition, Enum: RAZORPAY_UPI, RAZORPAY_CARD, RAZORPAY_NETBANKING, CASH)
// - PaymentTransaction has-a PaymentStatus (Composition, Enum: SUCCESS, FAILED, PENDING)
// - PaymentProcessor is interface implemented by RazorpayPaymentAdapter and CashPaymentProcessor (Adapter Pattern)
// - RazorpayPaymentAdapter has-a ExternalRazorpayClient (Composition via Adapter Pattern)
// - PaymentProcessorFactory has-a Map<PaymentMode, PaymentProcessor> (Registry Pattern)
// - ParkingAssignmentStrategy is interface implemented by NearestAvailableSpotStrategy (Strategy Pattern)
// - FeeCalculationStrategy is interface implemented by SlabBasedFeeStrategy (Strategy / Template Method Pattern)
// - SlabBasedFeeStrategy is abstract base class for TieredHourlyFeeStrategy and FlatRateFeeStrategy (Template Method)
// - TieredHourlyFeeStrategy has-a Map<VehicleType, List<PricingSlab>> (Composition, progressive hourly tariff slabs)
// - PricingSlab has-a int startHour, int endHour, double hourlyRate (Value Object)
// - ParkingEventPublisher has-a List<ParkingEventListener> (Aggregation via Observer Pattern)
// - ParkingEventListener is interface implemented by DisplayBoardListener and NotificationListener (Observer Pattern)
// - ParkingEvent is abstract base class for VehicleParkedEvent and VehicleExitedEvent (Observer Pattern)

// ============================================================================
// ENUMS
// ============================================================================

enum VehicleType {
    BIKE,
    CAR,
    TRUCK
}

enum ParkingSpotType {
    BIKE,
    CAR,
    TRUCK
}

enum ParkingSpotStatus {
    AVAILABLE,
    OCCUPIED,
    OUT_OF_SERVICE
}

enum GateType {
    ENTRY,
    EXIT
}

enum BarrierStatus {
    LOWERED,
    RAISED
}

enum TicketStatus {
    ACTIVE,
    PAID,
    CANCELLED
}

enum BillStatus {
    UNPAID,
    PAID
}

enum PaymentMode {
    RAZORPAY_UPI,
    RAZORPAY_CARD,
    RAZORPAY_NETBANKING,
    CASH
}

enum PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING
}

enum UserRole {
    PARKING_ATTENDANT,
    SUPERVISOR,
    ADMINISTRATOR,
    SYSTEM
}

enum WashPackage {
    BASIC_WASH,
    INTERIOR_VACUUM,
    FULL_SPA
}

// ============================================================================
// VALUE OBJECTS & GEOMETRY
// ============================================================================

class Coordinates {
    - int x
    - int y

    + Coordinates(int x, int y)
    + int getX()
    + int getY()
    + double distanceTo(Coordinates other)
}

class PricingSlab {
    - int startHour
    - int endHour
    - double hourlyRate

    + PricingSlab(int startHour, int endHour, double hourlyRate)
    + int getStartHour()
    + int getEndHour()
    + double getHourlyRate()
    + boolean appliesTo(long hour)
}

class BarrierExecution {
    - String barrierId
    - String commandName
    - Operator executedBy
    - Instant executionTime

    + BarrierExecution(String barrierId, String commandName, Operator executedBy, Instant executionTime)
    + String getBarrierId()
    + String getCommandName()
    + Operator getExecutedBy()
    + Instant getExecutionTime()
}

// ============================================================================
// ACTORS & USERS
// ============================================================================

abstract class User {
    # String userId
    # String name
    # String email
    # String phoneNumber
    # UserRole role

    + User(String userId, String name, String email, String phoneNumber, UserRole role)
    + String getUserId()
    + String getName()
    + String getEmail()
    + String getPhoneNumber()
    + UserRole getRole()
}

class Operator extends User {
    - String employeeId
    - String shiftTiming

    + Operator(String userId, String name, String email, String phoneNumber, UserRole role, String employeeId, String shiftTiming)
    + String getEmployeeId()
    + String getShiftTiming()
}

// ============================================================================
// HARDWARE & BARRIER CONTROL (Command Pattern)
// ============================================================================

class Barrier {
    - String barrierId
    - BarrierStatus status
    - List<BarrierExecution> auditHistory

    + Barrier(String barrierId)
    + String getBarrierId()
    + BarrierStatus getStatus()
    + boolean isRaised()
    + void raise()
    + void lower()
    + void recordExecution(BarrierExecution execution)
    + List<BarrierExecution> getAuditHistory()
}

interface BarrierCommand {
    + void execute(Operator operator)
    + String getCommandName()
}

class OpenBarrierCommand implements BarrierCommand {
    - Barrier barrier

    + OpenBarrierCommand(Barrier barrier)
    + void execute(Operator operator)
    + String getCommandName()
}

class CloseBarrierCommand implements BarrierCommand {
    - Barrier barrier

    + CloseBarrierCommand(Barrier barrier)
    + void execute(Operator operator)
    + String getCommandName()
}

// ============================================================================
// VEHICLE & PARKING SPOT DOMAIN
// ============================================================================

class Vehicle {
    - String registrationNumber
    - VehicleType vehicleType
    - String ownerPhone

    + Vehicle(String registrationNumber, VehicleType vehicleType, String ownerPhone)
    + String getRegistrationNumber()
    + VehicleType getVehicleType()
    + String getOwnerPhone()
}

class ParkingSpot {
    - String spotId
    - int floorNumber
    - ParkingSpotType spotType
    - ParkingSpotStatus status
    - Coordinates coordinates
    - Vehicle parkedVehicle

    + ParkingSpot(String spotId, int floorNumber, ParkingSpotType spotType, Coordinates coordinates)
    + String getSpotId()
    + int getFloorNumber()
    + ParkingSpotType getSpotType()
    + ParkingSpotStatus getStatus()
    + Coordinates getCoordinates()
    + Vehicle getParkedVehicle()
    + boolean isAvailable()
    + boolean canAccommodate(VehicleType vehicleType)
    + void assignVehicle(Vehicle vehicle)
    + void vacate()
}

class ParkingFloor {
    - int floorNumber
    - List<ParkingSpot> spots

    + ParkingFloor(int floorNumber)
    + int getFloorNumber()
    + List<ParkingSpot> getSpots()
    + void addSpot(ParkingSpot spot)
    + List<ParkingSpot> getAvailableSpots(ParkingSpotType spotType)
}

class Gate {
    - String gateId
    - GateType gateType
    - int floorNumber
    - Coordinates coordinates
    - Barrier barrier
    - Operator currentOperator

    + Gate(String gateId, GateType gateType, int floorNumber, Coordinates coordinates, Barrier barrier, Operator currentOperator)
    + String getGateId()
    + GateType getGateType()
    + int getFloorNumber()
    + Coordinates getCoordinates()
    + Barrier getBarrier()
    + Operator getCurrentOperator()
    + void assignOperator(Operator operator)
}

// ============================================================================
// TICKET DOMAIN
// ============================================================================

class Ticket {
    - String ticketNumber
    - Instant entryTime
    - Vehicle vehicle
    - ParkingSpot assignedSpot
    - Gate entryGate
    - Operator issuedBy
    - TicketStatus status

    + Ticket(String ticketNumber, Instant entryTime, Vehicle vehicle, ParkingSpot assignedSpot, Gate entryGate, Operator issuedBy)
    + String getTicketNumber()
    + Instant getEntryTime()
    + Vehicle getVehicle()
    + ParkingSpot getAssignedSpot()
    + Gate getEntryGate()
    + Operator getIssuedBy()
    + TicketStatus getStatus()
    + void markPaid()
}

// ============================================================================
// ITEMIZED BILLING & VALUE-ADDED AMENITIES (Decorator Pattern)
// ============================================================================

interface BillableItem {
    + double getCost()
    + List<String> getItemizedLines()
}

class BaseParkingCharge implements BillableItem {
    - double cost
    - String description

    + BaseParkingCharge(double cost, String description)
    + double getCost()
    + List<String> getItemizedLines()
}

abstract class BillableItemDecorator implements BillableItem {
    # BillableItem wrappedItem

    + BillableItemDecorator(BillableItem wrappedItem)
    + double getCost()
    + List<String> getItemizedLines()
    # abstract double getAdditionalCost()
    # abstract String getItemizedLine()
}

interface AmenityService {
    + String getServiceName()
    + double getPrice()
}

class EvChargingService implements AmenityService {
    - double kwhConsumed
    - double ratePerKwh

    + EvChargingService(double kwhConsumed, double ratePerKwh)
    + String getServiceName()
    + double getPrice()
}

class CarWashService implements AmenityService {
    - WashPackage washPackage
    - double packagePrice

    + CarWashService(WashPackage washPackage, double packagePrice)
    + String getServiceName()
    + double getPrice()
}

class AmenityServiceDecorator extends BillableItemDecorator {
    - AmenityService amenityService

    + AmenityServiceDecorator(BillableItem wrappedItem, AmenityService amenityService)
    # double getAdditionalCost()
    # String getItemizedLine()
}

class PaymentTransaction {
    - String transactionId
    - String billId
    - double amount
    - PaymentMode paymentMode
    - PaymentStatus status
    - String gatewayReferenceId
    - Instant transactionTime
    - Operator processedBy

    + PaymentTransaction(String transactionId, String billId, double amount, PaymentMode paymentMode, PaymentStatus status, String gatewayReferenceId, Instant transactionTime, Operator processedBy)
    + String getTransactionId()
    + String getBillId()
    + double getAmount()
    + PaymentMode getPaymentMode()
    + PaymentStatus getStatus()
    + String getGatewayReferenceId()
    + Instant getTransactionTime()
    + Operator getProcessedBy()
}

class Bill {
    - String billId
    - Ticket ticket
    - Instant exitTime
    - long billableHours
    - BillableItem billableItem
    - Gate exitGate
    - Operator processedBy
    - BillStatus status
    - List<PaymentTransaction> transactions

    + Bill(String billId, Ticket ticket, Instant exitTime, long billableHours, BillableItem billableItem, Gate exitGate, Operator processedBy)
    + String getBillId()
    + Ticket getTicket()
    + Instant getExitTime()
    + long getBillableHours()
    + BillableItem getBillableItem()
    + double getTotalAmount()
    + List<String> getItemizedBreakdown()
    + Gate getExitGate()
    + Operator getProcessedBy()
    + BillStatus getStatus()
    + List<PaymentTransaction> getTransactions()
    + void addPaymentTransaction(PaymentTransaction transaction)
    + boolean isSettled()
}

// ============================================================================
// SPOT ASSIGNMENT STRATEGY (Strategy Pattern)
// ============================================================================

interface ParkingAssignmentStrategy {
    + ParkingSpot findSpot(ParkingLot parkingLot, Gate entryGate, VehicleType vehicleType)
}

class NearestAvailableSpotStrategy implements ParkingAssignmentStrategy {
    + NearestAvailableSpotStrategy()
    + ParkingSpot findSpot(ParkingLot parkingLot, Gate entryGate, VehicleType vehicleType)
    - double computeSpatialDistance(Gate entryGate, ParkingSpot spot)
}

// ============================================================================
// TARIFF & FEE CALCULATION STRATEGY (Strategy / Template Method Pattern)
// ============================================================================

interface FeeCalculationStrategy {
    + double calculateFee(Ticket ticket, Instant exitTime)
    + long calculateBillableHours(Instant entryTime, Instant exitTime)
}

abstract class SlabBasedFeeStrategy implements FeeCalculationStrategy {
    # Map<VehicleType, List<PricingSlab>> slabConfiguration

    + SlabBasedFeeStrategy(Map<VehicleType, List<PricingSlab>> slabConfiguration)
    + double calculateFee(Ticket ticket, Instant exitTime)
    + long calculateBillableHours(Instant entryTime, Instant exitTime)
    # abstract double computeSlabFee(VehicleType type, long billableHours)
}

class TieredHourlyFeeStrategy extends SlabBasedFeeStrategy {
    + TieredHourlyFeeStrategy()
    + TieredHourlyFeeStrategy(Map<VehicleType, List<PricingSlab>> customSlabs)
    # double computeSlabFee(VehicleType type, long billableHours)
}

class FlatRateFeeStrategy extends SlabBasedFeeStrategy {
    - Map<VehicleType, double> flatHourlyRates

    + FlatRateFeeStrategy(Map<VehicleType, double> flatHourlyRates)
    # double computeSlabFee(VehicleType type, long billableHours)
}

// ============================================================================
// PAYMENT GATEWAY & ADAPTER PATTERN (Razorpay Integration)
// ============================================================================

interface PaymentProcessor {
    + PaymentTransaction processPayment(String billId, double amount, PaymentMode mode, Operator operator)
}

class ExternalRazorpayClient {
    - String apiKey
    - String apiSecret

    + ExternalRazorpayClient(String apiKey, String apiSecret)
    + String createOrder(long amountInPaise, String currency)
    + String capturePayment(String orderId, long amountInPaise)
    + boolean verifySignature(String orderId, String paymentId, String signature)
}

class RazorpayPaymentAdapter implements PaymentProcessor {
    - ExternalRazorpayClient razorpayClient

    + RazorpayPaymentAdapter(ExternalRazorpayClient razorpayClient)
    + PaymentTransaction processPayment(String billId, double amount, PaymentMode mode, Operator operator)
    - long convertRupeesToPaise(double rupees)
}

class CashPaymentProcessor implements PaymentProcessor {
    + CashPaymentProcessor()
    + PaymentTransaction processPayment(String billId, double amount, PaymentMode mode, Operator operator)
}

class PaymentProcessorFactory {
    - Map<PaymentMode, PaymentProcessor> registry

    + PaymentProcessorFactory(Map<PaymentMode, PaymentProcessor> processors)
    + void registerProcessor(PaymentMode mode, PaymentProcessor processor)
    + PaymentProcessor getPaymentProcessor(PaymentMode mode)
}

// ============================================================================
// EVENT-DRIVEN NOTIFICATIONS (Observer Pattern)
// ============================================================================

abstract class ParkingEvent {
    # String eventId
    # Instant timestamp

    + ParkingEvent(String eventId, Instant timestamp)
    + String getEventId()
    + Instant getTimestamp()
}

class VehicleParkedEvent extends ParkingEvent {
    - Ticket ticket

    + VehicleParkedEvent(String eventId, Instant timestamp, Ticket ticket)
    + Ticket getTicket()
}

class VehicleExitedEvent extends ParkingEvent {
    - Bill bill

    + VehicleExitedEvent(String eventId, Instant timestamp, Bill bill)
    + Bill getBill()
}

interface ParkingEventListener {
    + void onEvent(ParkingEvent event)
}

class DisplayBoardListener implements ParkingEventListener {
    - Map<ParkingSpotType, Integer> availableCountMap

    + DisplayBoardListener()
    + void onEvent(ParkingEvent event)
    + int getAvailableCount(ParkingSpotType type)
}

class NotificationListener implements ParkingEventListener {
    + NotificationListener()
    + void onEvent(ParkingEvent event)
    + void sendTicketSms(String phone, Ticket ticket)
    + void sendBillReceipt(String phone, Bill bill)
}

class ParkingEventPublisher {
    - List<ParkingEventListener> listeners

    + ParkingEventPublisher()
    + void subscribe(ParkingEventListener listener)
    + void unsubscribe(ParkingEventListener listener)
    + void publish(ParkingEvent event)
}

// ============================================================================
// SUBSYSTEM CONTROLLERS & DELEGATES (Single Responsibility Principle)
// ============================================================================

class TicketManager {
    - Map<String, Ticket> activeTickets

    + TicketManager()
    + void registerTicket(Ticket ticket)
    + Ticket getActiveTicketOrThrow(String ticketNumber)
    + Ticket closeTicket(String ticketNumber)
}

class GateController {
    - Map<String, Gate> gates

    + GateController(List<Gate> gateList)
    + Gate getValidatedGate(String gateId, GateType expectedType)
    + void openBarrier(String gateId, Operator operator)
    + void closeBarrier(String gateId, Operator operator)
    + void cycleGate(String gateId, Operator operator)
}

class BillingService {
    - FeeCalculationStrategy feeCalculationStrategy

    + BillingService(FeeCalculationStrategy feeCalculationStrategy)
    + Bill generateBill(Ticket ticket, Instant exitTime, Gate exitGate, Operator operator)
    + Bill generateBillWithAmenities(Ticket ticket, Instant exitTime, Gate exitGate, Operator operator, List<AmenityService> amenities)
}

class PaymentService {
    - PaymentProcessorFactory paymentProcessorFactory

    + PaymentService(PaymentProcessorFactory paymentProcessorFactory)
    + PaymentTransaction processBillPayment(Bill bill, PaymentMode mode, Operator operator)
}

// ============================================================================
// PARKING LOT FACADE (Main System Entry Point)
// ============================================================================

class ParkingLot {
    - String lotId
    - List<ParkingFloor> floors
    - GateController gateController
    - TicketManager ticketManager
    - BillingService billingService
    - PaymentService paymentService
    - ParkingAssignmentStrategy assignmentStrategy
    - ParkingEventPublisher eventPublisher

    + ParkingLot(String lotId, List<ParkingFloor> floors, GateController gateController, TicketManager ticketManager, BillingService billingService, PaymentService paymentService, ParkingAssignmentStrategy assignmentStrategy, ParkingEventPublisher eventPublisher)
    + Ticket parkVehicle(String entryGateId, Vehicle vehicle, Operator operator)
    + Bill processExit(String exitGateId, String ticketNumber, Operator operator)
    + Bill processExitWithAmenities(String exitGateId, String ticketNumber, Operator operator, List<AmenityService> amenities)
    + PaymentTransaction settlePayment(Bill bill, PaymentMode mode, Operator operator)
    + void setAssignmentStrategy(ParkingAssignmentStrategy assignmentStrategy)
    + void setFeeCalculationStrategy(FeeCalculationStrategy feeStrategy)
    + void subscribeToEvents(ParkingEventListener listener)
    + int getAvailableSpotCount(ParkingSpotType type)
}

// ============================================================================
// DESIGN PATTERN JUSTIFICATION & ARCHITECTURAL SUMMARY
// ============================================================================
// 1. Facade Pattern:
//    - ParkingLot acts as the unified facade orchestrating dedicated sub-domain delegates
//      (GateController, TicketManager, BillingService, PaymentService, ParkingEventPublisher),
//      preventing the core domain from collapsing into a God Object.
//
// 2. Strategy Pattern (Spot Assignment):
//    - ParkingAssignmentStrategy decouples the spot search algorithm from the ParkingLot and Gate domain.
//    - NearestAvailableSpotStrategy calculates Euclidean/Manhattan distance + floor elevation delta relative
//      to the entry gate. If business rules change (e.g., Farthest Spot, VIP Priority, Best Fit, Random),
//      new strategies can be plugged in seamlessly at runtime without modifying ParkingLot.
//
// 3. Strategy Pattern + Template Method Pattern (Tariff / Fee Calculation):
//    - FeeCalculationStrategy isolates mathematical billing algorithms from Ticket and Bill settlement.
//    - SlabBasedFeeStrategy defines the invariant skeleton (calculateFee, ceiling round-up Duration -> hours)
//      and delegates slab accumulation to computeSlabFee() in subclasses.
//    - TieredHourlyFeeStrategy accumulates charges across sequential hourly slabs with ceiling round-up
//      (e.g., 5.5 hours -> 6 billable hours: 2*25 + 2*30 + 2*35 = Rs. 180 for CAR).
//    - Accommodates future pricing models (e.g., FlatRateFeeStrategy, PeakHoursSurgeStrategy) complying fully with OCP.
//
// 4. Command Pattern (Hardware Actuation):
//    - BarrierCommand (OpenBarrierCommand, CloseBarrierCommand) encapsulates physical gate movements,
//      decoupling gate hardware from gate controllers while maintaining a complete BarrierExecution audit log
//      of which Operator triggered barrier movement.
//
// 5. Decorator Pattern (Itemized Billing & Amenities):
//    - BillableItem is dynamically wrapped by AmenityServiceDecorator around BaseParkingCharge, allowing
//      optional value-added services (EV charging, car wash) to compose recursively into transparent,
//      itemized invoices without polluting the core parking charge logic.
//
// 6. Adapter Pattern (Razorpay Integration):
//    - ExternalRazorpayClient is a vendor SDK requiring amount in paise (1 INR = 100 paise) and order verification.
//    - RazorpayPaymentAdapter implements the domain's PaymentProcessor port, shielding the core system from
//      vendor-specific signatures, data types, and API contracts.
//
// 7. Registry Pattern (PaymentProcessorFactory):
//    - Maps PaymentMode (RAZORPAY_UPI, RAZORPAY_CARD, CASH) to PaymentProcessor abstractions, enabling
//      zero-coupling registration of alternative payment rails (e.g., Stripe, Paytm).
//
// 8. Observer Pattern (Real-Time Event Decoupling):
//    - ParkingEventPublisher broadcasts VehicleParkedEvent and VehicleExitedEvent asynchronously.
//    - DisplayBoardListener updates live LED occupancy displays across floors, and NotificationListener
//      dispatches SMS tickets/invoices to vehicle drivers without coupling to ParkingLot.
//
// 9. Operator Accountability & Audit Logging:
//    - Operators are captured at every state transition: on Ticket generation (entry gate attendant),
//      on Bill calculation (exit gate attendant), and on PaymentTransaction (cashier / auditing record).
//
// 10. Strict Spot Type Compatibility:
//    - ParkingSpotType (CAR, BIKE, TRUCK) strictly maps 1:1 to VehicleType. canAccommodate() guarantees
//      that a CAR spot cannot be assigned to BIKEs, and a BIKE spot cannot host TRUCKs.


// ============================================================================
// RELATIONAL DATABASE SCHEMA (SQL DDL & REFERENTIAL INTEGRITY)
// ============================================================================
/*
-- -----------------------------------------------------------------------------
-- 1. PARKING LOT & INFRASTRUCTURE TABLES
-- -----------------------------------------------------------------------------
CREATE TABLE parking_lots (
    parking_lot_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(255) NOT NULL,
    total_floors INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE parking_floors (
    floor_id VARCHAR(64) PRIMARY KEY,
    parking_lot_id VARCHAR(64) NOT NULL REFERENCES parking_lots(parking_lot_id) ON DELETE CASCADE,
    floor_number INT NOT NULL,
    capacity INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_floor_per_lot UNIQUE (parking_lot_id, floor_number)
);

CREATE TABLE parking_spots (
    spot_id VARCHAR(64) PRIMARY KEY,
    floor_id VARCHAR(64) NOT NULL REFERENCES parking_floors(floor_id) ON DELETE CASCADE,
    spot_number VARCHAR(32) NOT NULL,
    spot_type VARCHAR(32) NOT NULL CHECK (spot_type IN ('CAR', 'BIKE', 'TRUCK')),
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'OUT_OF_SERVICE')),
    coordinate_x DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    coordinate_y DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_spot_per_floor UNIQUE (floor_id, spot_number)
);

CREATE INDEX idx_parking_spots_status_type ON parking_spots(floor_id, spot_type, status);

-- -----------------------------------------------------------------------------
-- 2. USERS, OPERATORS & GATES
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    user_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    email VARCHAR(128) NOT NULL UNIQUE,
    phone_number VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE operators (
    operator_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL UNIQUE REFERENCES users(user_id) ON DELETE RESTRICT,
    employee_id VARCHAR(64) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE gates (
    gate_id VARCHAR(64) PRIMARY KEY,
    parking_lot_id VARCHAR(64) NOT NULL REFERENCES parking_lots(parking_lot_id) ON DELETE CASCADE,
    gate_name VARCHAR(64) NOT NULL,
    gate_type VARCHAR(16) NOT NULL CHECK (gate_type IN ('ENTRY', 'EXIT')),
    coordinate_x DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    coordinate_y DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    barrier_status VARCHAR(16) NOT NULL DEFAULT 'LOWERED' CHECK (barrier_status IN ('RAISED', 'LOWERED')),
    current_operator_id VARCHAR(64) REFERENCES operators(operator_id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE barrier_executions (
    execution_id VARCHAR(64) PRIMARY KEY,
    gate_id VARCHAR(64) NOT NULL REFERENCES gates(gate_id) ON DELETE CASCADE,
    operator_id VARCHAR(64) NOT NULL REFERENCES operators(operator_id) ON DELETE RESTRICT,
    command_action VARCHAR(32) NOT NULL CHECK (command_action IN ('OPEN', 'CLOSE')),
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_successful BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_barrier_executions_gate ON barrier_executions(gate_id, executed_at DESC);

-- -----------------------------------------------------------------------------
-- 3. VEHICLES, SESSIONS, TICKETS & BILLING
-- -----------------------------------------------------------------------------
CREATE TABLE vehicles (
    vehicle_id VARCHAR(64) PRIMARY KEY,
    registration_number VARCHAR(32) NOT NULL UNIQUE,
    vehicle_type VARCHAR(32) NOT NULL CHECK (vehicle_type IN ('CAR', 'BIKE', 'TRUCK')),
    color VARCHAR(32),
    model VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tickets (
    ticket_id VARCHAR(64) PRIMARY KEY,
    ticket_number VARCHAR(64) NOT NULL UNIQUE,
    vehicle_id VARCHAR(64) NOT NULL REFERENCES vehicles(vehicle_id) ON DELETE RESTRICT,
    parking_spot_id VARCHAR(64) NOT NULL REFERENCES parking_spots(spot_id) ON DELETE RESTRICT,
    entry_gate_id VARCHAR(64) NOT NULL REFERENCES gates(gate_id) ON DELETE RESTRICT,
    entry_operator_id VARCHAR(64) NOT NULL REFERENCES operators(operator_id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAID', 'CANCELLED')),
    entry_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tickets_vehicle_active ON tickets(vehicle_id, status);
CREATE INDEX idx_tickets_spot_active ON tickets(parking_spot_id, status);

CREATE TABLE bills (
    bill_id VARCHAR(64) PRIMARY KEY,
    bill_number VARCHAR(64) NOT NULL UNIQUE,
    ticket_id VARCHAR(64) NOT NULL UNIQUE REFERENCES tickets(ticket_id) ON DELETE RESTRICT,
    exit_gate_id VARCHAR(64) NOT NULL REFERENCES gates(gate_id) ON DELETE RESTRICT,
    exit_operator_id VARCHAR(64) NOT NULL REFERENCES operators(operator_id) ON DELETE RESTRICT,
    base_parking_charge NUMERIC(10, 2) NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID', 'PAID')),
    exit_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bill_amenities (
    amenity_item_id VARCHAR(64) PRIMARY KEY,
    bill_id VARCHAR(64) NOT NULL REFERENCES bills(bill_id) ON DELETE CASCADE,
    amenity_type VARCHAR(64) NOT NULL CHECK (amenity_type IN ('EV_CHARGING', 'CAR_WASH', 'DETAILING')),
    charge_amount NUMERIC(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_transactions (
    transaction_id VARCHAR(64) PRIMARY KEY,
    bill_id VARCHAR(64) NOT NULL REFERENCES bills(bill_id) ON DELETE RESTRICT,
    transaction_reference VARCHAR(128) NOT NULL UNIQUE,
    amount NUMERIC(10, 2) NOT NULL,
    payment_mode VARCHAR(32) NOT NULL CHECK (payment_mode IN ('RAZORPAY_UPI', 'RAZORPAY_CARD', 'RAZORPAY_NETBANKING', 'CASH')),
    payment_status VARCHAR(32) NOT NULL CHECK (payment_status IN ('SUCCESS', 'FAILED', 'PENDING')),
    processed_by_operator_id VARCHAR(64) REFERENCES operators(operator_id) ON DELETE SET NULL,
    gateway_payment_id VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_transactions_bill ON payment_transactions(bill_id, payment_status);

-- -----------------------------------------------------------------------------
-- 4. FUTURE SCOPE SCALABILITY & NON-DISRUPTIVE EXTENSIONS
-- -----------------------------------------------------------------------------
-- Example A: Seamless addition of EV Charging Infrastructure without disrupting spots table
CREATE TABLE ev_charging_stations (
    station_id VARCHAR(64) PRIMARY KEY,
    parking_spot_id VARCHAR(64) NOT NULL UNIQUE REFERENCES parking_spots(spot_id) ON DELETE CASCADE,
    charger_type VARCHAR(32) NOT NULL CHECK (charger_type IN ('CCS2', 'TYPE_2', 'CHADEMO')),
    max_kw_output NUMERIC(6, 2) NOT NULL DEFAULT 22.0,
    rate_per_kwh NUMERIC(8, 2) NOT NULL,
    is_operational BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Example B: Dynamic Surge / Congestion Pricing Config without breaking pricing models
CREATE TABLE surge_pricing_rules (
    rule_id VARCHAR(64) PRIMARY KEY,
    parking_lot_id VARCHAR(64) NOT NULL REFERENCES parking_lots(parking_lot_id) ON DELETE CASCADE,
    occupancy_threshold_percentage NUMERIC(5, 2) NOT NULL,
    surge_multiplier NUMERIC(4, 2) NOT NULL DEFAULT 1.25,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL
);
*/
