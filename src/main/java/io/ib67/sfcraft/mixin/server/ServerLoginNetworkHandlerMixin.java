package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.SFCraft;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin {
    @Shadow
    public abstract void disconnect(Text reason);

    private String currentPlayer;

    @Inject(method = "onHello", at = @At("HEAD"))
    public void onHello(LoginHelloC2SPacket packet, CallbackInfo ci) {
        currentPlayer = packet.name();
    }

    @Redirect(method = "onHello", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;isLocal()Z"))
    public boolean isLocal(ClientConnection connection) {
        return SFCraft.getListener().onPlayerLogin(currentPlayer, connection, this::disconnect);
    }


}
