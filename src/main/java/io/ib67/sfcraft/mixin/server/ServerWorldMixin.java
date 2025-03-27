package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.mixin.common.bridge.ServerWorldBridge;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.util.MixinHelper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @ModifyArg(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setTimeOfDay(J)V"))
    public long setTimeOfDay(long timeOfDay) {
        int i = ((ServerWorld) (Object) this).getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        if (((ServerWorldBridge) this).getSleepManager().canSkipNight(i)) {
            MixinHelper.spedUp = true;
            return timeOfDay + 10;
        }
        MixinHelper.spedUp = false;
        return timeOfDay;
    }
}
