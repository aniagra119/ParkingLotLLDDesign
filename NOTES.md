# Automated Smart Parking Lot System — Master Architecture & Engineering Guide

A comprehensive, production-grade reference and deep-dive revision guide for designing, implementing, and scaling automated multi-floor smart parking lot systems.

---

## 1. System Domain Decomposition & Responsibility Boundaries

A robust Low-Level Design separates physical gate sensors, entry/exit workflows, spot allocation algorithms, tariff calculation engines, payment adapters, and real-time telemetry.

```
                      ┌─────────────────────────────────────────┐
                      │           ParkingLot (Facade)           │
                      └────────────────────┬────────────────────┘
                                           │
         ┌─────────────────────────────────┴─────────────────────────────────┐
         ▼                                                                   ▼
┌─────────────────────────────────┐                         ┌─────────────────────────────────┐
│     Ingress / Egress Gates      │                         │     Spatial Inventory Core      │
│  - EntranceGate (ANPR / Ticket) │                         │  - ParkingFloor (Spatial Unit)  │
│  - ExitGate (Settlement / Loop) │                         │  - ParkingSpot (Atomic Entity)  │
│  - GateHardwareAdapter (Ports)  │                         │  - Coordinates (Spatial Metric) │
│  - BoomBarrier (State Machine)  │                         │  - ParkingTicket (Value Audit)  │
└────────────────┬────────────────┘                         └────────────────┬────────────────┘
                 │ delegates allocation                                      │ queries / releases
                 ▼                                                           ▼
┌─────────────────────────────────┐                         ┌─────────────────────────────────┐
│    Spot Allocation Hierarchy    │                         │    Tariff & Billing Engines     │
│  - ParkingSpotManager (Abstract)│                         │  - FeeStrategy (Strategy Port)  │
│  - Vehicle-Specific Managers    │                         │  - SlabBasedFeeStrategy (Tmplt) │
│  - ParkingStrategy (Nearest/Fit)│                         │  - BillingDecorator Hierarchy   │
└────────────────┬────────────────┘                         └────────────────┬────────────────┘
                 │ notifies on status change                                 │ processes settlement
                 ▼                                                           ▼
┌─────────────────────────────────┐                         ┌─────────────────────────────────┐
│    Observer Telemetry Layer     │                         │   Hexagonal Payment Adapters    │
│  - ParkingObserver (Interface)  │                         │  - PaymentProcessor (SPI Port)  │
│  - DisplayPanel (LED Matrix)    │                         │  - PaymentProcessorFactory (Reg)│
│  - Remote BMS / IoT Broker      │                         │  - RazorpayPaymentAdapter       │
└─────────────────────────────────┘                         └─────────────────────────────────┘
```

### Component Responsibility Matrix

| Layer / Component | Type | Core Responsibility | Anti-Patterns to Avoid |
| :--- | :--- | :--- | :--- |
| **`ParkingLot`** | Facade / Singleton | High-level system lifecycle, floor registry, and global telemetry coordination. | **Never** calculate hourly fees or micromanage individual spot locking directly. |
| **`ParkingFloor`** | Spatial Aggregate | Owns a collection of spots for a physical floor. Tracks real-time occupancy counts. | **Never** issue tickets or invoke external payment gateways. |
| **`ParkingSpot`** | Domain Entity | Encapsulates single-bay state (`isOccupied`), spot type (`VehicleType`), spatial coordinates, and live vehicle reference. | **Never** search other floors to find an alternative spot. |
| **`EntranceGate`** | Workflow Controller | Reads vehicle license plate, orchestrates spot discovery via `ParkingSpotManager`, assigns spot, and prints `ParkingTicket`. | **Never** hardcode pricing rules or physical barrier GPIO pins. |
| **`ExitGate`** | Workflow Controller | Validates `ParkingTicket`, computes fee via `FeeStrategy` + Decorators, delegates payment, releases spot, and commands barrier actuation. | **Never** mutate spot allocation queues directly (delegate to floor/spot manager). |
| **`ParkingSpotManager`** | Domain Service | Manages discovery, assignment, and release of spots for a specific vehicle category. Uses pluggable `ParkingStrategy`. | **Never** create or settle financial payments. |
| **`FeeStrategy`** | Strategy (Open/Closed) | Computes base parking fees based on duration, vehicle class, and temporal slabs. | **Never** charge credit cards or interact with gate barriers. |
| **`PaymentProcessorFactory`**| Fowler Registry | Thread-safe registry mapping `PaymentMethod` to decoupled `PaymentProcessor` adapters. | **Never** instantiate vendor-specific SDK classes (e.g., `ExternalRazorpayClient`) directly. |
| **`GateHardwareAdapter`** | Hexagonal Port / SPI | Bridges domain barrier commands (`raiseBarrier()`, `lowerBarrier()`) to electrical microcontrollers/PLCs. | **Never** hold domain state or make billing decisions. |
| **`DisplayPanel`** | Observer | Displays available spot counts per floor and vehicle type in real time. | **Never** modify spot states or trigger gate opening. |

