package io.ib67.sfcraft.registry;

import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomFactory;
import io.ib67.sfcraft.subserver.RoomSession;
import java.util.Collection;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public interface RoomRegistry {
    Collection<? extends Room> getRooms();

    Room getRoomBy(ResourceLocation identifier);

    RoomSession getRoomBy(UUID player);

    <T extends Room> T createRoomOf(Class<T> type, ResourceLocation roomId, ServerPlayer issuer, String... arguments);

    boolean isRoomWorld(ResourceKey<Level> world);

    <T extends Room> void registerRoomType(Class<T> type, ResourceKey<Level> worldKey, RoomFactory<T> factory);
}
