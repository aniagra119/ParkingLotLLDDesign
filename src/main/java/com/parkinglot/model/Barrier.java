package com.parkinglot.model;

import com.parkinglot.command.BarrierExecution;
import com.parkinglot.enums.BarrierStatus;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Domain entity representing the physical boom barrier hardware at a gate.
 * Maintains barrier state (LOWERED/RAISED) and command audit history.
 */
public class Barrier {
    private final String barrierId;
    private volatile BarrierStatus status;
    private final List<BarrierExecution> auditHistory;

    public Barrier(String barrierId) {
        this.barrierId = Objects.requireNonNull(barrierId, "Barrier ID cannot be null");
        this.status = BarrierStatus.LOWERED;
        this.auditHistory = new CopyOnWriteArrayList<>();
    }

    public synchronized void raise() {
        this.status = BarrierStatus.RAISED;
    }

    public synchronized void lower() {
        this.status = BarrierStatus.LOWERED;
    }

    public boolean isRaised() {
        return this.status == BarrierStatus.RAISED;
    }

    public BarrierStatus getStatus() {
        return status;
    }

    public String getBarrierId() {
        return barrierId;
    }

    public void recordExecution(BarrierExecution execution) {
        Objects.requireNonNull(execution, "Execution cannot be null");
        this.auditHistory.add(execution);
    }

    public List<BarrierExecution> getAuditHistory() {
        return Collections.unmodifiableList(auditHistory);
    }
}
