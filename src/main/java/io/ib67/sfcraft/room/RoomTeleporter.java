package io.ib67.sfcraft.room;

import com.google.inject.Inject;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.inject.MinecraftServerSupplier;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.module.SignatureService;
import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.subserver.Room;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.CommonColors;

public class RoomTeleporter {
    @Inject
    private RoomRegistry registry;
    @Inject
    private RoomModule roomModule;
    @Inject
    private SignatureService signatureModule;
    @Inject
    private MinecraftServerSupplier serverSupplier;
    @Inject
    private SFConfig config;

    public static final Identifier ROOM_COOKIE = Identifier.fromNamespaceAndPath(SFCraft.MOD_ID, "room");

    public void teleportTo(Room room, ServerPlayer player) {
        if (roomModule.isVirtual(player.getUUID())) {
            player.sendSystemMessage(Component.literal("您已经在某个房间里了").withColor(CommonColors.SOFT_RED));
            return;
        }
        var finalUuid = roomModule.generateIdForRoom(player.getGameProfile(), player.getName().tryCollapseToString(), room.getServerIdentifier());
        var sess = room.getPlayerManager().createSessionFor(finalUuid);
        var name = player.getName().tryCollapseToString();

        if (player.level().getServer().getLevel(sess.getSpawnPosition().dimension()) == null) {
            throw new IllegalStateException("world isn't exist");
        }
        var networkHandler = player.connection;
        networkHandler.switchToConfig();
        var request = new RequestedRoom(room.getServerIdentifier(), name, finalUuid);
        var requestBuf = Unpooled.buffer();
        RequestedRoom.PACKET_CODEC.encode(requestBuf, request);
        var cookie = signatureModule.createSignature(
                new SignatureService.Signature(
                        RoomModule.SIGN_TOPIC,
                        name,
                        0,
                        System.currentTimeMillis() + 60000,
                        requestBuf.array()
                )
        );
        networkHandler.send(new ClientboundStoreCookiePacket(ROOM_COOKIE, cookie), PacketSendListener.thenRun(() -> {
            networkHandler.send(new ClientboundTransferPacket(config.domain, config.port));
        }));
    }
}
