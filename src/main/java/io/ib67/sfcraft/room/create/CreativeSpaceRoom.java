package io.ib67.sfcraft.room.create;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.room.CreativeRoomModule;
import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomPlayerManager;
import lombok.Getter;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CreativeSpaceRoom implements Room {
    public static final RegistryKey<World> WORLD = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(SFCraft.MOD_ID, "playground"));
    public static final Identifier IDENTIFIER = Identifier.of(SFCraft.MOD_ID, "creative_space");
    public static final BlockPos SPAWN_POS = new BlockPos(0, 66, 0);
    @Getter
    private final RoomPlayerManager playerManager;

    public CreativeSpaceRoom(CreativeRoomModule module) {
        this.playerManager = new CreativePlayerManager(this,module);
    }

    @Override
    public void shutdown() {
        for (ServerPlayerEntity joinedPlayer : playerManager.getJoinedPlayers()) {
            joinedPlayer.networkHandler.disconnect(Text.of("Room is shutting down"));
        }
    }

    @Override
    public Identifier getServerIdentifier() {
        return IDENTIFIER;
    }
}
