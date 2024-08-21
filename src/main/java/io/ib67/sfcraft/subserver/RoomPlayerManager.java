package io.ib67.sfcraft.subserver;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;

public interface RoomPlayerManager {
    List<ServerPlayerEntity> getJoinedPlayers();

    List<RoomSession> getSessions();

    RoomSession createSessionFor(UUID uuid);

    RoomSession getSessionBy(UUID uuid);

    ServerPlayerEntity getPlayer(UUID uuid);
}
