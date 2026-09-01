package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.network.HandshakeAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakePacketListenerImpl.class)
public abstract class ServerHandshakeNetworkHandlerMixin {
    @Shadow
    @Final
    private Connection connection;

    /**
     * 记下玩家实际用来进服的地址,房间 transfer 时把他送回同一个入口。
     */
    @Inject(method = "handleIntention", at = @At("HEAD"))
    private void sf$captureAddress(ClientIntentionPacket packet, CallbackInfo ci) {
        ((HandshakeAddress.Holder) connection)
                .sfcraft$setHandshakeAddress(HandshakeAddress.sanitize(packet.hostName(), packet.port()));
    }

    @Redirect(method = "handleIntention", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getStatus()Lnet/minecraft/network/protocol/status/ServerStatus;"))
    public ServerStatus getMetadata(MinecraftServer instance) {
        var result = SFCallbacks.MOTD.invoker().onMotd(instance,connection);
        if(result == null) {
            return instance.getStatus();
        }else{
            return result;
        }
    }
}
