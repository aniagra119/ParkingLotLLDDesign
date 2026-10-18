# Comprehensive Architectural Design & Decision Log
## Multi-Floor Parking Lot Low-Level Design (LLD)

---

## 1. System Requirements & Domain Scope

### 1.1 Problem Statement
Design a clean, extensible, multi-floor parking lot system capable of managing high-throughput entry and exit flows, dynamically assigning parking spots based on vehicle dimensions, maintaining transactional bill settlement, calculating tiered hourly fees, and supporting seamless third-party payment integration (e.g. Razorpay).

### 1.2 Core Capabilities
1. **Multi-Floor Capacity**: Multi-level floors, each containing designated spot types (`BIKE`, `CAR`, `TRUCK`).
2. **Strict 1:1 Spot Compatibility**: A car spot cannot accommodate bikes; a truck spot cannot accommodate cars. Each vehicle maps strictly to its dedicated spot type.
3. **Automated Ticket Issuance**: Generates an immutable entry record (`Ticket`) containing entry timestamp, assigned spot ID, vehicle plate, and ticket number.
4. **Spot Allocation Algorithm**: Nearest available spot across floors (calculated using floor levels and spot offsets) in $O(1)$ lookup time.
5. **Tiered Hourly Fee Math**: Progressive fee calculation with hourly ceiling rounding ($\lceil \text{hours} \rceil$).
6. **Payment & Barrier Decoupling**: Pluggable payment processors (Razorpay adapter) and physical boom barrier actuator isolation.

---

## 2. Design Patterns Applied & Rationale

| Pattern | Category | Where Applied | Concrete Architectural Justification |
| :--- | :--- | :--- | :--- |
| **Facade** | Structural | `ParkingLot` class | Hides complex subsystem orchestration (`TicketManager`, `ParkingFloorManager`, `GateManager`) from external clients, preventing Law of Demeter violations. |
| **State** | Behavioral | `GateModeState` on `Gate` | Models dynamic lane flipping (`EntryModeState`, `ExitModeState`, `MaintenanceModeState`) to handle peak traffic reversals without server reboots or flag spaghetti. |
| **Command** | Behavioral | `BarrierCommand` (`OpenBarrier`, `CloseBarrier`) | Decouples gate business logic from physical barrier hardware/actuators. Also decouples multiple hardware invokers (ANPR camera, FASTag reader, guard UI) from ticket issuance. |
| **Strategy** | Behavioral | `ParkingAssignmentStrategy` | Encapsulates spot allocation logic (`NearestAvailableSpotStrategy`, `RandomSpotStrategy`) to allow swapping search heuristics without touching floor models. |
| **Strategy** | Behavioral | `FeeCalculationStrategy` | Encapsulates progressive tiered slab mathematics (`TieredHourlyFeeStrategy`, `FlatRateStrategy`, `WeekendSurgeStrategy`). |
| **Decorator** | Structural | `BillableItemDecorator` on `BaseParkingCharge` | Dynamically wraps parking fees with optional customer amenities (`EvChargingDecorator`, `CarWashDecorator`) avoiding an exponential $2^N$ subclass explosion. |
| **Adapter** | Structural | `RazorpayPaymentAdapter` | Converts proprietary third-party SDK calls (`RazorpayClient`) into our clean domain `PaymentProcessor` contract. |
| **Factory** | Creational | `PaymentProcessorFactory` | Centralizes runtime instantiation of payment adapters based on user's chosen `PaymentMode` enum. |
| **Builder** | Creational | `ParkingLotBuilder` | Constructs complex multi-floor, multi-gate parking lot hierarchies cleanly without telescoping constructors. |

---

## 3. The Great Pattern Debate: Strategy vs. Adapter + Factory (The Common Misconception)

