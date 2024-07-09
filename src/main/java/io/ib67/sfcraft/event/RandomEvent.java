package io.ib67.sfcraft.event;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public abstract class RandomEvent {
    /**
     * @return duration, -1 for no-update and {@link #end()} will not be called.
     */
    public abstract int start();

    public abstract void onUpdate(int ticks);

    public abstract void end();
}
