package io.ib67.sfcraft.mixin.server;

import net.minecraft.server.world.ChunkLevelManager;
import net.minecraft.server.world.ServerChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkManager.class)
public interface ServerChunkManagerMixin {
    @Accessor("levelManager")
    ChunkLevelManager getLevelManager();
}