---

## 2. Hardware Transducers, Barrier Actuation & Anti-Tailgating Loops

In real-world parking automation, software must synchronize with physical magnetic induction loops, optical safety curtains, and automated boom barriers.

```
+---------------------------------------------------------------------------------------+
|                                PHYSICAL DRIVEWAY LAYOUT                               |
|                                                                                       |
|   Direction of Travel  ===> ===> ===> ===> ===> ===> ===> ===>                        |
|                                                                                       |
|      [ Loop A ]           [ ANPR Camera ]          [ Barrier ]          [ Loop B ]    |
|   (Vehicle Presence)      (License Plate)         (Boom Gate)         (Safety/Clear)  |
|          │                       │                     │                     │        |
+──────────┼───────────────────────┼─────────────────────┼─────────────────────┼────────+
           ▼                       ▼                     ▲                     ▼
     Ground Sensor           Optical OCR           Servo Actuator         Clearance Loop
     Detects Arrival         Reads Reg No.         Raises / Lowers        Detects Departure
```

### Barrier Hardware State Machine

```
   ┌───────────────┐
   │    LOWERED    │◄─────────────────────────────┐
   └───────┬───────┘                              │
           │ Payment Verified / Ticket Issued     │ Vehicle Cleared Loop B
           ▼                                      │ (Safety Timeout)
   ┌───────────────┐                              │
   │    RAISING    │                              │
   └───────┬───────┘                              │
           │ Limit Switch: Fully Up               │
           ▼                                      │
   ┌───────────────┐                        ┌─────┴─────────┐
   │    RAISED     │───────────────────────►│    LOWERING   │
   └───────────────┘  Vehicle Enters Loop B └───────────────┘
                      (Passage Detected)
```

### Series Safety Interlocks & Anti-Tailgating Rules

1. **Anti-Tailgating Hardware Interlock**:
   - Loop A detects Vehicle 1. Ticket issued $\to$ Barrier transitions to `RAISED`.
   - Vehicle 1 crosses over Loop B. As Vehicle 1 clears Loop B, an immediate `LOWERING` sequence triggers.
   - If Vehicle 2 attempts to tailgate without pulling a ticket, Loop B's optical beam or magnetic field triggers an emergency obstruction pause: the barrier immediately halts downward movement to prevent vehicle damage, sounds an audible intrusion klaxon, and dispatches a security operator alarm.
2. **Software-Hardware Decoupling via Hexagonal SPI**:
```java
public interface GateHardwareAdapter {
    void raiseBarrier();
    void lowerBarrier();
    boolean isVehiclePresent();      // Loop A contact
    boolean isPassageClear();        // Loop B contact
    boolean isObstructionDetected(); // Optical infrared light curtain
}
```

---

## 3. High-Throughput Concurrency & Atomic Spot Allocation

### Why Naive Linear Iteration Collapses Under Rush-Hour Load

