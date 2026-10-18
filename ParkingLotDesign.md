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
