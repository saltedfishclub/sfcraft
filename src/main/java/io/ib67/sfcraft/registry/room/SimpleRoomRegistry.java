package io.ib67.sfcraft.registry.room;

import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomFactory;
import io.ib67.sfcraft.subserver.RoomSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class SimpleRoomRegistry implements RoomRegistry {
    private final ConcurrentMap<ResourceLocation, Room> rooms = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Class<?>, RoomFactory<?>> factories = new HashMap<>();
    private final Set<ResourceKey<Level>> roomWorlds = new HashSet<>();

    @Override
    public Collection<? extends Room> getRooms() {
        return rooms.values();
    }

    @Override
    public Room getRoomBy(ResourceLocation identifier) {
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
    public <T extends Room> T createRoomOf(Class<T> type, ResourceLocation roomId, ServerPlayer issuer, String... arguments) {
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
    public boolean isRoomWorld(ResourceKey<Level> world) {
        return roomWorlds.contains(world);
    }

    @Override
    public <T extends Room> void registerRoomType(Class<T> type, ResourceKey<Level> worldKey, RoomFactory<T> factory) {
        roomWorlds.add(worldKey);
        factories.put(type, factory);
    }
}
