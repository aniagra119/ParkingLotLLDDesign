package com.parkinglot.model;

public record Coordinates(int x, int y) {
    public Coordinates {
        if (x < 0) {
            throw new IllegalArgumentException("x coordinate cannot be negative");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y coordinate cannot be negative");
        }
    }

    public int manhattanDistance(Coordinates other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }
}
