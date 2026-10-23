package com.parkinglot.service;

import com.parkinglot.command.CloseBarrierCommand;
import com.parkinglot.command.OpenBarrierCommand;
import com.parkinglot.enums.GateType;
import com.parkinglot.model.Gate;
import com.parkinglot.model.User;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages physical gates and enforces entry/exit validation rules.
 * Uses Command Pattern to actuate barriers with full operator audit logging.
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

    public void openBarrier(String gateId, User operator) {
        Gate gate = gates.get(gateId);
        if (gate != null) {
            new OpenBarrierCommand(gate.getBarrier()).execute(operator);
        }
    }

    public void closeBarrier(String gateId, User operator) {
        Gate gate = gates.get(gateId);
        if (gate != null) {
            new CloseBarrierCommand(gate.getBarrier()).execute(operator);
        }
    }

    public void cycleGate(String gateId, User operator) {
        Gate gate = gates.get(gateId);
        if (gate != null) {
            new OpenBarrierCommand(gate.getBarrier()).execute(operator);
            new CloseBarrierCommand(gate.getBarrier()).execute(operator);
        }
    }

    public void cycleGate(String gateId) {
        cycleGate(gateId, User.SYSTEM_KIOSK);
    }

    public void openBarrier(String gateId) {
        openBarrier(gateId, User.SYSTEM_KIOSK);
    }

    public void closeBarrier(String gateId) {
        closeBarrier(gateId, User.SYSTEM_KIOSK);
    }

    public Map<String, Gate> getGates() {
        return Collections.unmodifiableMap(gates);
    }
}
