package io.ib67.sfcraft.mixin.server.subserver;

import com.mojang.authlib.GameProfile;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.module.SignatureService;
import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.room.CookieState;
import io.ib67.sfcraft.room.RequestedRoom;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.SocketAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;

import static io.ib67.sfcraft.room.CookieState.*;
import static io.ib67.sfcraft.room.RoomTeleporter.ROOM_COOKIE;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    abstract void startClientVerification(GameProfile profile);

    @Shadow
    private @Nullable GameProfile authenticatedProfile;
    @Shadow
    @Final
    private boolean transferred;
    @Shadow
    @Final
    private Connection connection;

    @Shadow
    public abstract void disconnect(Component reason);

    @Shadow
    @Final
    private static Logger LOGGER;
    @Unique
    private CookieState sf$cookieState = CookieState.NOT_SENT;
    @Unique
    private RequestedRoom sf$room = null;
    @Unique
    private boolean clean;

    @Inject(method = "tick", at = @At("TAIL"))
    void restoreVerify(CallbackInfo ci) {
        if ((sf$cookieState == SENT && sf$room != null) || (clean && sf$cookieState != DONE)) {
            sf$cookieState = CookieState.RECV;
            startClientVerification(this.authenticatedProfile);
        }
    }

    @Inject(method = "handleCookieResponse", at = @At("HEAD"), cancellable = true)
    private void sf$onRoomId(ServerboundCookieResponsePacket packet, CallbackInfo ci) {
        if (packet.key().equals(ROOM_COOKIE)) {
            if (sf$cookieState != SENT) throw new IllegalStateException("Protocol error");
            var roomSvc = SFCraft.getInjector().getInstance(RoomModule.class);
            var signSvc = SFCraft.getInjector().getInstance(SignatureService.class);
            try {
                if (packet.payload().length == 0) {
                    // clean reconnect.
                    LOGGER.info("Player " + authenticatedProfile.getName() + " requested a clean reconnection");
                    clean = true;
                    ci.cancel();
                    return;
                }
                var sign = signSvc.readSignature(Unpooled.wrappedBuffer(packet.payload()));
                this.sf$room = RequestedRoom.PACKET_CODEC.decode(Unpooled.wrappedBuffer(sign.data()));
            } catch (Exception t) {
                LOGGER.error("Failed to read cookie: {0}", t);
                this.disconnect(Component.nullToEmpty("Protocol error."));
                ci.cancel();
                return;
            }
        }
        ci.cancel();
    }

    @Inject(method = "startClientVerification", at = @At("HEAD"), cancellable = true)
    void sf$makeTransferPlayerProfile(GameProfile profile, CallbackInfo ci) {
        if (transferred && !clean) {
            switch (this.sf$cookieState) {
                case NOT_SENT -> {
                    this.authenticatedProfile = profile;
                    this.connection.send(new ClientboundCookieRequestPacket(ROOM_COOKIE));
                    this.sf$cookieState = SENT;
                }
                case RECV -> {
                    var ssr = SFCraft.getInjector().getInstance(RoomRegistry.class);
                    var room = ssr.getRoomBy(sf$room.identifier());
                    if (room != null) {
                        this.authenticatedProfile = new GameProfile(sf$room.profileUuid(), sf$room.profileName());
                        var session = room.getPlayerManager().getSessionBy(sf$room.profileUuid());
                        if (session == null) {
                            disconnect(Component.nullToEmpty("Session isn't exists."));
                            ci.cancel();
                            return;
                        }
                        session.onPlayerLogin(sf$room.profileUuid());
                        this.sf$cookieState = DONE;
                        this.startClientVerification(this.authenticatedProfile);
                        ci.cancel();
                        return;
                    }
                }
            }
            if (this.sf$cookieState != CookieState.DONE) {
                ci.cancel();
            }
        }
        if (clean) {
            this.sf$cookieState = DONE;
        }
    }

    @Redirect(method = "verifyLoginAndFinishConnectionSetup", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"))
    private Component sf$bypassRoomPlayer(PlayerList instance, SocketAddress address, GameProfile profile) {
        if (sf$cookieState == DONE) return null;
        return instance.canPlayerLogin(address, profile);
    }
}
