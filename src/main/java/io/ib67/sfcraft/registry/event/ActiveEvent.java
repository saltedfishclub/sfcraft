package io.ib67.sfcraft.registry.event;

import io.ib67.sfcraft.event.RandomEvent;

public interface ActiveEvent {
    RandomEvent getEvent();
    int getRemainingTicks();
}
