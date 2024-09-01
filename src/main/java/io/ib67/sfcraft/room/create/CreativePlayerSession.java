package io.ib67.sfcraft.room.create;

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

    protected CreativePlayerSession(Room room) {
        super(room);
        spawn = new GlobalPos(
                CreativeSpaceRoom.WORLD,
                CreativeSpaceRoom.SPAWN_POS
        );
    }

    @Override
    public void onPlayerJoin(ServerPlayerEntity player) {
        player.sendMessage(Text.of("你现在正在创造游乐园中。"));
        player.sendMessage(Text.literal("游乐园中的生物不能逃逸到其他维度。").withColor(Colors.GRAY));
        player.sendMessage(Text.literal("使用 /reco 或重新加入游戏即可离开。").withColor(Colors.GRAY));
        player.changeGameMode(GameMode.CREATIVE);
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

    }
}
