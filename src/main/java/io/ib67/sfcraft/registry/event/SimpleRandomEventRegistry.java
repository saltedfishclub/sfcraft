package io.ib67.sfcraft.registry.event;

import io.ib67.sfcraft.event.RandomEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class SimpleRandomEventRegistry implements RandomEventRegistry {
    private final List<RegisteredEntry> entries = new ArrayList<>();
    private final List<StartedEntry> activeEvents = new ArrayList<>();

    protected SimpleRandomEventRegistry() {
        ServerTickEvents.END_WORLD_TICK.register(this::afterWorldTick);
    }

    private void afterWorldTick(ServerWorld serverWorld) {
        for (RegisteredEntry entry : entries) {
            if (entry.world == serverWorld.getRegistryKey()
                    && !entry.started && entry.worldTickPredicate.test(serverWorld)) {
                var event = entry.event.apply(serverWorld);
                var ticks = event.start();
                if (ticks > 0) {
                    activeEvents.add(new StartedEntry(event, () -> entry.started = false, ticks));
                }
            }
        }
        var it = activeEvents.iterator();
        while (it.hasNext()) {
            var entry = it.next();
            entry.event.onUpdate(++entry.ticks);
            if (entry.ticks >= entry.maxTicks) {
                it.remove();
                entry.event.end();
                entry.afterEvent.run();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ActiveEvent> getActiveEvents() {
        return (List<ActiveEvent>) (Object) activeEvents;
    }

    @Override
    public void registerEvent(Function<World, RandomEvent> event, RegistryKey<World> world, Predicate<World> worldTickPredicate) {
        entries.add(new RegisteredEntry(world, event, worldTickPredicate));
    }

    @RequiredArgsConstructor
    private static final class StartedEntry implements ActiveEvent {
        @Getter
        private final RandomEvent event;
        private final Runnable afterEvent;
        private final int maxTicks;
        private int ticks;

        @Override
        public int getRemainingTicks() {
            return maxTicks - ticks;
        }
    }

    @RequiredArgsConstructor
    private static final class RegisteredEntry {
        private final RegistryKey<World> world;
        private final Function<World, RandomEvent> event;
        private final Predicate<World> worldTickPredicate;
        private boolean started;
    }
}