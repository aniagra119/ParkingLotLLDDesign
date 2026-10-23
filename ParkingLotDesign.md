# Comprehensive Architectural Design & Decision Log
## Multi-Floor Parking Lot Low-Level Design (LLD)

---

## 1. System Requirements & Domain Scope

### 1.1 Problem Statement
Design a clean, extensible, multi-floor parking lot system capable of managing high-throughput entry and exit flows, dynamically assigning parking spots based on vehicle dimensions, maintaining transactional bill settlement, calculating tiered hourly fees, supporting dynamic value-added amenities (EV charging, car wash), and enabling seamless third-party payment integration (e.g. Razorpay).

### 1.2 Core Capabilities
1. **Multi-Floor Capacity**: Multi-level floors, each containing designated spot types (`BIKE`, `CAR`, `TRUCK`).
2. **Strict 1:1 Spot Compatibility**: A car spot cannot accommodate bikes; a truck spot cannot accommodate cars. Each vehicle maps strictly to its dedicated spot type.
3. **Automated Ticket Issuance**: Generates an immutable entry record (`Ticket`) containing entry timestamp, assigned spot ID, floor number, vehicle plate, and ticket number.
4. **Spatial Spot Allocation (Strategy Pattern)**: Nearest available spot across floors (calculated using floor elevation delta first, then 2D Manhattan coordinates) in $O(1)$ lookup time relative to the entry gate.
5. **Tiered Hourly Fee Math (Template Strategy)**: Progressive fee calculation with hourly ceiling rounding ($\lceil \text{hours} \rceil$).
6. **Dynamic Amenity Billing (Polymorphic Services)**: Replaced naive Decorator pattern with an extensible `AmenityService` hierarchy (EV kWh-based, Wash package-based) directly aggregated in `Bill` for clean database serialization and itemized receipts.
7. **Payment Gateway Integration**: Pluggable payment processors (Razorpay adapter) decoupled from core parking session orchestration.

---

## 2. Design Patterns Applied & Rationale

| Pattern | Category | Where Applied | Concrete Architectural Justification |
| :--- | :--- | :--- | :--- |
| **Facade** | Structural | `ParkingLot` class | Hides complex subsystem orchestration (`ParkingFloorManager`, `GateManager`, `BillingEngine`) from external clients, preventing Law of Demeter violations. |
| **Strategy (x2)** | Behavioral | `ParkingAssignmentStrategy`<br>`FeeCalculationStrategy` | **1.** Swaps spatial search heuristics (Nearest Available Spot vs Random) in $O(1)$.<br>**2.** Swaps mathematical fee algorithms (Progressive Tiered Slabs vs Flat Rate) using `SlabBasedFeeStrategy`. |
| **Adapter** | Structural | `RazorpayPaymentAdapter` | Converts proprietary third-party SDK calls (`ExternalRazorpayClient`) into our clean domain `PaymentProcessor` contract (Rupees $\to$ Paise). |
| **Factory** | Creational | `PaymentProcessorFactory` | Centralizes runtime instantiation of payment adapters based on user's chosen `PaymentMode` enum. |
| **Builder** | Creational | `ParkingLotBuilder` | Constructs complex multi-floor, multi-gate parking lot hierarchies cleanly without telescoping constructors. |
| **Observer** | Behavioral | `ParkingEventPublisher`<br>`ParkingEventListener` | Decouples core parking lot workflows from cross-cutting concerns (LED displays, SMS receipts, mobile app notifications, and metrics). |

---

### 2.1 Package Architecture & Bounded Contexts
To adhere to the **Common Closure Principle (CCP)** and prevent the generic `model` "junk drawer" anti-pattern, components are segmented into distinct bounded contexts:

* **`com.parkinglot.model`**: Physical inventory, spatial layout, and entry/exit checkpoints (`Vehicle`, `ParkingSpot`, `ParkingFloor`, `Coordinates`, `Ticket`, `Gate`).
* **`com.parkinglot.strategy.assignment`**: Spatial spot allocation algorithms (`ParkingAssignmentStrategy`, `NearestAvailableSpotStrategy`). Separated by strategy type because spatial routing heuristics evolve independently from financial formulas.
* **`com.parkinglot.strategy.fee`**: Mathematical pricing and duration billing algorithms (`FeeCalculationStrategy`, `PricingSlab`, `SlabBasedFeeStrategy`, `TieredHourlyFeeStrategy`, `FlatRateFeeStrategy`).
* **`com.parkinglot.service`**: Specialized subsystem controllers enforcing Single Responsibility Principle (SRP):
  * `TicketManager`: In-memory active session ticket management, registration, and lifecycle closing.
  * `GateController`: Physical gate access validation and boom passage control.
