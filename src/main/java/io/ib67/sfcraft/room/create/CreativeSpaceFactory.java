package io.ib67.sfcraft.room.create;

import io.ib67.sfcraft.subserver.RoomFactory;
import lombok.RequiredArgsConstructor;
import net.minecraft.server.network.ServerPlayerEntity;

@RequiredArgsConstructor
public class CreativeSpaceFactory implements RoomFactory<CreativeSpaceRoom> {
    private final CreativeSpaceRoom room;

    @Override
    public CreativeSpaceRoom create(ServerPlayerEntity issuer, String[] arguments) {
        return room;
    }
}
