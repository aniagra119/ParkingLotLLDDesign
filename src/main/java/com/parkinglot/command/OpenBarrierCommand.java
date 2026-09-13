package com.parkinglot.command;

import com.parkinglot.model.Barrier;
import com.parkinglot.model.User;
import java.util.Objects;

/**
 * Concrete Command that raises the boom barrier to allow vehicle ingress or egress.
 */
public class OpenBarrierCommand implements BarrierCommand {
    private final Barrier barrier;

    public OpenBarrierCommand(Barrier barrier) {
        this.barrier = Objects.requireNonNull(barrier, "Barrier cannot be null");
    }

    @Override
    public void execute(User operator) {
        User actor = operator != null ? operator : User.SYSTEM_KIOSK;
        barrier.raise();
        barrier.recordExecution(new BarrierExecution(barrier.getBarrierId(), getCommandName(), actor));
    }

    @Override
    public String getCommandName() {
        return "OPEN_BARRIER";
    }

    public Barrier getBarrier() {
        return barrier;
    }
}
