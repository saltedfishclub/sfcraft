package io.ib67.sfcraft.mixin.server.subserver;

import com.mojang.authlib.GameProfile;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.module.SignatureService;
import io.ib67.sfcraft.registry.RoomRegistry;
import io.ib67.sfcraft.room.CookieState;
import io.ib67.sfcraft.room.RequestedRoom;
import io.netty.buffer.Unpooled;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.common.CookieResponseC2SPacket;
import net.minecraft.network.packet.s2c.common.CookieRequestS2CPacket;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
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

import static io.ib67.sfcraft.room.CookieState.*;
import static io.ib67.sfcraft.room.RoomTeleporter.ROOM_COOKIE;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    abstract void startVerify(GameProfile profile);

    @Shadow
    private @Nullable GameProfile profile;
    @Shadow
    @Final
    private boolean transferred;
    @Shadow
    @Final
    private ClientConnection connection;

    @Shadow
    public abstract void disconnect(Text reason);

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
            startVerify(this.profile);
        }
    }

    @Inject(method = "onCookieResponse", at = @At("HEAD"), cancellable = true)
    private void sf$onRoomId(CookieResponseC2SPacket packet, CallbackInfo ci) {
        if (packet.key().equals(ROOM_COOKIE)) {
            if (sf$cookieState != SENT) throw new IllegalStateException("Protocol error");
            var roomSvc = SFCraft.getInjector().getInstance(RoomModule.class);
            var signSvc = SFCraft.getInjector().getInstance(SignatureService.class);
            try {
                if (packet.payload().length == 0) {
                    // clean reconnect.
                    LOGGER.info("Player " + profile.getName() + " requested a clean reconnection");
                    clean = true;
                    ci.cancel();
                    return;
                }
                var sign = signSvc.readSignature(Unpooled.wrappedBuffer(packet.payload()));
                this.sf$room = RequestedRoom.PACKET_CODEC.decode(Unpooled.wrappedBuffer(sign.data()));
            } catch (Exception t) {
                LOGGER.error("Failed to read cookie: {0}", t);
                this.disconnect(Text.of("Protocol error."));
                ci.cancel();
                return;
            }
        }
        ci.cancel();
    }

    @Inject(method = "startVerify", at = @At("HEAD"), cancellable = true)
    void sf$makeTransferPlayerProfile(GameProfile profile, CallbackInfo ci) {
        if (transferred && !clean) {
            switch (this.sf$cookieState) {
                case NOT_SENT -> {
                    this.profile = profile;
                    this.connection.send(new CookieRequestS2CPacket(ROOM_COOKIE));
                    this.sf$cookieState = SENT;
                }
                case RECV -> {
                    var ssr = SFCraft.getInjector().getInstance(RoomRegistry.class);
                    var room = ssr.getRoomBy(sf$room.identifier());
                    if (room != null) {
                        this.profile = new GameProfile(sf$room.profileUuid(), sf$room.profileName());
                        var session = room.getPlayerManager().getSessionBy(sf$room.profileUuid());
                        if (session == null) {
                            disconnect(Text.of("Session isn't exists."));
                            ci.cancel();
                            return;
                        }
                        session.onPlayerLogin(sf$room.profileUuid());
                        this.sf$cookieState = DONE;
                        this.startVerify(this.profile);
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

    @Redirect(method = "tickVerify", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;"))
    private Text sf$bypassRoomPlayer(PlayerManager instance, SocketAddress address, GameProfile profile) {
        if (sf$cookieState == DONE) return null;
        return instance.checkCanJoin(address, profile);
    }
}
