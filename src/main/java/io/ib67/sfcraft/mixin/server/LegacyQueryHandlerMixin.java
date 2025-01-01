package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.util.Helper;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.QueryableServer;
import net.minecraft.network.handler.LegacyQueryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Locale;

@Mixin(LegacyQueryHandler.class)
public class LegacyQueryHandlerMixin {
    @Unique
    SocketAddress address;

    @Inject(method = "channelRead", at = @At("HEAD"))
    public void onChannelRead(ChannelHandlerContext ctx, Object msg, CallbackInfo ci) {
        address = ctx.channel().remoteAddress();
    }

    @Redirect(method = "channelRead", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/handler/LegacyQueryHandler;getResponse(Lnet/minecraft/network/QueryableServer;)Ljava/lang/String;"))
    private String getResponse(QueryableServer server) {
        var version = server.getVersion();
        return String.format(
                Locale.ROOT,
                "§1\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
                127,
                version,
                server.getServerMotd(),
                server.getCurrentPlayerCount(),
                server.getMaxPlayerCount()
        );
    }
}
