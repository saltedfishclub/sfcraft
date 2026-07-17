package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.callback.SFCallbacks;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    public abstract void disconnect(Component reason);

    @Shadow
    @Final
    MinecraftServer server;
    @Unique
    private String currentPlayer;

    @Inject(method = "handleHello", at = @At("HEAD"))
    public void onHello(ServerboundHelloPacket packet, CallbackInfo ci) {
        currentPlayer = packet.name();
    }

    @Redirect(method = "handleHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;isMemoryConnection()Z"))
    public boolean isLocal(Connection connection) {
        if (!(connection.getRemoteAddress() instanceof InetSocketAddress)) {
            return connection.isMemoryConnection(); // 碰上真 isLocal 了
        }
        var offlineProfile = NameAndId.createOffline(currentPlayer);
        boolean _r;
        var nameCache = server.services().nameToIdCache();
        if (nameCache != null) {
            _r = nameCache.get(offlineProfile.id()).isPresent();
        }else{
            var wl = server.getPlayerList().getWhiteList();
            _r = wl.isWhiteListed(offlineProfile);
        }
        SFCallbacks.PRE_LOGIN.invoker().onPlayerPreLogin(currentPlayer, connection, this::disconnect, _r);
        return _r;
    }


}
