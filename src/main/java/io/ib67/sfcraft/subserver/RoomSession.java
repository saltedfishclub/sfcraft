package io.ib67.sfcraft.subserver;

import lombok.Getter;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public abstract class RoomSession {
    @Getter
    protected final Room room;

    protected RoomSession(Room room) {
        this.room = room;
    }

    public abstract GlobalPos getSpawnPosition();

    public abstract void onPlayerLogin(UUID uuid);

    public abstract void onPlayerJoin(ServerPlayer player);

    public abstract void onPlayerQuit(ServerPlayer player);
}
