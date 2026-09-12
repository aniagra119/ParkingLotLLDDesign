package com.parkinglot.service;

import com.parkinglot.enums.GateType;
import com.parkinglot.model.Gate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages physical gates and enforces entry/exit validation rules.
 * Single Responsibility: Gate lookup, access validation, and passage cycles.
 */
public class GateController {
    private final Map<String, Gate> gates;

    public GateController(List<Gate> gateList) {
        Objects.requireNonNull(gateList, "Gate list cannot be null");
        this.gates = new ConcurrentHashMap<>();
        for (Gate gate : gateList) {
            this.gates.put(gate.getGateId(), gate);
        }
    }

    public Gate getValidatedGate(String gateId, GateType expectedType) {
        Gate gate = gates.get(gateId);
        if (gate == null) {
            throw new IllegalArgumentException("Gate not found: " + gateId);
        }
        if (gate.getGateType() != expectedType) {
            throw new IllegalStateException("Gate " + gateId + " is not an " + expectedType + " gate");
        }
        return gate;
    }

    public void cycleGate(String gateId) {
        Gate gate = gates.get(gateId);
        if (gate != null) {
            gate.open();
            gate.close();
        }
    }

    public Map<String, Gate> getGates() {
        return Collections.unmodifiableMap(gates);
    }
}
