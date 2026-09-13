package com.parkinglot.command;

import com.parkinglot.model.Barrier;
import com.parkinglot.model.User;
import java.util.Objects;

/**
 * Concrete Command that lowers the boom barrier after vehicle passage.
 */
public class CloseBarrierCommand implements BarrierCommand {
    private final Barrier barrier;

    public CloseBarrierCommand(Barrier barrier) {
        this.barrier = Objects.requireNonNull(barrier, "Barrier cannot be null");
    }

    @Override
    public void execute(User operator) {
        User actor = operator != null ? operator : User.SYSTEM_KIOSK;
        barrier.lower();
        barrier.recordExecution(new BarrierExecution(barrier.getBarrierId(), getCommandName(), actor));
    }

    @Override
    public String getCommandName() {
        return "CLOSE_BARRIER";
    }

    public Barrier getBarrier() {
        return barrier;
    }
}
