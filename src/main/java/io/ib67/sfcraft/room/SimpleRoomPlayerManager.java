package io.ib67.sfcraft.room;

import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomPlayerManager;
import io.ib67.sfcraft.subserver.RoomSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.GlobalPos;

import java.util.*;

public abstract class SimpleRoomPlayerManager implements RoomPlayerManager {
    protected final Map<UUID, PlayerEntry> onlinePlayers = new HashMap<>();
    protected final Room room;

    protected SimpleRoomPlayerManager(Room room) {
        this.room = room;
    }

    @Override
    public List<ServerPlayerEntity> getJoinedPlayers() {
        return onlinePlayers.values().stream().map(it -> it.player).toList();
    }

    @Override
    public List<RoomSession> getSessions() {
        return onlinePlayers.values().stream().map(it -> it.session).toList();
    }

    @Override
    public RoomSession createSessionFor(UUID uuid) {
        return onlinePlayers.computeIfAbsent(uuid, this::makeSession).getSession();
    }

    private PlayerEntry makeSession(UUID uuid) {
        var session = createSession(uuid);
        return new PlayerEntry(null, new DelegatedSession(session));
    }

    protected abstract RoomSession createSession(UUID id);

    @Override
    public RoomSession getSessionBy(UUID uuid) {
        var proc = onlinePlayers.get(uuid);
        if (proc == null) return null;
        return proc.session;
    }

    @Override
    public ServerPlayerEntity getPlayer(UUID uuid) {
        return onlinePlayers.get(uuid).getPlayer();
    }

    class DelegatedSession extends RoomSession {
        private final RoomSession session;

        protected DelegatedSession(RoomSession session) {
            super(SimpleRoomPlayerManager.this.room);
            this.session = session;
        }

        @Override
        public GlobalPos getSpawnPosition() {
            return session.getSpawnPosition();
        }

        @Override
        public void onPlayerLogin(UUID uuid) {
            session.onPlayerLogin(uuid);
        }

        @Override
        public void onPlayerJoin(ServerPlayerEntity player) {
            onlinePlayers.get(player.getUuid()).setPlayer(player);
            session.onPlayerJoin(player);
        }

        @Override
        public void onPlayerQuit(ServerPlayerEntity player) {
            onlinePlayers.remove(player.getUuid());
            session.onPlayerQuit(player);
        }
    }

    @Data
    @AllArgsConstructor
    class PlayerEntry {
        private ServerPlayerEntity player;
        private RoomSession session;
    }
}
