package io.ib67.sfcraft.mixin.server.room;

import com.mojang.authlib.GameProfile;
import io.ib67.sfcraft.module.HintModule;
import io.ib67.sfcraft.util.Helper;
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

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    @Final
    private boolean transferred;

    @Shadow
    @Final
    private ClientConnection connection;

    @Shadow
    private @Nullable GameProfile profile;

    @Shadow
    abstract void startVerify(GameProfile profile);

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    public abstract void disconnect(Text reason);

    @Unique
    private String sf$roomId;

    @Unique
    private boolean sf$cookieVerified;

    @Inject(method = "tick", at = @At("TAIL"))
    private void sf$awaitRoom(CallbackInfo ci) {
        if (!sf$cookieVerified && transferred && sf$roomId != null) {
            startVerify(this.profile);
        }
    }

    @Inject(method = "onCookieResponse", at = @At("HEAD"), cancellable = true)
    private void sf$onRoomId(CookieResponseC2SPacket packet, CallbackInfo ci) {
        if (packet.key().equals(HintModule.IDENTIFIER_ROOM)) {
            var payload = packet.payload();
            sf$roomId = new String(payload);
        }
        ci.cancel();
    }

    @Inject(method = "startVerify", at = @At("HEAD"), cancellable = true)
    private void sf$queryCookies(GameProfile profile, CallbackInfo ci) {
        if (this.transferred) {
            if (sf$roomId == null) {
                if (this.profile == null) {
                    this.profile = profile;
                    // ask for cookies
                    this.connection.send(new CookieRequestS2CPacket(HintModule.IDENTIFIER_ROOM));
                    ci.cancel();
                } else {
                    // wrongly transferred. (cookie miss)
                    LOGGER.warn("Player {} is trying to transfer to us without a valid room cookie!", profile);
                    LOGGER.warn("Denying..");
                    this.disconnect(Text.literal("Illegal transfer request."));
                }
            } else if (!sf$cookieVerified) {
                // room id is now fetch!
                this.profile = Helper.generateForRoomPlayer(profile.getName());

                HintModule.rooms.put(this.profile, sf$roomId);
                sf$cookieVerified = true;
                startVerify(this.profile);
                ci.cancel();
            }
        }
    }

    @Redirect(method = "tickVerify", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;checkCanJoin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/text/Text;"))
    private Text sf$bypassRoomPlayer(PlayerManager instance, SocketAddress address, GameProfile profile) {
        if (sf$cookieVerified) return null;
        return instance.checkCanJoin(address, profile);
    }
}