* **`com.parkinglot.billing`**: Financial settlement domain (`Bill`, `BillingService`, `AmenityService`, `CarWashService`, `EvChargingService`).
* **`com.parkinglot.payment`**: Payment domain contracts, orchestration (`PaymentService`), transaction entities (`PaymentTransaction`), and factory-selected adapters.
  * **`com.parkinglot.payment.processor`**: Native first-party domain processors (`CashPaymentProcessor`).
  * **`com.parkinglot.payment.adapter`**: Secondary Driven Adapters converting third-party APIs into domain contracts (`RazorpayPaymentAdapter`).
  * **`com.parkinglot.payment.external`**: External third-party vendor SDKs and network clients (`ExternalRazorpayClient`).
* **`com.parkinglot.builder`**: Fluent construction hierarchy (`ParkingLotBuilder`).
* **`com.parkinglot.event`**: Event-driven domain messaging and Observer pattern infrastructure (`ParkingEvent`, `VehicleParkedEvent`, `VehicleExitedEvent`, `ParkingEventListener`, `ParkingEventPublisher`).
* **`com.parkinglot`**: Facade entry point coordinating subsystems (`ParkingLot`).

---

## 3. Deep Dive: Strategy vs. Adapter + Factory (The Widespread Interview Misconception)

### 3.1 The Mislabeling
In 90% of online tutorials and mock LLD interviews, payment gateway integration is mislabeled as the **"Strategy Pattern"**:
```java
// ❌ What tutorials mislabel as Strategy:
interface PaymentStrategy { void pay(double amount); }
class UpiStrategy implements PaymentStrategy { ... }
```
**Why do people do this?**  
Because syntactically it matches Strategy: an interface with multiple polymorphic classes injected into a context.

### 3.2 The GoF Reality: What is Strategy vs. Adapter?

#### GoF Definition of Strategy:
> *"Define a family of **algorithms**, encapsulate each one, and make them interchangeable. Strategy lets the **algorithm** vary independently from clients that use it."*

The keyword is **algorithm** — internal computation, sorting, searching, mathematical calculation, or spatial routing:
* `com.parkinglot.strategy.assignment.ParkingAssignmentStrategy` (Nearest Spot vs Distributed vs Random) $\to$ True **Strategy** (spatial graph search algorithms).
* `com.parkinglot.strategy.fee.FeeCalculationStrategy` (Tiered Slabs vs Flat Rate vs Surge) $\to$ True **Strategy** (mathematical fee computation algorithms).

#### GoF Definition of Adapter:
> *"Convert the interface of a class into another interface clients expect. Adapter lets classes work together that couldn't otherwise because of incompatible interfaces."*

When you integrate with Razorpay, Stripe, or Paytm:
* You are **not** choosing an internal calculation algorithm.
* You are communicating with an **external 3rd-party vendor API/SDK** whose proprietary methods (`RazorpayClient.captureTransaction(orderRef, paise, "INR")`) do not match your domain's `processPayment(billId, amountInRupees)` signature.
* Converting their proprietary SDK interface to your domain contract is the textbook definition of the **Adapter Pattern**.

#### GoF Definition of Simple Factory:
Selecting which concrete payment adapter to instantiate based on user input (`PaymentMode.RAZORPAY_UPI`, `PaymentMode.CASH`) is the textbook definition of a **Factory**.

---

## 4. The Architectural Law of the Decorator Pattern: Why Invoices Reject Decorators

### 4.1 The Golden Rule: Method Execution Alteration vs. Data Aggregation
1. **USE the Decorator Pattern for**: **In-memory, behavioral method execution alteration**
   - Intercepting, wrapping, and augmenting live method calls (`read()`, `send()`, `draw()`, `execute()`).
   - *Legitimate Examples*:
     - **Java I/O Streams**: `GzipInputStream(CipherInputStream(BufferedInputStream(FileInputStream)))` modifying `read()`.
     - **Network Middleware**: Retries, JWT auth injection, rate-limiting on `HttpClient.send()`.
     - **AutoCAD / UI Toolkits**: `ResizableShapeDecorator(TooltipDecorator(Circle))` intercepting `draw()` and click handlers.