In a multi-level structure with 5,000 spots across 10 floors and 6 concurrent entrance gates:
- A linear scan ((F 	imes S)$) iterates up to 5,000 elements per vehicle arrival.
- Under peak load (30 arrivals/second), 6 threads executing (N)$ linear scans cause massive CPU cache thrashing and memory bus contention.
- **Race Condition Vulnerability**: Gate 1 and Gate 2 simultaneously evaluate Spot `F2-S104` as unoccupied. Both gates issue tickets for the exact same spot, resulting in an in-facility gridlock and driver conflict.

### The In-Memory Solution: Partitioned Concurrent Collections & Atomic CAS

Instead of scanning all spots, partition spots by `VehicleType` into thread-safe priority structures:

```java
public class ConcurrentNearestSpotStrategy implements ParkingStrategy {
    // Floor-partitioned concurrent skip list sorted by distance to entrance
    private final ConcurrentMap<VehicleType, ConcurrentSkipListSet<ParkingSpot>> availableSpots = 
        new ConcurrentHashMap<>();

    @Override
    public Optional<ParkingSpot> findSpot(List<ParkingFloor> floors, Vehicle vehicle, Coordinates gateCoord) {
        ConcurrentSkipListSet<ParkingSpot> queue = availableSpots.get(vehicle.getType());
        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }

        // Lock-free poll with atomic compare-and-set verification
        for (ParkingSpot candidate : queue) {
            if (candidate.tryOccupy(vehicle)) { // Atomic CAS: false -> true
                queue.remove(candidate);
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
```

### Atomic Spot Reservation in `ParkingSpot`:
```java
public class ParkingSpot {
    private final AtomicBoolean occupied = new AtomicBoolean(false);
    private volatile Vehicle parkedVehicle;

    public boolean tryOccupy(Vehicle vehicle) {
        if (occupied.compareAndSet(false, true)) {
            this.parkedVehicle = vehicle;
            return true; // Won the race condition atomically
        }
        return false; // Lost race: another thread grabbed this spot
    }

    public void vacate() {
        this.parkedVehicle = null;
        this.occupied.set(false);
    }
}
```

### Multi-Node Distributed Architecture (Horizontal Scaling)

When the parking facility runs across multiple gateway servers behind a load balancer, JVM-level synchronization is insufficient.

```
                      ┌─────────────────────────────────────────┐
                      │          API Gateway / Ingress          │
                      └────────────┬───────────────┬────────────┘
                                   │               │
                    ┌──────────────┘               └──────────────┐
                    ▼                                             ▼
         ┌────────────────────┐                        ┌────────────────────┐
         │ Entrance Worker 01 │                        │ Entrance Worker 02 │
         └─────────┬──────────┘                        └──────────┬─────────┘
                   │                                              │
                   └──────────────────────┬───────────────────────┘
                                          │ Distributed Lock (Redlock)
                                          ▼
                               ┌─────────────────────┐
                               │   Redis Cluster     │
                               │   SET spot:{id} NX  │
                               └──────────┬──────────┘
                                          │ Fallback / Event Stream
                                          ▼
                               ┌─────────────────────┐
                               │   PostgreSQL DB     │
                               │  SELECT FOR UPDATE  │
                               └─────────────────────┘
```

1. **Redis Atomic Reservation**:
   ```redis
   -- Try reserving spot atomically for 60 seconds (reservation TTL)
   SET spot:reservation:F3-S12 "GATE_04" NX EX 60
   ```
2. **Database Optimistic Concurrency Control (OCC)**:
   ```sql
   UPDATE parking_spots 
   SET is_occupied = TRUE, version = version + 1 
   WHERE id = 'F3-S12' AND is_occupied = FALSE AND version = 4;
   ```
   If rows updated  0$, another gate acquired the spot; retry with the next closest spot.

---

## 4. Dynamic Tariff Engine & Mathematical Rate Modeling

### Template Method + Strategy Hierarchy

