package io.ib67.sfcraft.room.create;

import io.ib67.sfcraft.room.SimpleRoomPlayerManager;
import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomSession;

import java.util.UUID;

public class CreativePlayerManager extends SimpleRoomPlayerManager {
    protected CreativePlayerManager(Room room) {
        super(room);
    }

    @Override
    protected RoomSession createSession(UUID id) {
        return new CreativePlayerSession(room);
    }
}
