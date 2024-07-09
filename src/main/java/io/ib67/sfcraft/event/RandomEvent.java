package io.ib67.sfcraft.event;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public abstract class RandomEvent {
    /**
     * @return duration
     */
    public abstract int start();

    public abstract void onUpdate(int ticks);

    public abstract void end();
}
