package io.ib67.sfcraft.module.randomevt;

public interface ActiveEvent {
    RandomEvent getEvent();
    int getRemainingTicks();
}
