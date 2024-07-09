package io.ib67.sfcraft.mixin.server.bridge;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.SleepManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerWorld.class)
public interface ServerWorldBridge {
    @Accessor
    SleepManager getSleepManager();
}