The billing framework combines the **Strategy Pattern** (allowing pluggable pricing models) with the **Template Method Pattern** (enforcing structural invariants like rounding, minimum charges, and grace periods).

```
                      ┌───────────────────────────────────────┐
                      │              FeeStrategy              │
                      │               «interface»             │
                      └───────────────────▲───────────────────┘
                                          │
                      ┌───────────────────┴───────────────────┐
                      │         SlabBasedFeeStrategy          │
                      │               «abstract»              │
                      │ + calculateFee() [final Template]     │
                      │ # computeSlabFee() [abstract Hook]    │
                      └───────────────────▲───────────────────┘
                                          │
                      ┌───────────────────┴───────────────────┐
                      │        TieredHourlyFeeStrategy        │
                      │ # computeSlabFee() [Implementation]   │
                      └───────────────────────────────────────┘
```

### Invariant Template Method Implementation:
```java
public abstract class SlabBasedFeeStrategy implements FeeStrategy {
    public static final Duration GRACE_PERIOD = Duration.ofMinutes(15);
    public static final double MINIMUM_BASE_FEE = 20.0;

    @Override
    public final double calculateFee(ParkingTicket ticket) {
        Objects.requireNonNull(ticket, "ticket cannot be null");
        Duration duration = Duration.between(ticket.getEntryTime(), ticket.getExitTime());
        
        // Invariant 1: Grace period (free exit if driver cannot find spot or leaves immediately)
        if (duration.compareTo(GRACE_PERIOD) <= 0) {
            return 0.0;
        }

        // Invariant 2: Ceil to full hours
        long billedHours = Math.max(1, (long) Math.ceil(duration.toMinutes() / 60.0));

        // Delegate to specialized slab calculation hook
        double computed = computeSlabFee(ticket.getVehicle().getType(), billedHours);

        // Invariant 3: Base floor fee
        return Math.max(computed, MINIMUM_BASE_FEE);
    }

    protected abstract double computeSlabFee(VehicleType type, long billedHours);
}
```

### Mathematical Slab Rate Formula

For non-overlapping slabs  = [0, H_1], S_2 = (H_1, H_2], \dots, S_k = (H_{k-1}, \infty)$ with rates , R_2, \dots, R_k$:

66636\text{TotalSlabFee} = \sum_{i=1}^k \max\Big(0, \, \min(\text{BilledHours}, H_i) - H_{i-1}\Big) \times R_i66636

### Decorator Pattern for Dynamic Add-on Services

```
                      ┌───────────────────────────────────────┐
                      │           BillingComponent            │
                      │               «interface»             │
                      │ + calculateTotal(): double            │
                      │ + getDetailedBreakdown(): List<Item>  │
                      └───────────────────▲───────────────────┘
                                          │
                  ┌───────────────────────┴───────────────────────┐
                  │                                               │
      ┌───────────────────────────┐                 ┌───────────────────────────┐
      │     BaseTicketBilling     │                 │   BillingServiceDecorator │
      │  (Wraps Base FeeStrategy) │                 │         «abstract»        │
      └───────────────────────────┘                 └─────────────▲─────────────┘
                                                                  │
                                      ┌───────────────────────────┴───────────────────────────┐
                                      │                                                       │
                          ┌───────────────────────────┐                           ┌───────────────────────────┐
                          │  ElectricityFeeDecorator  │                           │    ValetFeeDecorator      │
                          │   (EV Fast Charging Svc)  │                           │  (Premium Curbside Valet) │
                          └───────────────────────────┘                           └───────────────────────────┘
```

**Benefits**:
1. **Mathematical Auditability**: Generates itemized receipts displaying base parking tariff, EV kWh electricity costs, and valet surcharges separately.
2. **Zero Invariant Pollution**: Base ticket pricing logic does not need modification when adding new amenities (car wash, vacuuming, tire pressure check).

---

## 5. Payment Gateway Architecture: Hexagonal Adapters & The Registry Pattern

### The Problem with Tightly-Coupled Payment Gateways

