# Multi-Floor Parking Lot System — Low-Level Design (LLD)

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge&logo=apachemaven&logoColor=white)](pom.xml)
[![Design Patterns](https://img.shields.io/badge/Design_Patterns-GoF_%26_SOLID-blueviolet?style=for-the-badge)](ParkingLotDesign.md)

A production-grade, object-oriented Low-Level Design for an automated Multi-Floor Parking Lot system with multi-vehicle support, dynamic spot allocation, tiered fee calculation, and payment gateway integration in Java 21 adhering to clean architecture, SOLID principles, and GoF design patterns.

---

## 1. System Requirements & Feature Matrix

| # | Requirement | Implementation Details | Design Pattern / Component |
| :---: | :--- | :--- | :--- |
| **1** | **Multi-Floor Structure** | Multi-level facility with categorized spots (`BIKE`, `CAR`, `TRUCK`). | `ParkingLot`, `ParkingFloor` |
| **2** | **Vehicle & Spot Segregation** | Vehicle size constraint validation; strict prevention of cross-parking. | `Vehicle`, `ParkingSpot`, `ParkingSpotType` |
| **3** | **Multiple Entry & Exit Gates** | Concurrent entry gates with operator ticket issuing; exit gates processing billing. | `EntryGate`, `ExitGate`, `Operator` |
| **4** | **Pluggable Spot Assignment** | Nearest available spot allocation (distance-aware across floors & coordinates). | `ParkingAssignmentStrategy`, `NearestAvailableSpotStrategy` (Strategy Pattern) |
| **5** | **Ticket Lifecycle** | Entry ticket with timestamp, vehicle number, and allocated spot. | `Ticket` (Domain Entity) |
| **6** | **Tiered Progressive Fee Model** | Hourly slab billing with partial-hour ceiling round-up. | `FeeCalculationStrategy`, `TieredHourlyFeeStrategy` (Strategy Pattern) |
| **7** | **Payment Gateway Integration** | Vendor-agnostic payment processing via Razorpay integration. | `PaymentProcessor`, `RazorpayPaymentAdapter` (Adapter Pattern) |
| **8** | **Thread Safety & Double-Booking Guard** | Prevents race conditions when multiple gates allocate the same spot simultaneously. | Atomic spot locking (`ParkingSpot.assignVehicle()`) |
| **9** | **Fluent Assembly & Facade** | Step-by-step setup of floors, gates, and pricing strategies. | `ParkingLotBuilder` (Builder Pattern), `ParkingLot` (Facade Pattern) |

---

## 2. High-Level Architecture

```mermaid
graph TD
    Driver([Driver]) -->|Arrives at| EG[EntryGate]
    Driver -->|Departs via| XG[ExitGate]
    
    subgraph Gate & Orchestration Layer
        EG -->|Assigns spot via| PAS[ParkingAssignmentStrategy]
        EG -->|Issues| T[Ticket]
        XG -->|Calculates bill via| FCS[FeeCalculationStrategy]
        XG -->|Processes payment via| PP[PaymentProcessor Interface]
        PP --> RZ[RazorpayPaymentAdapter]
    end

    subgraph Core Domain Layer
        PL[ParkingLot Facade]
        PL --> PF[ParkingFloor]
        PF --> PS[ParkingSpot]
        PS --> V[Vehicle]
    end

    subgraph Billing Layer
        XG --> B[Bill]
        B --> P[Payment Transaction]
    end
```

---

## 3. Detailed Specifications

For complete mathematical fee models, class contracts, and design pattern justifications, see [ParkingLotDesign.md](ParkingLotDesign.md).
