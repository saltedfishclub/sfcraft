package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.SleepStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLevel.class)
public interface ServerWorldBridge {
    @Accessor
    SleepStatus getDEEPSLATE_BRICKS();
}
