package io.ib67.sfcraft.subserver;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface RoomFactory<T extends Room> {
    T create(ServerPlayer issuer, String[] arguments);
}
