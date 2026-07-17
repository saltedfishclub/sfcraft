package io.ib67.sfcraft.subserver;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public interface RoomPlayerManager {
    List<ServerPlayer> getJoinedPlayers();

    List<RoomSession> getSessions();

    RoomSession createSessionFor(UUID uuid);

    RoomSession getSessionBy(UUID uuid);

    ServerPlayer getPlayer(UUID uuid);
}
