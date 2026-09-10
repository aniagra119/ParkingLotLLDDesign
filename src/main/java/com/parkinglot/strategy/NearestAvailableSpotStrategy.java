package com.parkinglot.strategy;

import com.parkinglot.enums.ParkingSpotType;
import com.parkinglot.model.Coordinates;
import com.parkinglot.model.ParkingFloor;
import com.parkinglot.model.ParkingSpot;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class NearestAvailableSpotStrategy implements ParkingAssignmentStrategy {

    @Override
    public Optional<ParkingSpot> findSpot(
            List<ParkingFloor> floors,
            ParkingSpotType spotType,
            int entryFloorNumber,
            Coordinates entryCoordinates) {

        if (floors == null || floors.isEmpty() || spotType == null || entryCoordinates == null) {
            return Optional.empty();
        }

        return floors.stream()
                // Sort floors by elevation distance from entry gate floor
                .sorted(Comparator.comparingInt(f -> Math.abs(f.getFloorNumber() - entryFloorNumber)))
                // Flatten available spots of the required type on that floor
                .flatMap(floor -> floor.getAvailableSpots(spotType).stream())
                // Find nearest spot using floor delta first, then Manhattan distance
                .min(Comparator.comparingInt((ParkingSpot s) -> Math.abs(s.getFloorNumber() - entryFloorNumber))
                        .thenComparingInt(s -> s.getCoordinates().manhattanDistance(entryCoordinates)));
    }
}