2. **AVOID the Decorator Pattern for**: **Data entities that interact with Databases, Message Queues, or REST APIs**
   - Invoices, Shopping Carts, Menus, Orders, Receipts.

### 4.2 Why Decorator on an Invoice is an Anti-Pattern:
- **Immutability & Item Removal Trap**: If a customer cancels a Car Wash 5 minutes after parking, removing an inner decorator from `WashDecorator(EvDecorator(BaseCharge))` is structurally impossible in Java without deconstructing the entire object tree. In a `List<AmenityService>`, it is a simple `list.remove(service)`.
- **Serialization & DB Disaster**: Calling `jsonMapper.writeValueAsString()` on nested decorator instances creates recursive, nested JSON objects that cannot be queried or stored in standard relational SQL tables. A flat `List<AmenityService>` maps directly to database rows and clean JSON arrays.
- **Dangerous Order-of-Execution Bugs**: Chaining `TaxDecorator` and `DiscountDecorator` depends on whatever order the developer nested the wrappers. Reversing the nesting causes taxes to be calculated before discounts, violating legal accounting standards.

---

## 5. Child-Parent Relationships & Inverted Indexing

### 5.1 Why `ParkingSpot` stores `floorNumber` instead of full `ParkingFloor`
1. **Prevents Circular Dependencies**: `Floor` holds `Map<String, ParkingSpot>`. If `Spot` also held `ParkingFloor`, circular references complicate lifecycle, serialization, and test isolation.
2. **Normalized Foreign Key**: A spot is physically painted on a specific floor. It cannot physically exist without that floor. Therefore, `floorNumber` is an intrinsic attribute of the spot.
3. **Mismatched Input Prevention**: When generating a `Ticket`, passing `spot` allows `Ticket` to safely extract `spotId` and `floorNumber` directly from `spot`, eliminating caller data corruption.

### 5.2 Clean Floor Delegation (No Redundant Spot Maps)
`ParkingFloor` owns its spots in a single `Map<String, ParkingSpot> spots`:
- $O(1)$ duplicate spot ID detection via `putIfAbsent()`.
- $O(1)$ unparking lookup by ID.
- `ParkingLot` delegates directly to `floor.getSpot(spotId)` rather than duplicating a redundant global `spotIndex`.

---

## 6. Dynamic Amenity Billing: Polymorphic `AmenityService`

Instead of hardcoded values, services implement `AmenityService` and encapsulate their own dynamic mathematical pricing:
- `EvChargingService`: Energy formula ($\text{kWh Consumed} \times \text{Rate per kWh}$).
- `CarWashService`: Package tier + Vehicle size surcharge formula.

