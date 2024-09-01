package io.ib67.sfcraft.subserver;

import net.minecraft.server.network.ServerPlayerEntity;

@FunctionalInterface
public interface RoomFactory<T extends Room> {
    T create(ServerPlayerEntity issuer, String[] arguments);
}
