package io.ib67.sfcraft.room.create;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.room.CreativeRoomModule;
import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.subserver.RoomPlayerManager;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class CreativeSpaceRoom implements Room {
    public static final ResourceKey<Level> WORLD = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, "playground"));
    public static final Identifier IDENTIFIER = Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, "creative_space");
    public static final BlockPos SPAWN_POS = new BlockPos(0, 66, 0);
    @Getter
    private final RoomPlayerManager playerManager;

    public CreativeSpaceRoom(CreativeRoomModule module) {
        this.playerManager = new CreativePlayerManager(this,module);
    }

    @Override
    public void shutdown() {
        for (ServerPlayer joinedPlayer : playerManager.getJoinedPlayers()) {
            joinedPlayer.connection.disconnect(Component.nullToEmpty("Room is shutting down"));
        }
    }

    @Override
    public Identifier getServerIdentifier() {
        return IDENTIFIER;
    }
}
