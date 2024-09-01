package io.ib67.sfcraft.registry.room;

import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomFactory;
import io.ib67.sfcraft.subserver.RoomSession;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SimpleRoomRegistry implements RoomRegistry {
    private final ConcurrentMap<Identifier, Room> rooms = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Class<?>, RoomFactory<?>> factories = new HashMap<>();
    private final Set<RegistryKey<World>> roomWorlds = new HashSet<>();

    @Override
    public Collection<? extends Room> getRooms() {
        return rooms.values();
    }

    @Override
    public Room getRoomBy(Identifier identifier) {
        return rooms.get(identifier);
    }

    @Override
    public RoomSession getRoomBy(UUID player) {
        lock.readLock().lock();
        try {
            return rooms.values().stream()
                    .map(it -> it.getPlayerManager().getSessionBy(player))
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public <T extends Room> T createRoomOf(Class<T> type, Identifier roomId, ServerPlayerEntity issuer, String... arguments) {
        if (getRoomBy(roomId) != null) {
            throw new IllegalArgumentException("duplicate room id: " + roomId);
        }
        lock.writeLock().lock();
        try {
            var room = factories.get(type).create(issuer, arguments);
            rooms.put(roomId, room);
            return (T) room;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean isRoomWorld(RegistryKey<World> world) {
        return roomWorlds.contains(world);
    }

    @Override
    public <T extends Room> void registerRoomType(Class<T> type, RegistryKey<World> worldKey, RoomFactory<T> factory) {
        roomWorlds.add(worldKey);
        factories.put(type, factory);
    }
}
