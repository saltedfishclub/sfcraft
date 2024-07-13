package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.callback.SFCallbacks;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    public abstract void disconnect(Text reason);

    @Shadow
    @Final
    MinecraftServer server;
    @Unique
    private String currentPlayer;

    @Inject(method = "onHello", at = @At("HEAD"))
    public void onHello(LoginHelloC2SPacket packet, CallbackInfo ci) {
        currentPlayer = packet.name();
    }

    @Redirect(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;isLocal()Z"))
    public boolean isLocal(ClientConnection connection) {
        if (!(connection.getAddress() instanceof InetSocketAddress)) {
            return connection.isLocal(); // 碰上真 isLocal 了
        }
        var offlineProfile = Uuids.getOfflinePlayerProfile(currentPlayer);
        boolean _r;
        if (server.getUserCache() != null) {
            _r = server.getUserCache().getByUuid(offlineProfile.getId()).isPresent();
        }else{
            var wl = server.getPlayerManager().getWhitelist();
            _r = wl.isAllowed(offlineProfile);
        }
        SFCallbacks.PRE_LOGIN.invoker().onPlayerPreLogin(currentPlayer, connection, this::disconnect, _r);
        return _r;
    }


}