Directly instantiating proprietary payment SDKs in business services leads to severe design flaws:
- Vendor SDKs have incompatible method signatures (`chargeCard(...)` vs `createOrder(...)`).
- Adding a new gateway (e.g., Stripe, PayPal, UPI) causes cascade modifications to factories, controllers, and tests, violating the **Open/Closed Principle**.

### Hexagonal Ports & Adapters Architecture

```
   ┌────────────────────────────────────────────────────────┐
   │                     DOMAIN CORE                        │
   │                                                        │
   │   [ExitGate] ──► (PaymentProcessor Interface - Port)   │
   └───────────────────────────────────▲────────────────────┘
                                       │ Implements
        ┌──────────────────────────────┴──────────────────────────────┐
        │                                                             │
┌───────┴─────────────────────────────┐               ┌───────────────┴─────────────────────┐
│       RazorpayPaymentAdapter        │               │         StripePaymentAdapter        │
│       (Hexagonal SPI Adapter)       │               │       (Hexagonal SPI Adapter)       │
└───────────────────┬─────────────────┘               └───────────────────┬─────────────────┘
                    │ Delegates                                           │ Delegates
                    ▼                                                     ▼
┌─────────────────────────────────────┐               ┌─────────────────────────────────────┐
│        ExternalRazorpayClient       │               │         ExternalStripeClient        │
│     (3rd-Party Vendor Proprietary)  │               │     (3rd-Party Vendor Proprietary)  │
└─────────────────────────────────────┘               └─────────────────────────────────────┘
```

### The Fowler Registry Pattern (`PaymentProcessorFactory`)

Decouples the factory from vendor-specific classes using dynamic registration:

```java
public final class PaymentProcessorFactory {
    private static final Map<PaymentMethod, PaymentProcessor> REGISTRY = new ConcurrentHashMap<>();

    // Prevent direct instantiation
    private PaymentProcessorFactory() {}

    public static void register(PaymentMethod method, PaymentProcessor processor) {
        Objects.requireNonNull(method, "method cannot be null");
        Objects.requireNonNull(processor, "processor cannot be null");
        REGISTRY.put(method, processor);
    }

    public static PaymentProcessor getProcessor(PaymentMethod method) {
        PaymentProcessor processor = REGISTRY.get(method);
        if (processor == null) {
            throw new IllegalArgumentException("No processor registered for payment method: " + method);
        }
        return processor;
    }
}
```

### Distributed Payment Idempotency & Webhook Reconciliation

To eliminate double-charging on network timeout:
1. **Deterministic Idempotency Key**:
   66636\text{IdempotencyKey} = \text{SHA256}(\text{ticketId} + \text{amount} + \text{paymentMethod})66636
2. **Two-Phase Settlement**:
   - Step 1: Submit authorization request with `Idempotency-Key` header.
   - Step 2: If network times out, the client safely retries using the identical key; the gateway recognizes the token and returns the cached transaction response without re-executing credit card debits.
   - Step 3: Webhook endpoint asynchronously confirms `PAYMENT_CAPTURED` event in the event of dropped HTTP responses at the physical gate terminal.

---

## 6. Real-Time Telemetry & Distributed Pub/Sub

In modern smart facilities, spot transitions must immediately broadcast to LED entrance displays, mobile navigation apps, and municipal traffic management networks.

```
ParkingSpot State Change (VACATE / OCCUPY)
                 │
                 ▼
         ParkingLot (Subject)
                 │
                 ▼
        notifyObservers(...)
                 │
    ┌────────────┼───────────────────────────┐
    ▼            ▼                           ▼
[DisplayPanel] [Cloud Event Producer]  [Building Management]
(LED Screen)   (Kafka / MQTT Broker)    (HVAC / Lighting CO2)
                         │
        ┌────────────────┴────────────────┐
        ▼                                 ▼
[Mobile Navigation App]        [City Highway Variable Signs]
("Level 2: 14 Spots Free")     ("Central Garage: 82% FULL")
```

