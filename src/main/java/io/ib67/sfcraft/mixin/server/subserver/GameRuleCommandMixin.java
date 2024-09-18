package io.ib67.sfcraft.mixin.server.subserver;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.GameRuleCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRuleCommand.class)
public abstract class GameRuleCommandMixin {
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGameRules()Lnet/minecraft/world/GameRules;"), method = "executeSet")
    private static GameRules sf$redirectSetForPerWorld(MinecraftServer instance, CommandContext<ServerCommandSource> context) {
        return getGameRule(instance, context.getSource());
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGameRules()Lnet/minecraft/world/GameRules;"), method = "executeQuery")
    private static GameRules sf$redirectSetForPerWorld(MinecraftServer instance, ServerCommandSource context) {
        return getGameRule(instance, context);
    }

    private static GameRules getGameRule(MinecraftServer instance, ServerCommandSource source) {
        var world = source.getWorld();
        if (world == null) return instance.getGameRules();
        return world.getGameRules(); // this is same as Server#getGameRule since one GameRules is shared across multiple worlds in vanilla.
    }
}
