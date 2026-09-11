# Comprehensive Architectural Design & Decision Log
## Multi-Floor Parking Lot Low-Level Design (LLD)

---

## 1. System Requirements & Domain Scope

### 1.1 Problem Statement
Design a clean, extensible, multi-floor parking lot system capable of managing high-throughput entry and exit flows, dynamically assigning parking spots based on vehicle dimensions, maintaining transactional bill settlement, calculating tiered hourly fees, supporting dynamic value-added amenities (EV charging, car wash), and enabling seamless third-party payment integration (e.g. Razorpay).

### 1.2 Core Capabilities
1. **Multi-Floor Capacity**: Multi-level floors, each containing designated spot types (`BIKE`, `CAR`, `TRUCK`).
2. **Strict 1:1 Spot Compatibility**: A car spot cannot accommodate bikes; a truck spot cannot accommodate cars. Each vehicle maps strictly to its dedicated spot type.
3. **Automated Ticket Issuance**: Generates an immutable entry record (`Ticket`) containing entry timestamp, assigned spot ID, vehicle plate, and ticket number.
4. **Spatial Spot Allocation (Strategy Pattern)**: Nearest available spot across floors (calculated using floor levels and 2D Manhattan coordinates) in $O(1)$ lookup time relative to the entry gate.
5. **Tiered Hourly Fee Math (Template Strategy)**: Progressive fee calculation with hourly ceiling rounding ($\lceil \text{hours} \rceil$).
6. **Dynamic Amenity Billing (Decorator Pattern)**: Universal decorator pattern wrapping polymorphic `AmenityService` implementations (EV kWh-based, Wash package-based) without hardcoding values.
7. **Payment & Barrier Decoupling**: Pluggable payment processors (Razorpay adapter) and physical boom barrier actuator isolation.

---

## 2. Design Patterns Applied & Rationale

| Pattern | Category | Where Applied | Concrete Architectural Justification |
| :--- | :--- | :--- | :--- |
| **Facade** | Structural | `ParkingLot` class | Hides complex subsystem orchestration (`FloorManager`, `GateManager`, `Billing`) from external clients, preventing Law of Demeter violations. |
| **Strategy (x2)** | Behavioral | `ParkingAssignmentStrategy`<br>`FeeCalculationStrategy` | **1.** Swaps spatial search heuristics (Nearest Available Spot vs Random) in $O(1)$.<br>**2.** Swaps mathematical fee algorithms (Progressive Tiered Slabs vs Flat Rate) using `SlabBasedFeeStrategy`. |
| **Decorator** | Structural | `AmenityServiceDecorator` on `BillableItem` | Dynamically wraps parking fees with optional polymorphic customer amenities (`EvChargingService`, `CarWashService`) avoiding an exponential $2^N$ subclass explosion. |
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

## 4. Deep Dive: Child-Parent Relationships & Inverted Indexing

### 4.1 Why `ParkingSpot` stores `floorNumber` instead of full `ParkingFloor`
1. **Prevents Circular Dependencies**: `Floor` holds `Map<String, ParkingSpot>`. If `Spot` also held `ParkingFloor`, circular references complicate lifecycle, serialization, and test isolation.
2. **Normalized Foreign Key**: A spot is physically painted on a specific floor. It cannot physically exist without that floor. Therefore, `floorNumber` is an intrinsic attribute of the spot.
3. **Mismatched Input Prevention**: When generating a `Ticket`, passing `spot` allows `Ticket` to safely extract `spotId` and `floorNumber` directly from `spot`, eliminating caller data corruption.

### 4.2 Inverted Index vs. Linear Spot Scanning
Instead of scanning every spot linearly across floors ($O(N)$), `ParkingFloor` indexes spots in `Map<String, ParkingSpot> spots`:
- $O(1)$ duplicate spot ID detection via `putIfAbsent()`.
- $O(1)$ unparking lookup by ID.
- Clean stream filter for available spots matching vehicle type.

---

## 5. Deep Dive: Dynamic Amenity Billing & Universal Decorator Pattern

### 5.1 The Anti-Pattern: Hardcoded Decorators
Hardcoding flat values (`₹150` for EV, `₹250` for Car Wash) into decorators or enums violates reality:
- **EV Charging** depends on electricity consumed: $\text{kWh Consumed} \times \text{Rate per kWh}$.
- **Car Wash** depends on vehicle chassis size and package tier (Basic vs Premium Foam).

### 5.2 The Solution: Universal `AmenityServiceDecorator` + Polymorphic `AmenityService`
```
[BaseParkingCharge (Strategy Output: ₹180.00)]
                    │
                    ▼
[AmenityServiceDecorator(EvChargingService: 18.5 kWh @ ₹12 = ₹222.00)]
                    │
                    ▼
[AmenityServiceDecorator(CarWashService: CAR - PREMIUM_FOAM = ₹300.00)]
                    │
                    ▼
[Final Bill: ₹702.00 with full itemized breakdown]
```

#### Code Architecture:
```java
public interface AmenityService {
    double calculateCost();
    String getItemizedDescription();
}

public class AmenityServiceDecorator extends BillableItemDecorator {
    private final AmenityService amenityService;

    public AmenityServiceDecorator(BillableItem wrapped, AmenityService amenityService) {
        super(wrapped);
        this.amenityService = Objects.requireNonNull(amenityService);
    }

    @Override
    public double getCost() {
        return Math.round((wrappedItem.getCost() + amenityService.calculateCost()) * 100.0) / 100.0;
    }

    @Override
    protected String getServiceLine() {
        return amenityService.getItemizedDescription();
    }
}
```

---

## 6. Progressive Tiered Hourly Pricing Model

### 6.1 Rate Matrix
Partial hours are rounded up using ceiling math: $\text{billableHours} = \lceil \text{durationInHours} \rceil$.

| Vehicle Type | 0 - 2 Hours | 2 - 4 Hours | 4 - 6 Hours | 6+ Hours |
| :--- | :--- | :--- | :--- | :--- |
| **BIKE** | ₹20 / hr | ₹25 / hr | ₹30 / hr | ₹40 / hr |
| **CAR** | ₹25 / hr | ₹30 / hr | ₹35 / hr | ₹45 / hr |
| **TRUCK** | ₹50 / hr | ₹60 / hr | ₹65 / hr | ₹80 / hr |

### 6.2 The ₹180.00 Car Benchmark Walkthrough
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

## 7. Advanced Architectural Extensions (For Discussion & Reference)

### 7.1 State Pattern for Dynamic Gate Lane Reversal
In peak rush-hour traffic (5:00 PM), lanes flip roles:
- Entry gates dynamically transition to Exit gates (`EntryModeState` $\to$ `ExitModeState`).
- Avoids `if/else` flag spaghetti across gate methods without restarting servers.

### 7.2 Command Pattern for Gate Hardware Decoupling
- Hardware sensors (ANPR camera, RFID FASTag reader, inductive ground loop, guard UI) invoke `IssueTicketCommand` uniformly.
- Physical barrier boom actuators are decoupled from domain gate logic via `OpenBarrierCommand` and `CloseBarrierCommand`.

### 7.3 Observer Pattern for Real-Time Display Boards
- Real-time digital LED signage and operations monitors subscribe as `ParkingLotObserver`s.
- Pushes `SpotOccupancyEvent` on vehicle park/vacate without wasteful polling.
