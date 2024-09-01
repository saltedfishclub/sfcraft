package io.ib67.sfcraft.registry;

import io.ib67.sfcraft.module.randomevt.ActiveEvent;
import io.ib67.sfcraft.module.randomevt.RandomEvent;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface RandomEventRegistry {
    List<ActiveEvent> getActiveEvents();

    void registerEvent(Function<World, RandomEvent> event, RegistryKey<World> world, Predicate<World> worldTickPredicate);
}
