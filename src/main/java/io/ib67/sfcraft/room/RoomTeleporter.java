package io.ib67.sfcraft.room;

import com.google.inject.Inject;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.subserver.Room;
import io.ib67.sfcraft.util.Helper;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.packet.s2c.common.ServerTransferS2CPacket;
import net.minecraft.network.packet.s2c.common.StoreCookieS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;

public class RoomTeleporter {
    @Inject
    private RoomRegistry registry;
    @Inject
    private RoomModule roomModule;
    @Inject
    private MinecraftServerSupplier serverSupplier;
    @Inject
    private SFConfig config;

    public static final Identifier ROOM_COOKIE = Identifier.of(SFCraft.MOD_ID, "room");

    public void teleportTo(Room room, ServerPlayerEntity player) {
        if (roomModule.isVirtual(player.getUuid())) {
            player.sendMessage(Text.literal("您已经在某个房间里了").withColor(Colors.LIGHT_RED));
            return;
        }
        var finalUuid = roomModule.generateIdForRoom(player.getGameProfile(), player.getName().getLiteralString(), room.getServerIdentifier());
        var sess = room.getPlayerManager().createSessionFor(finalUuid);
        if (player.getServer().getWorld(sess.getSpawnPosition().dimension()) == null) {
            throw new IllegalStateException("world isn't exist");
        }
        var networkHandler = player.networkHandler;
        networkHandler.reconfigure();
        networkHandler.send(new StoreCookieS2CPacket(ROOM_COOKIE, roomModule.serializeRequestedRoom(new RequestedRoom(
                room.getServerIdentifier(),
                player.getName().getLiteralString(),
                finalUuid
        ))), PacketCallbacks.always(() -> {
            networkHandler.sendPacket(new ServerTransferS2CPacket(config.domain, serverSupplier.get().getServerPort()));
        }));
    }
}
