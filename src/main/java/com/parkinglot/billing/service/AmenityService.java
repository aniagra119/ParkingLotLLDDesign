package com.parkinglot.billing.service;

/**
 * Domain interface for any value-added amenity service provided to a vehicle.
 * Encapsulates the specific mathematical pricing algorithm and receipt description
 * for each individual service (e.g. kWh-based, package-based, hourly-based).
 */
public interface AmenityService {
    double calculateCost();
    String getItemizedDescription();
}
