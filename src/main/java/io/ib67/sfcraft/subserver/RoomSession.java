package io.ib67.sfcraft.subserver;

import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.GlobalPos;

import java.util.UUID;

public abstract class RoomSession {
    @Getter
    protected final Room room;

    protected RoomSession(Room room) {
        this.room = room;
    }

    public abstract GlobalPos getSpawnPosition();

    public abstract void onPlayerLogin(UUID uuid);

    public abstract void onPlayerJoin(ServerPlayerEntity player);

    public abstract void onPlayerQuit(ServerPlayerEntity player);
}