### 3.1 The Widespread Interview & Tutorial Mislabeling
In 90% of online tutorials, YouTube videos, and mock LLD interviews, you will see payment integration labeled as the **"Strategy Pattern"**:
```java
// What the internet commonly mislabels as "Strategy Pattern":
interface PaymentStrategy {
    void pay(double amount);
}
class UpiPaymentStrategy implements PaymentStrategy { ... }
class CreditCardPaymentStrategy implements PaymentStrategy { ... }
```
**Why do people do this?**  
Because superficially, it mirrors the *syntactic shape* of Strategy:
1. There is an interface (`PaymentStrategy`).
2. There are multiple implementations (`Upi`, `CreditCard`, `NetBanking`).
3. It can be injected at runtime into a client context (`ShoppingCart` or `Bill`).

Because many developers match the surface mechanics (an interface with polymorphism), they blindly attach the label "Strategy Pattern" to it.

### 3.2 The GoF Reality: What is Strategy vs. Adapter?

#### GoF Definition of Strategy:
> *"Define a family of **algorithms**, encapsulate each one, and make them interchangeable. Strategy lets the **algorithm** vary independently from clients that use it."*

The keyword is **algorithm** — internal computation, sorting, searching, mathematical calculation, or spatial routing:
* `Collections.sort(list, Comparator)` $\to$ `Comparator` is a true **Strategy** (pluggable sorting comparison algorithms).
* `ParkingAssignmentStrategy` (Nearest Spot vs Distributed vs Random) $\to$ True **Strategy** (spatial graph search algorithms).
* `FeeCalculationStrategy` (Tiered Slabs vs Flat Rate vs Surge) $\to$ True **Strategy** (mathematical fee computation algorithms).

#### GoF Definition of Adapter:
> *"Convert the interface of a class into another interface clients expect. Adapter lets classes work together that couldn't otherwise because of incompatible interfaces."*

When you integrate with Razorpay, Stripe, or Paytm:
* You are **not** choosing an internal calculation algorithm.
* You are communicating with an **external 3rd-party vendor API/SDK** whose proprietary methods (`RazorpayClient.createOrder()`, `Stripe.PaymentIntent.create()`) do not match your domain's `processPayment()` signature.
* Converting their proprietary SDK interface to your domain contract is the textbook definition of the **Adapter Pattern**.

#### GoF Definition of Simple Factory / Factory Method:
> *"Define an interface or helper for creating an object, but let subclasses or runtime parameters decide which class to instantiate."*

Selecting which concrete payment adapter to instantiate based on user input (`PaymentMode.RAZORPAY_UPI`, `PaymentMode.CASH`) is the textbook definition of a **Factory**.

### 3.3 The Litmus Test: Comparing Strategy vs. 3rd-Party Payment Integration

| Architectural Dimension | Strategy Pattern | 3rd-Party Payment Gateway Integration |
| :--- | :--- | :--- |
| **Primary Intent** | Swap an internal **behavioral algorithm** (sorting, mathematical formula, routing). | Bridge an incompatible **external vendor API/SDK** into your domain model. |
| **External Dependency Conversion?** | **None.** Pure internal logic operating on in-memory domain data. | **Yes.** Wraps proprietary vendor libraries (`RazorpayClient`, `StripeSDK`). |
| **GoF Classification** | **Behavioral Pattern**. | **Structural Pattern** (Adapter) + **Creational Pattern** (Factory). |
| **Failure Semantics** | Fails due to algorithmic conditions or invalid data. | Fails due to network I/O, network timeouts, API auth failures, vendor webhooks. |

### 3.4 The Architectural Architecture:
```
[ExitGate / Billing] ---> (PaymentProcessor Interface)
                                    ▲
                                    │ implements
                         [RazorpayPaymentAdapter]
                                    │ wraps
                         [External RazorpayClient SDK]
```
If tomorrow the business replaces Razorpay with Stripe or Cashfree, our billing engine never changes — we merely write `StripePaymentAdapter` and add it to `PaymentProcessorFactory`.

---

## 4. Deep Dive: The GoF State Pattern & Dynamic Gate Lane Reversal

