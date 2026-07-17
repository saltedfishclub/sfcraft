package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerBridge {
    @Accessor
    void setLastActionTime(long time);
}
