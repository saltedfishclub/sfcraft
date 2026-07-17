package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.util.Helper;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Locale;
import net.minecraft.server.ServerInfo;
import net.minecraft.server.network.LegacyQueryHandler;

@Mixin(LegacyQueryHandler.class)
public class LegacyQueryHandlerMixin {
    @Unique
    SocketAddress address;

    @Inject(method = "channelRead", at = @At("HEAD"))
    public void onChannelRead(ChannelHandlerContext ctx, Object msg, CallbackInfo ci) {
        address = ctx.channel().remoteAddress();
    }

    @Redirect(method = "channelRead", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/LegacyQueryHandler;createVersion1Response(Lnet/minecraft/server/ServerInfo;)Ljava/lang/String;"))
    private String getResponse(ServerInfo server) {
        var version = server.getServerVersion();
        return String.format(
                Locale.ROOT,
                "§1\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
                127,
                version,
                server.getMotd(),
                server.getPlayerCount(),
                server.getMaxPlayers()
        );
    }
}
