package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.callback.SFCallbacks;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerHandshakeNetworkHandler.class)
public abstract class ServerHandshakeNetworkHandlerMixin {
    @Shadow
    @Final
    private ClientConnection connection;

    @Redirect(method = "onHandshake", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getServerMetadata()Lnet/minecraft/server/ServerMetadata;"))
    public ServerMetadata getMetadata(MinecraftServer instance) {
        var result = SFCallbacks.MOTD.invoker().onMotd(instance,connection);
        if(result == null) {
            return instance.getServerMetadata();
        }else{
            return result;
        }
    }
}
