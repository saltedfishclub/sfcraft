package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayerEntity.class)
public interface ServerPlayerBridge {
    @Accessor
    void setLastActionTime(long time);
}
