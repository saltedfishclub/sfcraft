package io.ib67.sfcraft.room.create;

import io.ib67.sfcraft.module.room.CreativeRoomModule;
import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomSession;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.GameMode;

import java.util.UUID;

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
    public void onPlayerJoin(ServerPlayerEntity player) {
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
    public void onPlayerQuit(ServerPlayerEntity player) {
        module.onPlayerQuit(player);
    }
}