### 4.1 The Problem It Solves: `if/else` Flag Spaghetti
Without the State Pattern, entities with operational modes (like toll gates, ATMs, or vending machines) end up with massive, nested conditional checks across every single method:
```java
// ❌ Anti-Pattern: Primitive Flag Pollution
public Ticket processEntry(Vehicle v) {
    if (this.mode == GateMode.MAINTENANCE) {
        throw new GateInMaintenanceException();
    } else if (this.mode == GateMode.EXIT_ONLY) {
        throw new InvalidOperationException("Gate is in exit-only mode!");
    }
    // actual entry logic...
}
```
This violates:
1. **Single Responsibility Principle**: The `Gate` class becomes bloated with validation logic for all operational states.
2. **Open/Closed Principle**: Adding a new mode (e.g. `VIP_EXCLUSIVE_MODE`) requires editing every single method in `Gate`.

### 4.2 The Solution: State Pattern on Gate
```java
public interface GateModeState {
    Ticket processEntry(Gate gate, Vehicle vehicle);
    Bill processExit(Gate gate, Ticket ticket);
    void enterMaintenance(Gate gate);
    void resumeNormalOperation(Gate gate);
}

public class EntryModeState implements GateModeState {
    public Ticket processEntry(Gate gate, Vehicle vehicle) {
        // execute entry flow, lower barrier, issue ticket
    }
    public Bill processExit(Gate gate, Ticket ticket) {
        throw new IllegalStateException("Cannot process exit at an Entry Gate!");
    }
    // ...
}
```
### 4.3 Real-World Application: 5:00 PM Rush-Hour Lane Reversal
In major airports, tech parks, and toll plazas:
* **Morning (8:00 AM - 11:00 AM)**: Gates 1, 2, 3, and 4 are configured as **Entry Lanes**. Only Gate 5 is an Exit Lane.
* **Evening (5:00 PM - 8:00 PM)**: Gate 2 and 3 dynamically flip their state to **Exit Lanes** to drain the parking lot quickly.
* With the State pattern, this transition happens cleanly in-memory:
  `gate2.setModeState(new ExitModeState());`
  Zero server downtime, zero code rewrites.

---

## 5. Advanced Gate Architecture: Role Interfaces & Command Pattern

### 5.1 Role-Based Gate Interfaces (Interface Segregation Principle)
To support dedicated entry gates, dedicated exit gates, and bidirectional gates cleanly without throwing runtime exceptions:
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
    protected Barrier barrier;
}

public class EntryGate extends Gate implements EntryCapable { ... }
public class ExitGate extends Gate implements ExitCapable { ... }
public class BidirectionalGate extends Gate implements EntryCapable, ExitCapable { ... }
```

### 5.2 Multi-Invoker Decoupling (Command Pattern)
In an enterprise smart parking facility, a ticket is not solely issued by a human guard pressing a button:
* **Invokers**:
  1. `GuardConsoleTerminal` (Manual UI touch screen)
  2. `AnprCameraScanner` (Automatic Number Plate Recognition camera)
  3. `RfidFastagReader` (Toll transponder reader on the ceiling)
  4. `MagneticGroundLoop` (Inductive metal sensor in the asphalt)
* **Command Encapsulation**:
  `IssueTicketCommand(EntryCapable gate, Vehicle vehicle)` encapsulates the action so any hardware sensor or user interface can execute entry without coupling to internal lot orchestration.
* **Hardware Decoupling**:
  `Barrier` actuation is encapsulated in `OpenBarrierCommand` and `CloseBarrierCommand`. If the physical stepper motor or relay protocol changes, domain gate logic remains 100% untouched.

---

## 6. Deep Dive: Strategy + Decorator Billing Pipeline

### 6.1 The Problem: Combining Dynamic Slabs with Optional Add-Ons
A customer's final invoice consists of two distinct components:
1. **The Base Parking Fee**: Calculated from stay duration (e.g. 5.5 hours $\to$ 6 billable hours), vehicle type, and progressive hourly rate slabs.
2. **Optional Value-Added Services**: Extra amenities chosen during the stay (e.g. EV Charging, Car Wash, Valet Service, Detailing).

### 6.2 The Architectural Collaboration:

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

### 6.3 Code Architecture:
```java
public interface BillableItem {
    double getCost();
    String getDescription();
}