```
[Strategy: Base Parking Fee = ₹180.00]
                   │
                   ▼
[Ticket: Consumed Services = { EV (18.5 kWh @ ₹12 = ₹222), Wash (Foam = ₹300) }]
                   │
                   ▼
[Bill: Direct Aggregation Total = ₹702.00 with itemized line items]
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

### 7.2 The ₹180.00 Car Benchmark Walkthrough
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

## 8. Architectural Justification: Elimination of `Barrier` and Inclusion of `Gate` in Model

### 8.1 The Pass-Through Anti-Pattern
In tutorials or over-engineered LLD attempts, developers often create a separate `Barrier` class with simple `open()` and `close()` methods and a dedicated `gate` package.
In practice:
1. **Pass-Through Overhead Without Value**: Without hardware message queues, asynchronous IoT drivers, or the **Command Pattern** (e.g. queuing or undoing barrier movements), `Barrier` acts as a useless 1-line wrapper.
2. **Law of Demeter Violation**: Invocations like `gate.getBarrier().open()` force external callers to traverse through `Gate` to reach an internal component.
3. **Domain Reality**: `Gate` is a physical spatial model entity (residing on a floor, with 2D/3D coordinates and a gate type) that directly controls entry/exit access. Moving `Gate` into `com.parkinglot.model` and exposing `gate.open()` / `gate.close()` directly eliminates redundant packaging and ceremonial abstractions while maintaining clean domain cohesion.

---

## 9. Architectural Justification: Hexagonal Organization of the Payment Bounded Context

### 9.1 Why Processors, Adapters, and External APIs Are Fundamentally Different
In naive OOP designs, everything relating to payments is dumped into a single `payment` package. This conflates three fundamentally different architectural layers:

1. **The Domain Port (`com.parkinglot.payment.PaymentProcessor`)**:
   - Represents the core domain interface for financial settlement.
   - Clean signature (`processPayment(billId, amountInRupees)`) adhering to domain types.
   - Orchestrated by `PaymentProcessorFactory`.

2. **First-Party Domain Processors (`com.parkinglot.payment.processor.CashPaymentProcessor`)**:
   - Direct implementations of the port for offline/internal domain workflows (e.g. cash booth settlement).
   - Require no foreign protocol translation.

3. **Structural Driven Adapters (`com.parkinglot.payment.adapter.RazorpayPaymentAdapter`)**:
   - Implements the domain `PaymentProcessor` port while encapsulating a reference to an external vendor client.
   - Performs currency conversion (Rupees $\to$ Paise), payload mapping, error-code translation, and network response normalization.

4. **External Third-Party SDKs / Clients (`com.parkinglot.payment.external.ExternalRazorpayClient`)**:
   - Simulates or interfaces with vendor proprietary libraries (e.g. `com.razorpay.RazorpayClient`, Stripe SDK).
   - Uses vendor-specific data structures, currency denominations, and authentication headers.
   - Contains zero knowledge of our parking lot domain, `Bill`, or `Ticket`.

This Ports & Adapters (Hexagonal) separation guarantees that introducing Stripe, PayPal, or mock testing environments requires writing an adapter in `payment.adapter` without touching core domain rules.

### 9.2 Transaction Entity & Payment Audit Trails
A common flaw in academic LLD designs is simply calling `bill.markPaid()` with a single boolean flag, destroying all payment history. In real-world financial architectures:
* Every payment attempt produces a `PaymentTransaction` entity recording:
  - `paymentId` (Unique ID)
  - `billId`
  - `amount`
  - `paymentMode`
  - `attemptedAt` timestamp
  - `status` (`PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`)
  - `transactionReference` (external gateway reference or cash receipt ID)
  - `failureReason` (for failed attempts)
* `Bill` maintains a thread-safe `List<PaymentTransaction> paymentHistory`, enabling retry attempts, accounting reconciliation against external vendor ledgers, and complete auditability without coupling to gateway mechanics.

---

## 10. Architectural Justification: Decomposing the ParkingLot Facade (Single Responsibility Principle)

### 10.1 Avoiding the God Object Anti-Pattern
In naive LLD designs, `ParkingLot` becomes a bloated "God Object" that mixes spot assignment, active ticket hashmap mutations, gate access validation, fee calculation math, payment gateway calls, and boom passage controls into single multi-responsibility methods.

### 10.2 The Decomposed Delegate Architecture
We decomposed `ParkingLot` into a pure high-level **Facade** orchestrating four specialized subsystem delegates:
1. **`TicketManager`**: Owns active ticket storage, retrieval, and closing. Only changes if ticket storage strategy changes (e.g. moving to Redis/PostgreSQL).
2. **`GateController`**: Owns gate lookup, entry/exit type validation, and passage cycles. Only changes if physical gate hardware protocols change.
3. **`BillingService`**: Owns invoice calculation and tariff strategy evaluation. Only changes if billing policies change.
4. **`PaymentService`**: Owns payment adapter execution and transaction audit logging. Only changes if payment orchestration or retry policies change.

`ParkingLot` now retains a single responsibility: **high-level subsystem workflow orchestration**.

---

## 11. Architectural Justification: Event-Driven Domain Architecture (Observer Pattern)

### 11.1 Cross-Cutting Decoupling
Real-world enterprise parking lots need to trigger external systems upon entry and exit:
- Updating electronic LED vacancy boards at the entrance.
- Dispatching SMS/WhatsApp parking slips to drivers.
- Sending telemetry and occupancy metrics to municipal traffic systems.
- Invoking reservation or billing ledgers.

Coupling these secondary concerns into the synchronous core parking algorithm violates the **Open/Closed Principle (OCP)** and **Single Responsibility Principle (SRP)**.

### 11.2 Implementation Details
* **Domain Events**: Clean, immutable records (`VehicleParkedEvent`, `VehicleExitedEvent`) implementing the `ParkingEvent` interface.
* **Thread-Safe Event Bus**: `ParkingEventPublisher` maintains a thread-safe subscriber list (`CopyOnWriteArrayList<ParkingEventListener>`) and safely isolates core operations from subscriber runtime exceptions.
* **Zero Overhead**: Listeners can be attached at startup via `ParkingLotBuilder.withEventListener(...)` or dynamically subscribed at runtime via `parkingLot.subscribeToEvents(...)`.
