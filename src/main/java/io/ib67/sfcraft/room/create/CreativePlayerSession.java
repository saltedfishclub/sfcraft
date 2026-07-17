package io.ib67.sfcraft.room.create;

import io.ib67.sfcraft.module.room.CreativeRoomModule;
import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomSession;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;

public class CreativePlayerSession extends RoomSession {
    private final GlobalPos spawn;
    protected final CreativeRoomModule module;

    protected CreativePlayerSession(Room room, CreativeRoomModule module) {
        super(room);
        this.module = module;
        spawn = new GlobalPos(
                CreativeSpaceRoom.WORLD,
                CreativeSpaceRoom.SPAWN_POS
        );
    }

    @Override
    public void onPlayerJoin(ServerPlayer player) {
        module.onPlayerJoin(player);
    }

    @Override
    public GlobalPos getSpawnPosition() {
        return spawn;
    }

    @Override
    public void onPlayerLogin(UUID uuid) {

    }

    @Override
    public void onPlayerQuit(ServerPlayer player) {
        module.onPlayerQuit(player);
    }
}
