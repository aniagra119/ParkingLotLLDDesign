package com.parkinglot.event.listener;

import com.parkinglot.event.ParkingEvent;
import com.parkinglot.event.ParkingEventListener;
import com.parkinglot.event.VehicleExitedEvent;
import com.parkinglot.event.VehicleParkedEvent;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concrete Observer: Updates real-time LED display boards with active parking occupancy.
 */
public class DisplayBoardListener implements ParkingEventListener {
    private final AtomicInteger totalOccupied = new AtomicInteger(0);
    private final Map<Integer, AtomicInteger> floorOccupancy = new ConcurrentHashMap<>();

    @Override
    public void onEvent(ParkingEvent event) {
        if (event instanceof VehicleParkedEvent parked) {
            totalOccupied.incrementAndGet();
            floorOccupancy.computeIfAbsent(parked.floorNumber(), k -> new AtomicInteger(0))
                    .incrementAndGet();
        } else if (event instanceof VehicleExitedEvent exited) {
            totalOccupied.decrementAndGet();
            floorOccupancy.computeIfAbsent(exited.floorNumber(), k -> new AtomicInteger(0))
                    .decrementAndGet();
        }
    }

    public int getTotalOccupied() {
        return Math.max(0, totalOccupied.get());
    }

    public int getOccupiedOnFloor(int floorNumber) {
        AtomicInteger count = floorOccupancy.get(floorNumber);
        return count != null ? Math.max(0, count.get()) : 0;
    }

    public Map<Integer, Integer> getAllFloorOccupancy() {
        Map<Integer, Integer> snapshot = new ConcurrentHashMap<>();
        floorOccupancy.forEach((floor, count) -> snapshot.put(floor, Math.max(0, count.get())));
        return Collections.unmodifiableMap(snapshot);
    }
}
