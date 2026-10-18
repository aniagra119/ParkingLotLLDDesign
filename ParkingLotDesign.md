# Parking Lot System — Architectural Decisions & Revision Guide

This document captures the architectural trade-offs, design patterns, and engineering decisions established during the design of this multi-floor parking lot system. Use this as a reference for interview prep and system design revision.

---

## 1. Architectural Decisions & Trade-Off Matrix

### Decision 1: The Facade Pattern vs. The "Train Wreck" Anti-Pattern (Law of Demeter)

#### The Tempting Anti-Pattern:
```java
// Leaking internal manager chains to the outside world
entryGate.getParkingLot().getTicketManager().createTicket(
    entryGate.getParkingLot().getAllotmentManager().findSpot(...)
);
```

#### Why this breaks down:
1. **Law of Demeter Violation**: *"A method should only call methods on its immediate dependencies, not reach through a chain of accessors."*
2. **Leaked Orchestration Logic**: To check in a car, you must query an allotment strategy, reserve the spot, mark it occupied, and generate a ticket. If `EntryGate` coordinates these steps, the business workflow leaks into the UI/hardware layer.
3. **Fragile Coupling**: If the ticket generation signature or spot reservation contract changes, every gate class breaks.

#### The Architectural Solution: `ParkingLot` as a Facade
```java
// EntryGate only talks to its direct friend:
public Ticket processEntry(Vehicle vehicle) {
    return parkingLot.parkVehicle(this.gateId, vehicle);
}
```
Inside `ParkingLot.parkVehicle(...)`, the lot acts as a **Domain Service / Facade**, orchestrating internal spot lookup, state transitions, and ticket emission. The gate stays completely decoupled.

---

### Decision 2: Gate Modeling — Compile-Time Safety vs. Dynamic Lane Reconfiguration

#### The Trade-Off:
In real life, parking lots have different lane configurations:
* **Scenario A (Fixed Physical Infrastructure)**: Gates have one-way spike strips, barrier arms, and ticket dispensers mounted specifically for incoming or outgoing cars.
* **Scenario B (Dynamic Reversible Lanes)**: At 9:00 AM (morning rush), Lane 3 is configured for ENTRY. At 5:00 PM (evening rush), an operator flips a switch, and Lane 3 dynamically operates as an EXIT lane.

#### Three Ways to Model This:

| Approach | Pattern | Pros | Cons |
| :--- | :--- | :--- | :--- |
| **1. Dedicated Subclasses** (`EntryGate`, `ExitGate`) | Clean Inheritance | **Compile-time type safety**. The compiler physically prevents calling `exitGate.processEntry(car)`. | Cannot dynamically transform a gate from entry to exit at runtime. |
| **2. Capability Interfaces** (`EntryCapable`, `ExitCapable`) | Interface Segregation (ISP) | Compile-time safety + supports a `BidirectionalGate` implementing both interfaces. | Static; cannot switch lane mode at runtime without object recreation. |
| **3. Mode / State Pattern** (`Gate` with `GateModeState`) | GoF State Pattern | **100% dynamic runtime lane flipping** without recompilation or redeployment. | Requires runtime exception guards when a lane mode is inactive. |

* **Project Consensus**: For standard interview specifications with dedicated gates, dedicated classes (`EntryGate`, `ExitGate`) provide maximum compile-time safety and zero runtime exceptions.

---

### Decision 3: Spatial Decomposition — YAGNI on `Space` vs. `Floor` & Zoning

