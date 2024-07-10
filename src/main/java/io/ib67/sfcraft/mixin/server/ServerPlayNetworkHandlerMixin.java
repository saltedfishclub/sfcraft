package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.SFCraft;
import net.minecraft.network.message.MessageChainTaskQueue;
import net.minecraft.network.message.MessageDecorator;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    /**
     * @author icybear
     * @reason small method, provide custom one
     */
    @Redirect(method = "method_44900", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getMessageDecorator()Lnet/minecraft/network/message/MessageDecorator;"))
    public MessageDecorator getMessageDecorator(MinecraftServer instance) {
        return SFCraft.getInstance().getMessageDecorator();
    }
}
