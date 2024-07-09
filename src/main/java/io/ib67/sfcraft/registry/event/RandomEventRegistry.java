package io.ib67.sfcraft.registry.event;

import io.ib67.sfcraft.event.RandomEvent;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface RandomEventRegistry {
    List<ActiveEvent> getActiveEvents();
    void registerEvent(Function<World, RandomEvent> event, Predicate<World> worldTickPredicate);
}