#### The Over-Abstraction Trap:
Introducing a generic `Space` class (`Space has-a Optional<Floor>, SpaceType, List<ParkingSpot>`) before needed violates **YAGNI (You Aren't Gonna Need It)**.

#### The Clean Solution:
* The building has `ParkingFloor`s containing `ParkingSpot`s.
* Real-world **Zoning** (e.g., *Floor 1 - North Wing (close to mall)* vs. *Floor 1 - South Wing (far)*) does **not** require extra entity wrappers.
* Instead, **spatial coordinates** (floor number, slot index, distance from gate) are attributes on the `ParkingSpot`.
* The **`ParkingAssignmentStrategy`** uses those coordinates to decide allocation (e.g., filling near spots during low traffic, or filling far spots during peak rush).

---

### Decision 4: Lifecycle Separation — `Ticket` (Entry) vs. `Bill` (Exit)

#### The Nullable Fields Anti-Pattern:
Combining entry and exit data into a single `Ticket` class:
```java
class Ticket {
    LocalDateTime entryTime;
    LocalDateTime exitTime;    // NULL at entry!
    Bill bill;                 // NULL at entry!
    Payment payment;           // NULL at entry!
}
```
Having core domain entities filled with null fields that only get populated hours later is a major domain modeling code smell.

#### The Clean Domain Boundary:
1. **`Ticket` (The Entry Fact)**:
   - Created at check-in.
   - Encapsulates: `ticketId`, `vehicleNumber`, `vehicleType`, `assignedSpot`, `entryTime`, `entryGateId`, `TicketStatus (ACTIVE, COMPLETED)`.
   - Immutable once issued.
2. **`Bill` / `Invoice` (The Exit Settlement)**:
   - Created at check-out.
   - Encapsulates: `billId`, `ticketReference`, `exitTime`, `durationHours`, `totalAmount`, `BillStatus (UNPAID, PAID)`.
3. **`Payment` (The Financial Transaction)**:
   - Emitted upon gateway execution.
   - Encapsulates: `transactionId`, `amount`, `paymentMode (UPI, CARD, CASH)`, `paymentStatus (SUCCESS, FAILED)`.

---

### Decision 5: Pluggable Strategies (Open/Closed Principle)

The requirements explicitly mandate that both **spot assignment** and **fee calculation** may change in the future.

#### 1. `ParkingAssignmentStrategy` (Strategy Pattern)
```java
public interface ParkingAssignmentStrategy {
    ParkingSpot findSpot(ParkingLot lot, Gate entryGate, VehicleType vehicleType);
}
```
* **`NearestAvailableSpotStrategy`**: Evaluates available spots by computing spatial distance:
  $$\text{Distance} = (\text{FloorDifference} \times \text{Weight}) + \text{SlotOffset}$$
* Extensibility: Adding `EcoVehicleSpotStrategy` or `VIPSpotStrategy` requires zero modifications to existing lot code.

#### 2. `FeeCalculationStrategy` (Strategy Pattern)
```java
public interface FeeCalculationStrategy {
    double calculateFee(Ticket ticket, LocalDateTime exitTime);
}
```
* **`TieredHourlyFeeStrategy`**: Implements the progressive slab pricing model with ceiling hour calculation:
  $$\text{Billable Hours} = \lceil \text{Duration in Hours} \rceil$$
* Extensibility: Adding weekend surge rates, flat-rate event passes, or free 15-minute grace periods is isolated into separate strategy implementations.

---

### Decision 6: Payment Gateway Integration (Adapter Pattern)

#### The Problem: Vendor Lock-In
Importing third-party SDKs directly (`import com.razorpay.RazorpayClient;`) into domain services welds your business code to external API schemas.

#### The Solution: The Adapter Pattern
```
[ExitGate / Billing] ---> (PaymentProcessor Interface)
                                    ▲
                                    │ implements
                         [RazorpayPaymentAdapter]
                                    │ wraps
                         [External RazorpayClient SDK]
```
If the business decides to switch to Stripe, Paytm, or Cash on Delivery, only a new adapter class is added without altering any billing domain logic.

---

## 2. Deep Dive: The GoF State Pattern & Classic Scenarios

### What is the State Pattern?
The State Pattern allows an object to alter its behavior when its internal state changes. The object appears to change its class by delegating behavior to an interface representing its current state.

### The Problem It Solves: `if/else` Flag Pollution
Without the State pattern, classes with lifecycle phases (e.g. an ATM or Order) end up with massive, repetitive `if/else` or `switch` blocks inside *every single method*:
```java
// Anti-Pattern: Primitive Flag Spaghetti
if (state == NO_CARD) { ... }
else if (state == HAS_CARD) { ... }
else if (state == PIN_ENTERED) { ... }
```
This violates:
1. **Single Responsibility Principle**: The context class contains the business logic for every state.
2. **Open/Closed Principle**: Adding a new state (`FRAUD_SUSPECTED`) requires editing every single method in the class.

### Classic Real-World Scenarios for the State Pattern:
1. **Vending Machine**: `NoCoinState` $\to$ `HasCoinState` $\to$ `DispensingState` $\to$ `SoldOutState`. (Cannot dispense without coins, cannot refund while dispensing).
2. **ATM Machine**: `NoCardState` $\to$ `HasCardState` $\to$ `PinEnteredState` $\to$ `CashDispensedState`.
3. **E-Commerce Order Lifecycle**: `Created` $\to$ `PaymentPending` $\to$ `Processing` $\to$ `Shipped` $\to$ `Delivered` (or `Cancelled`). An order in `Shipped` cannot be cancelled directly by the user.
4. **Seat Reservation (BookMyShow)**: `AVAILABLE` $\to$ `TEMPORARILY_LOCKED` $\to$ `BOOKED` (or timeout rolls back to `AVAILABLE`).
5. **Reconfigurable Highway Toll / Gate Lanes**: `EntryModeState` $\to$ `ExitModeState` $\to$ `MaintenanceModeState`. Allows dynamic 5:00 PM peak-rush lane role reversal at runtime without restarting servers.

### State Pattern vs. Strategy Pattern:

| Dimension | Strategy Pattern | State Pattern |
| :--- | :--- | :--- |
| **Intent** | Swap an **algorithm** (e.g., QuickSort vs MergeSort, UPI vs Card, Nearest vs Distributed Spot). | Model an object whose **lifecycle behavior changes dynamically** over time. |
| **Who initiates change?** | The **client** usually chooses which Strategy to inject at initialization. | The **states themselves** trigger transitions to subsequent states automatically as actions occur. |
| **Mutual Knowledge** | Strategies have zero awareness of each other. | States often know and instantiate the next valid state. |

---

## 3. Advanced Gate Architecture: Role Interfaces & Multi-Invoker Command Pattern

### 1. Role-Based Gate Interfaces (Interface Segregation Principle)
To support dedicated entry gates, dedicated exit gates, and bidirectional gates cleanly without runtime exceptions:
```java
public interface EntryCapable {
    Ticket processEntry(Vehicle vehicle);
}

public interface ExitCapable {
    Bill processExit(Ticket ticket);
}

public abstract class Gate {
    protected final String gateId;
    protected Operator currentOperator;
}

public class EntryGate extends Gate implements EntryCapable { ... }
public class ExitGate extends Gate implements ExitCapable { ... }
public class BidirectionalGate extends Gate implements EntryCapable, ExitCapable { ... }
```

### 2. Multi-Invoker Decoupling (The Command Pattern for Entries)
In modern automated parking, the trigger for an entry is not solely a guard pressing a button:
* **Invokers**:
  - `GuardConsoleTerminal` (Manual UI entry)
  - `AnprCameraScanner` (Automatic Number Plate Recognition)
  - `RfidFastagReader` (Passive windshield toll transponder)
  - `MagneticGroundLoop` (Inductive metal sensor under asphalt)
* **Command Encapsulation**:
  `IssueTicketCommand(EntryCapable gate, Vehicle vehicle)` encapsulates the action so any hardware sensor or user interface can execute entry without coupling to internal lot orchestration.

---

## 4. Deep Dive: Strategy + Decorator Billing Pipeline

### The Problem: Combining Dynamic Slabs with Optional Add-Ons
A customer's final invoice often consists of two distinct parts:
1. **The Base Parking Fee**: Calculated based on duration (e.g. 5.5 hrs $\to$ 6 billable hours), vehicle type, and progressive hourly rate slabs.
2. **Optional Value-Added Services**: Extra amenities chosen during the stay (e.g., EV Charging, Car Wash, Valet Service, Discount Vouchers).

### The Architectural Collaboration:

```
[Ticket & ExitTime] 
         │
         │ 1. Strategy Pattern computes base time & slabs
         ▼
[FeeCalculationStrategy (TieredHourlyFeeStrategy)]
         │
         │ Output: Base Parking Fee = ₹180.0
         ▼
[BaseParkingCharge (₹180.0)]
         │
         │ 2. Decorator Pattern wraps optional add-on services!
         ▼
[EvChargingDecorator (+ ₹150.0)]
         │
         ▼
[CarWashDecorator (+ ₹250.0)]
         │
         │ Output: Final Itemized Bill = ₹580.0
         ▼
[Final Bill Settlement]
```

### 1. Strategy Pattern Role
Answers: *"How is time translated into cost for this vehicle?"*
* Encapsulates the progressive tiered slabs.
* Outputs the raw base charge: `double baseAmount = feeStrategy.calculateFee(ticket, exitTime);` (e.g. ₹180.0).

### 2. Decorator Pattern Role
Answers: *"How do we layer optional amenities without a combinatorial $2^N$ subclass explosion?"*
* Defines a uniform interface:
  ```java
  public interface BillableItem {
      double getCost();
      String getDescription();
  }
  ```
* **Base Component**:
  ```java
  public class BaseParkingCharge implements BillableItem {
      private final double amount;
      public BaseParkingCharge(double amount) { this.amount = amount; }
      public double getCost() { return amount; }
      public String getDescription() { return "Parking Fee: ₹" + amount; }
  }
  ```
* **Decorators**:
  ```java
  public abstract class BillableItemDecorator implements BillableItem {
      protected final BillableItem wrapped;
      public BillableItemDecorator(BillableItem wrapped) { this.wrapped = wrapped; }
  }

  public class EvChargingDecorator extends BillableItemDecorator {
      public EvChargingDecorator(BillableItem wrapped) { super(wrapped); }
      public double getCost() { return wrapped.getCost() + 150.0; }
      public String getDescription() { return wrapped.getDescription() + " + EV Charging (₹150)"; }
  }

  public class CarWashDecorator extends BillableItemDecorator {
      public CarWashDecorator(BillableItem wrapped) { super(wrapped); }
      public double getCost() { return wrapped.getCost() + 250.0; }
      public String getDescription() { return wrapped.getDescription() + " + Car Wash (₹250)"; }
  }
  ```

---

## 5. Factory Pattern Analysis: When is it Useful vs. Over-Engineered?

### When Factory is an Anti-Pattern / Pointless Boilerplate:
If an entity like `ParkingSpot` or `Vehicle` does not have polymorphic subclasses, creating a factory that merely executes:
```java
// ❌ Pointless Pass-Through Boilerplate
public static ParkingSpot createSpot(...) {
    return new ParkingSpot(...);
}
```
...adds zero architectural value and violates the YAGNI principle.

### When Factory IS Legitimate in this System:
1. **Parameterized `PaymentProcessorFactory`**:
   - Takes a `PaymentMode` enum (`RAZORPAY_UPI`, `RAZORPAY_CARD`, `CASH`) and instantiates the correct adapter implementing `PaymentProcessor`.
2. **`PricingSlabFactory`**:
   - Decouples default rate slab tables from strategy logic, providing canonical rate slabs for `BIKE`, `CAR`, and `TRUCK`.