public class BaseParkingCharge implements BillableItem {
    private final double amount;
    public BaseParkingCharge(double amount) { this.amount = amount; }
    public double getCost() { return amount; }
    public String getDescription() { return "Parking Fee: ₹" + amount; }
}

public abstract class BillableItemDecorator implements BillableItem {
    protected final BillableItem wrapped;
    public BillableItemDecorator(BillableItem wrapped) { this.wrapped = wrapped; }
}

public class EvChargingDecorator extends BillableItemDecorator {
    public EvChargingDecorator(BillableItem wrapped) { super(wrapped); }
    public double getCost() { return wrapped.getCost() + 150.0; }
    public String getDescription() { return wrapped.getDescription() + " + EV Charging (₹150.0)"; }
}

public class CarWashDecorator extends BillableItemDecorator {
    public CarWashDecorator(BillableItem wrapped) { super(wrapped); }
    public double getCost() { return wrapped.getCost() + 250.0; }
    public String getDescription() { return wrapped.getDescription() + " + Car Wash (₹250.0)"; }
}
```

---

## 7. Progressive Tiered Hourly Pricing Model

### 7.1 Rate Matrix
Partial hours are rounded up using ceiling math: $\text{billableHours} = \lceil \text{durationInHours} \rceil$.

| Vehicle Type | 0 - 2 Hours | 2 - 4 Hours | 4 - 6 Hours | 6+ Hours |
| :--- | :--- | :--- | :--- | :--- |
| **BIKE** | ₹20 / hr | ₹25 / hr | ₹30 / hr | ₹40 / hr |
| **CAR** | ₹25 / hr | ₹30 / hr | ₹35 / hr | ₹45 / hr |
| **TRUCK** | ₹50 / hr | ₹60 / hr | ₹65 / hr | ₹80 / hr |

### 7.2 Progressive Slab Calculation Walkthrough (The ₹180 Car Benchmark)
**Example**: A `CAR` enters at `10:00 AM` and exits at `03:30 PM`.
1. **Raw Duration**: 5 hours, 30 minutes = 5.5 hours.
2. **Ceiling Rounding**: $\lceil 5.5 \rceil = 6 \text{ billable hours}$.
3. **Progressive Slab Breakdown**:
   * **Slab 1 (Hours 0 to 2 = 2 hrs)**: $2 \times ₹25 = \mathbf{₹50}$
   * **Slab 2 (Hours 2 to 4 = 2 hrs)**: $2 \times ₹30 = \mathbf{₹60}$
   * **Slab 3 (Hours 4 to 6 = 2 hrs)**: $2 \times ₹35 = \mathbf{₹70}$
   * **Slab 4 (Hours 6+ = 0 hrs)**: $0 \times ₹45 = \mathbf{₹0}$
4. **Total Base Fee**: $₹50 + ₹60 + ₹70 = \mathbf{₹180.00}$.

---

## 8. Factory Pattern Analysis: When is it Useful vs. Over-Engineered?

### 8.1 When Factory is an Anti-Pattern (Pointless Boilerplate):
If an entity like `ParkingSpot` or `Vehicle` does not have polymorphic subtypes, creating a factory that merely executes:
```java
// ❌ Pointless Pass-Through Boilerplate
public static ParkingSpot createSpot(...) {
    return new ParkingSpot(...);
}
```
...adds zero architectural value, inflates cognitive load, and violates the YAGNI principle.

### 8.2 When Factory IS Legitimate in this System:
1. **Parameterized `PaymentProcessorFactory`**:
   - Takes a `PaymentMode` enum (`RAZORPAY_UPI`, `RAZORPAY_CARD`, `CASH`) and instantiates the correct adapter implementing `PaymentProcessor`.
2. **`PricingSlabFactory`**:
   - Decouples default rate slab tables from strategy logic, providing canonical rate slabs for `BIKE`, `CAR`, and `TRUCK`.
