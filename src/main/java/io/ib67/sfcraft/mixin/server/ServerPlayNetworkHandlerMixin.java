package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.mixin.server.bridge.ServerPlayerBridge;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import net.minecraft.entity.EntityType;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.message.MessageDecorator;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    /**
     * @author icybear
     * @reason small method, overwrite to reduce overhead
     */
    @Redirect(method = "method_44900", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getMessageDecorator()Lnet/minecraft/network/message/MessageDecorator;"))
    public MessageDecorator getMessageDecorator(MinecraftServer instance) {
        return SFCraft.getInjector().getInstance(SimpleMessageDecorator.class);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;disconnect(Lnet/minecraft/text/Text;)V", ordinal = 2))
    public void onPlayerIdling(ServerPlayNetworkHandler instance, Text text) {
        ((ServerPlayerBridge) instance.player).setLastActionTime(Long.MAX_VALUE);
        SFCallbacks.PLAYER_IDLE.invoker().onSwitchIdle(instance.player,true);
    }
}
