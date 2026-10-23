# Multi-Floor Parking Lot Low-Level Design (LLD)

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge&logo=apachemaven&logoColor=white)](pom.xml)
[![Tests](https://img.shields.io/badge/Tests-70%20Passing-success?style=for-the-badge)](src/test)
[![Design Patterns](https://img.shields.io/badge/Design_Patterns-GoF_%26_SOLID-blueviolet?style=for-the-badge)](ParkingLotDesign.md)

A production-grade, extensible Low-Level Design (LLD) for a multi-floor, multi-gate automated parking lot system in Java 21, built using first-principles Object-Oriented Design and Gang of Four (GoF) design patterns.

---

## 1. System Requirements & Domain Scope

1. **Multi-Floor Hierarchy**: Multiple physical floors, each containing designated spots for `BIKE`, `CAR`, and `TRUCK`.
2. **Strict 1:1 Spot Compatibility**: Enforced by `ParkingSpotType.canAccommodate(VehicleType)`.
3. **Automated Ticket Issuance with Operator Auditing**: Entry ticket with timestamp, vehicle plate, spot, and operator (`User issuedBy`).
4. **Spatial Spot Allocation (Strategy Pattern)**: Nearest available spot allocation using floor elevation delta first, then 2D Manhattan distance ($|x_1 - x_2| + |y_1 - y_2|$).
5. **Progressive Tiered Hourly Slabs (Template Strategy)**: Ceiling-rounded duration ($\lceil \text{hours} \rceil$) with progressive pricing slabs. The **₹180.00 Car Benchmark** (5.5 hrs $\to$ 6 hrs = ₹50 + ₹60 + ₹70).
6. **Hardware Actuation (Command Pattern)**: Gates physically own `Barrier` hardware, actuated via `OpenBarrierCommand` and `CloseBarrierCommand` with full operator audit logging.
7. **Itemized Billing (Decorator Pattern)**: Composable invoices (`BillableItem`, `BaseParkingCharge`, `BillableItemDecorator`, `AmenityServiceDecorator`) dynamically decorating base parking fees with EV charging and car wash services.
8. **Real-Time Event Bus (Observer Pattern)**: Event-driven decoupling (`VehicleParkedEvent`, `VehicleExitedEvent`) with live occupancy LED boards (`DisplayBoardListener`) and driver SMS notifications (`NotificationListener`).
9. **Hexagonal Payment Architecture**: Pluggable adapters (`RazorpayPaymentAdapter`, `CashPaymentProcessor`) producing immutable audit trails (`PaymentTransaction`).

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
| **Facade** | Structural | `ParkingLot` class | Orchestrates specialized delegates (`GateController`, `TicketManager`, `BillingService`, `PaymentService`). |
| **Strategy (x2)** | Behavioral | `ParkingAssignmentStrategy`<br>`FeeCalculationStrategy` | **1.** Swaps spatial spot allocation heuristics.<br>**2.** Swaps progressive fee slab calculations. |
| **Command** | Behavioral | `BarrierCommand`<br>`OpenBarrierCommand`<br>`CloseBarrierCommand` | Encapsulates barrier hardware movements with operator audit logging. |
| **Decorator** | Structural | `BillableItemDecorator`<br>`AmenityServiceDecorator` | Dynamically decorates base charges with consumed amenities using the Template Method pattern inside the decorator. |
| **Observer** | Behavioral | `ParkingEventPublisher`<br>`ParkingEventListener` | Decouples core parking lot logic from LED display updates and driver SMS receipts. |
| **Adapter** | Structural | `RazorpayPaymentAdapter` | Bridges third-party SDK calls (`ExternalRazorpayClient`) into clean domain contracts. |
| **Factory** | Creational | `PaymentProcessorFactory` | Centralizes runtime instantiation of payment adapters from `PaymentMode`. |
| **Builder** | Creational | `ParkingLotBuilder`<br>`Vehicle.Builder` | Assembles complex lots and builds rich vehicle records without telescoping constructors. |

---

## 4. Package Structure & Bounded Contexts

```
com.parkinglot/
├── ParkingLot.java                         # Facade entry point
├── builder/
│   └── ParkingLotBuilder.java              # Fluent builder for parking lot hierarchy
├── command/                                # Command Pattern for barrier actuation
│   ├── BarrierCommand.java
│   ├── OpenBarrierCommand.java
│   ├── CloseBarrierCommand.java
│   └── BarrierExecution.java
├── config/                                 # Operational policy configuration
│   └── ParkingConfiguration.java
├── enums/                                  # Domain enumerations
│   ├── BarrierStatus.java
│   ├── BillStatus.java
│   ├── GateType.java
│   ├── ParkingSpotType.java
│   ├── PaymentMode.java
│   ├── TicketStatus.java
│   ├── UserRole.java
│   ├── VehicleType.java
│   └── WashPackage.java
├── event/                                  # Observer Pattern event bus
│   ├── ParkingEvent.java
│   ├── ParkingEventListener.java
│   ├── ParkingEventPublisher.java
│   ├── VehicleExitedEvent.java
│   ├── VehicleParkedEvent.java
│   └── listener/
│       ├── DisplayBoardListener.java
│       └── NotificationListener.java
├── model/                                  # Domain entities & value objects
│   ├── Barrier.java
│   ├── Coordinates.java
│   ├── Gate.java
│   ├── ParkingFloor.java
│   ├── ParkingSpot.java
│   ├── Ticket.java
│   ├── User.java
│   └── Vehicle.java
├── billing/                                # Billing & settlement domain
│   ├── Bill.java
│   ├── BillingService.java
│   ├── decorator/                          # Decorator Pattern for itemized invoices
│   │   ├── BillableItem.java
│   │   ├── BaseParkingCharge.java
│   │   ├── BillableItemDecorator.java
│   │   └── AmenityServiceDecorator.java
│   └── service/
│       ├── AmenityService.java
│       ├── CarWashService.java
│       └── EvChargingService.java
├── payment/                                # Hexagonal payment domain
│   ├── PaymentProcessor.java
│   ├── PaymentProcessorFactory.java
│   ├── PaymentService.java
│   ├── PaymentStatus.java
│   ├── PaymentTransaction.java
│   ├── adapter/
│   │   └── RazorpayPaymentAdapter.java
│   ├── external/
│   │   └── ExternalRazorpayClient.java
│   └── processor/
│       └── CashPaymentProcessor.java
├── service/                                # Subsystem delegates
│   ├── GateController.java
│   └── TicketManager.java
└── strategy/                               # Strategy patterns
    ├── assignment/
    │   ├── NearestAvailableSpotStrategy.java
    │   └── ParkingAssignmentStrategy.java
    └── fee/
        ├── FeeCalculationStrategy.java
        ├── FlatRateFeeStrategy.java
        └── slab/
            ├── PricingSlab.java
            ├── SlabBasedFeeStrategy.java
            └── TieredHourlyFeeStrategy.java
```

---

## 5. Build & Test Verification

```bash
mvn clean test
```

70 automated tests covering:
- Spatial distance heuristics & elevation preferences
- Progressive hourly pricing & the ₹180.00 Car benchmark
- Physical barrier actuation & command execution audits
- Decorator pattern itemized billing & amenity chaining
- Event-driven live occupancy tracking & driver SMS dispatching
- Operator accountability on tickets and payment transactions
- Concurrency and thread safety across entry/exit gates
