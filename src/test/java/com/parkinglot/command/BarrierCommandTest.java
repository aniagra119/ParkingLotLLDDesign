package com.parkinglot.command;

import com.parkinglot.enums.BarrierStatus;
import com.parkinglot.enums.GateType;
import com.parkinglot.enums.UserRole;
import com.parkinglot.model.Barrier;
import com.parkinglot.model.Coordinates;
import com.parkinglot.model.Gate;
import com.parkinglot.model.User;
import com.parkinglot.service.GateController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Command Pattern & Physical Barrier Hardware Tests")
class BarrierCommandTest {

    @Test
    @DisplayName("Barrier starts lowered and executes Open/Close commands with operator audit")
    void testBarrierCommandsWithAudit() {
        Barrier barrier = new Barrier("BARRIER-E1");
        assertEquals(BarrierStatus.LOWERED, barrier.getStatus());
        assertFalse(barrier.isRaised());

        User attendant = new User("ATT-001", "John Doe", UserRole.PARKING_ATTENDANT);

        BarrierCommand openCmd = new OpenBarrierCommand(barrier);
        openCmd.execute(attendant);

        assertEquals(BarrierStatus.RAISED, barrier.getStatus());
        assertTrue(barrier.isRaised());
        assertEquals(1, barrier.getAuditHistory().size());

        BarrierExecution entryLog = barrier.getAuditHistory().get(0);
        assertEquals("BARRIER-E1", entryLog.barrierId());
        assertEquals("OPEN_BARRIER", entryLog.commandName());
        assertEquals(attendant, entryLog.operator());
        assertNotNull(entryLog.executedAt());

        BarrierCommand closeCmd = new CloseBarrierCommand(barrier);
        closeCmd.execute(attendant);

        assertEquals(BarrierStatus.LOWERED, barrier.getStatus());
        assertFalse(barrier.isRaised());
        assertEquals(2, barrier.getAuditHistory().size());

        BarrierExecution exitLog = barrier.getAuditHistory().get(1);
        assertEquals("CLOSE_BARRIER", exitLog.commandName());
    }

    @Test
    @DisplayName("GateController uses Command Pattern to cycle barrier with operator tracking")
    void testGateControllerCommandActuation() {
        Gate gate = new Gate("GATE-ENTRY-1", GateType.ENTRY, 1, new Coordinates(0, 0));
        GateController controller = new GateController(List.of(gate));

        User operator = new User("SUP-01", "Alice Supervisor", UserRole.SUPERVISOR);

        controller.openBarrier("GATE-ENTRY-1", operator);
        assertTrue(gate.isOpen());
        assertEquals(1, gate.getBarrier().getAuditHistory().size());
        assertEquals(operator, gate.getBarrier().getAuditHistory().get(0).operator());

        controller.closeBarrier("GATE-ENTRY-1", operator);
        assertFalse(gate.isOpen());
        assertEquals(2, gate.getBarrier().getAuditHistory().size());

        // Cycle gate
        controller.cycleGate("GATE-ENTRY-1", operator);
        assertEquals(4, gate.getBarrier().getAuditHistory().size());
    }
}
