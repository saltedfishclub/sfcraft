package io.ib67.sfcraft.subserver;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.resources.Identifier;

public interface Room {
    Identifier getServerIdentifier();

    RoomPlayerManager getPlayerManager();

    void shutdown();
}
