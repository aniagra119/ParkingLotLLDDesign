package com.parkinglot.strategy;

import com.parkinglot.enums.ParkingSpotType;
import com.parkinglot.model.Coordinates;
import com.parkinglot.model.ParkingFloor;
import com.parkinglot.model.ParkingSpot;
import java.util.List;
import java.util.Optional;

public interface ParkingAssignmentStrategy {
    Optional<ParkingSpot> findSpot(
            List<ParkingFloor> floors,
            ParkingSpotType spotType,
            int entryFloorNumber,
            Coordinates entryCoordinates
    );
}
