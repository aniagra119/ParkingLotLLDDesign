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
7. **Payment & Barrier Decoupling**: Pluggable payment processors (Razorpay adapter) and physical boom barrier actuator isolation.

---

## 2. Design Patterns Applied & Rationale

| Pattern | Category | Where Applied | Concrete Architectural Justification |
| :--- | :--- | :--- | :--- |
| **Facade** | Structural | `ParkingLot` class | Hides complex subsystem orchestration (`ParkingFloorManager`, `GateManager`, `BillingEngine`) from external clients, preventing Law of Demeter violations. |
| **Strategy (x2)** | Behavioral | `ParkingAssignmentStrategy`<br>`FeeCalculationStrategy` | **1.** Swaps spatial search heuristics (Nearest Available Spot vs Random) in $O(1)$.<br>**2.** Swaps mathematical fee algorithms (Progressive Tiered Slabs vs Flat Rate) using `SlabBasedFeeStrategy`. |
| **Adapter** | Structural | `RazorpayPaymentAdapter` | Converts proprietary third-party SDK calls (`ExternalRazorpayClient`) into our clean domain `PaymentProcessor` contract (Rupees $\to$ Paise). |
| **Factory** | Creational | `PaymentProcessorFactory` | Centralizes runtime instantiation of payment adapters based on user's chosen `PaymentMode` enum. |
| **Builder** | Creational | `ParkingLotBuilder` | Constructs complex multi-floor, multi-gate parking lot hierarchies cleanly without telescoping constructors. |

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
* `ParkingAssignmentStrategy` (Nearest Spot vs Distributed vs Random) $\to$ True **Strategy** (spatial graph search algorithms).
* `FeeCalculationStrategy` (Tiered Slabs vs Flat Rate vs Surge) $\to$ True **Strategy** (mathematical fee computation algorithms).

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
