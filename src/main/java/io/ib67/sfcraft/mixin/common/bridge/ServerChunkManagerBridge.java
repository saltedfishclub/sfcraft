package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkCache.class)
public interface ServerChunkManagerBridge {
    @Accessor("distanceManager")
    DistanceManager getLevelManager();
}
