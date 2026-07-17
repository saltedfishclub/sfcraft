package io.ib67.sfcraft.subserver;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public interface Room {
    ResourceLocation getServerIdentifier();

    RoomPlayerManager getPlayerManager();

    void shutdown();
}
