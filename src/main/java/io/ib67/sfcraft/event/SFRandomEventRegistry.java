package io.ib67.sfcraft.event;

import io.ib67.sfcraft.event.longnight.DawnAfterLongNightEvent;
import io.ib67.sfcraft.event.longnight.LongNightEvent;
import io.ib67.sfcraft.registry.event.SimpleRandomEventRegistry;
import net.minecraft.world.World;

public class SFRandomEventRegistry extends SimpleRandomEventRegistry {
    public SFRandomEventRegistry() {
        registerEvent(LongNightEvent::new, World.OVERWORLD, world -> world.getTimeOfDay() % 18000 == 0); // At midnight
        registerEvent(DawnAfterLongNightEvent::new, World.OVERWORLD, world -> world.getTimeOfDay() % 22200 == 0); // At dawn
    }
}
