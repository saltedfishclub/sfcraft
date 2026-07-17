package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.callback.SFCallbacks;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerHandshakePacketListenerImpl.class)
public abstract class ServerHandshakeNetworkHandlerMixin {
    @Shadow
    @Final
    private Connection connection;

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