### Event Payload Schema:
```json
{
  "eventId": "evt_9082340912",
  "eventType": "SPOT_OCCUPIED",
  "timestamp": "2026-10-23T20:15:00Z",
  "facilityId": "GARAGE_NYC_04",
  "floorNumber": 2,
  "spotId": "F2-CAR-042",
  "vehicleType": "CAR",
  "remainingFloorCapacity": 18,
  "totalFreeSpots": 214
}
```

---

## 7. Production LLD Interview Revision & Defense Guide

### Architecture Tradeoff Matrix

| Decision Axis | Alternative A | Alternative B | Recommended Approach & Engineering Rationale |
| :--- | :--- | :--- | :--- |
| **Spot Allocation Strategy** | **Random / First-Fit**: Grabs first open spot found in list. | **Coordinate Distance (Nearest-Fit)**: Uses Euclidean / Manhattan distance to entrance gate. | **Nearest-Fit**: Minimizes driver transit time, prevents parking garage ramp congestion, and reduces carbon monoxide / exhaust emissions. |
| **Pricing Calculation** | **Pure Hourly Multiplier**: $\text{Fee} = \text{Hours} \times \text{Rate}$. | **Tiered Slab Model**: Decreasing marginal cost per hour with grace periods. | **Tiered Slab Model**: Industry standard. Encourages short-term turnover while offering fair rates for long-stay commuters. |
| **Payment Gateway Integration** | **Direct Hardcoded SDK**: Concrete API calls in `ExitGate`. | **Hexagonal SPI Adapter + Fowler Registry**: Uniform interface wrapping external SDK clients. | **Hexagonal Registry**: Strict compliance with Open/Closed and Single Responsibility principles. Allows zero-downtime addition of new vendors. |
| **Hardware Coupling** | **Domain calls GPIO drivers directly**: Classes import serial port libraries. | **Hexagonal Hardware Adapter**: Domain fires commands into abstraction interfaces. | **Hexagonal Adapter**: Allows simulated testing without physical test hardware, preventing hardware vendor lock-in. |

### Top 5 Senior LLD Interview Trap Questions

#### 1. "How do you handle multi-storey EV spots where standard cars shouldn't park?"
* **Answer**: Maintain separate `ParkingSpotManager` instances partitioned by `VehicleType.EV`. When a non-EV car arrives, the manager will never poll or return spots reserved for electric vehicles. At the database level, spots maintain a `type` enum check constraint.

#### 2. "What happens if a driver loses their physical printed ticket?"
* **Answer**: The system uses the vehicle license plate as a fallback index. The `ExitGate` queries the persistence store for the active ticket linked to `licensePlateNumber`. If the plate cannot be identified (e.g., mud on plate), a specialized `LostTicketFeeStrategy` charges a fixed penalty fee and logs the incident for security audit.

#### 3. "How do you prevent a deadlock if 4 cars enter 4 gates when only 1 spot is left?"
* **Answer**: Use atomic reservations with atomic compare-and-set (`AtomicBoolean.compareAndSet(false, true)` in-memory or Redis `SETNX`). Only one gate wins the reservation. The other 3 gates immediately receive an empty reservation response, display a "GARAGE FULL" notice on their LED displays, and keep their entry boom barriers down.

#### 4. "How do you handle dynamic peak-hour surge pricing without breaking existing tickets?"
* **Answer**: When a ticket is issued at the entrance gate, it records a reference to the active `FeeStrategy` or the tariff configuration version valid at entry time. The pricing contract is frozen upon ticket generation, preventing unfair retroactive surcharges while vehicles are parked.

#### 5. "How would you design the system for 100 entrance gates in an Olympic Stadium?"
* **Answer**: 
  1. Decompose gate controllers into decentralized edge workers.
  2. Implement an in-memory Redis cluster partitioning spot pools by gate sector (e.g., Gate 1–10 allocate from North Garage spots; Gate 11–20 from East Garage spots).
  3. Stream occupancy updates asynchronously via Apache Kafka to update global display totals without bottlenecking the critical gate opening path.
