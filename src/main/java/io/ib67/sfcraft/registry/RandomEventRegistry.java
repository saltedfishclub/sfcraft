package io.ib67.sfcraft.registry;

import io.ib67.sfcraft.module.randomevt.ActiveEvent;
import io.ib67.sfcraft.module.randomevt.RandomEvent;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface RandomEventRegistry {
    List<ActiveEvent> getActiveEvents();

    void registerEvent(Function<Level, RandomEvent> event, ResourceKey<Level> world, Predicate<Level> worldTickPredicate);
}
