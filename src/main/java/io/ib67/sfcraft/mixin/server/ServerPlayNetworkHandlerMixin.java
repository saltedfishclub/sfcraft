package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.mixin.common.bridge.ServerPlayerBridge;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayer player;

    /**
     * @author icybear
     * @reason small method, overwrite to reduce overhead
     */
    @Redirect(method = "lambda$handleChat$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getChatDecorator()Lnet/minecraft/network/chat/ChatDecorator;"))
    public ChatDecorator getMessageDecorator(MinecraftServer instance) {
        return SFCraft.getInjector().getInstance(SimpleMessageDecorator.class);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;disconnect(Lnet/minecraft/network/chat/Component;)V"))
    public void onPlayerIdling(ServerGamePacketListenerImpl instance, Component text) {
        ((ServerPlayerBridge) instance.player).setLastActionTime(Long.MAX_VALUE);
        SFCallbacks.PLAYER_IDLE.invoker().onSwitchIdle(instance.player, true);
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    public void onDisconnected(DisconnectionDetails instance, CallbackInfo ci) {
        player.resetLastActionTime();
    }
}
