package io.ib67.sfcraft.room.create;

import io.ib67.sfcraft.module.room.CreativeRoomModule;
import io.ib67.sfcraft.room.SimpleRoomPlayerManager;
import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomSession;

import java.util.UUID;

public class CreativePlayerManager extends SimpleRoomPlayerManager {
    protected final CreativeRoomModule module;

    protected CreativePlayerManager(Room room, CreativeRoomModule module) {
        super(room);
        this.module = module;
    }

    @Override
    protected RoomSession createSession(UUID id) {
        return new CreativePlayerSession(room, module);
    }
}
