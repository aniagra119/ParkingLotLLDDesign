# Multi-Floor Parking Lot Low-Level Design (LLD)

A production-grade, extensible Low-Level Design (LLD) for a multi-floor, multi-gate automated parking lot system in Java 21, built using first-principles Object-Oriented Design and Gang of Four (GoF) design patterns.

---

## 1. System Overview & Problem Statement

### Core Requirements
1. **Multi-Floor Hierarchy**: Multiple physical floors, each containing designated spots for `BIKE`, `CAR`, and `TRUCK`.
2. **Strict 1:1 Spot Compatibility**: 
   - A Car spot cannot park bikes; a Truck spot cannot park cars.
   - Enforced by `ParkingSpotType.canAccommodate(VehicleType)`.
3. **Automated Ticket Issuance**:
   - Unique UUID ticket generated on entry.
   - Records `spotId`, `floorNumber`, `vehicle`, and `entryTime`.
   - Thread-safe `markPaid()` guarding against duplicate payment exploitation.
4. **Spatial Spot Allocation (Strategy Pattern)**:
   - Pluggable allocation algorithm.
   - Default `NearestAvailableSpotStrategy` finds the closest vacant spot using floor elevation delta first, then 2D Manhattan distance ($|x_1 - x_2| + |y_1 - y_2|$).
5. **Progressive Tiered Hourly Slabs (Template Strategy)**:
   - Partial hours are always rounded up to the nearest whole hour ceiling ($\lceil \text{hours} \rceil$).
   - `SlabBasedFeeStrategy` defines the template algorithm; `TieredHourlyFeeStrategy` calculates the progressive sum over slabs.
   - **The ₹180.00 Car Benchmark**: A Car parked for 5.5 hours = 6 billable hours:
     - 0–2h (2 hrs @ ₹25/h) = ₹50
     - 2–4h (2 hrs @ ₹30/h) = ₹60
     - 4–6h (2 hrs @ ₹35/h) = ₹70
     - **Total Base Fee = ₹180.00**.
6. **Dynamic Amenity Services & Itemized Billing (Polymorphic Aggregation)**:
   - Avoids the fragile Decorator anti-pattern on persistent invoice data in favor of clean list aggregation (`List<AmenityService>`).
   - Dynamic self-encapsulating formulas:
     - `EvChargingService`: Energy-based formula ($\text{kWh Consumed} \times \text{Rate per kWh}$).
     - `CarWashService`: Dynamic formula ($\text{WashPackage} + \text{VehicleType Surcharge}$).
   - `Bill` provides full itemized breakdowns with clean line items, supporting standard database persistence and JSON serialization.
7. **Payment Adapter & Simple Factory**:
   - External vendor SDKs (e.g. Razorpay) require amounts in paise and vendor-specific payloads.
   - `RazorpayPaymentAdapter` bridges vendor APIs into our domain `PaymentProcessor`.
   - `PaymentProcessorFactory` selects the correct processor from `PaymentMode` (`RAZORPAY_UPI`, `RAZORPAY_CARD`, `CASH`).

---

## 2. Hourly Pricing Rate Matrix

| Vehicle Type | 0 - 2 Hours | 2 - 4 Hours | 4 - 6 Hours | 6+ Hours |
| :--- | :--- | :--- | :--- | :--- |
| **BIKE** | ₹20 / hr | ₹25 / hr | ₹30 / hr | ₹40 / hr |
| **CAR** | ₹25 / hr | ₹30 / hr | ₹35 / hr | ₹45 / hr |
| **TRUCK** | ₹50 / hr | ₹60 / hr | ₹65 / hr | ₹80 / hr |

---

## 3. Design Patterns Applied

| Pattern | Category | Where Applied | Concrete Architectural Justification |
| :--- | :--- | :--- | :--- |
| **Facade** | Structural | `ParkingLot` class | Hides complex subsystem orchestration (`FloorManager`, `GateManager`, `Billing`) behind clean methods (`parkVehicle`, `processExit`). |
| **Strategy (x2)** | Behavioral | `com.parkinglot.strategy.assignment`<br>`com.parkinglot.strategy.fee` | **1.** Swaps spatial spot allocation heuristics (`NearestAvailableSpotStrategy`).<br>**2.** Swaps mathematical fee algorithms (`TieredHourlyFeeStrategy`, `FlatRateFeeStrategy`). |
| **Adapter** | Structural | `com.parkinglot.payment.adapter.RazorpayPaymentAdapter` | Converts proprietary 3rd-party vendor SDK calls (`ExternalRazorpayClient`) to our domain `PaymentProcessor` contract. |
| **Factory** | Creational | `PaymentProcessorFactory` | Centralizes runtime instantiation of payment adapters based on `PaymentMode`. |
| **Builder** | Creational | `ParkingLotBuilder` | Assembles complex multi-floor, multi-gate parking lot hierarchies cleanly. |

---

## 4. Architectural Deep-Dives for Revision

For detailed notes on the design trade-offs, see [ParkingLotDesign.md](ParkingLotDesign.md):
- **Strategy vs. Adapter + Factory**: Why payment is NOT a Strategy.
- **Child-Parent Relationships**: Why `ParkingSpot` stores `floorNumber` instead of full `ParkingFloor`.
- **Manhattan Distance**: Why grid distance ($|x_1-x_2| + |y_1-y_2|$) reflects physical driving aisles better than Euclidean straight lines.
- **Dynamic Amenity Billing**: Why services encapsulate their own math (`AmenityService`) rather than hardcoding static fees.
