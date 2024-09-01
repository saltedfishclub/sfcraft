package io.ib67.sfcraft.registry;

import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomFactory;
import io.ib67.sfcraft.subserver.RoomSession;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.UUID;

public interface RoomRegistry {
    Collection<? extends Room> getRooms();

    Room getRoomBy(Identifier identifier);

    RoomSession getRoomBy(UUID player);

    <T extends Room> T createRoomOf(Class<T> type, Identifier roomId, ServerPlayerEntity issuer, String... arguments);

    boolean isRoomWorld(RegistryKey<World> world);

    <T extends Room> void registerRoomType(Class<T> type, RegistryKey<World> worldKey, RoomFactory<T> factory);
}
