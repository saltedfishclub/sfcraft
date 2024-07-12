package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.registry.chat.SimpleMessageDecorator;
import net.minecraft.network.message.MessageDecorator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
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
}
