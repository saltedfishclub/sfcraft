package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.server.world.ChunkLevelManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkManager.class)
public interface ServerChunkManagerBridge {
    @Accessor("levelManager")
    ChunkLevelManager getLevelManager();
}
