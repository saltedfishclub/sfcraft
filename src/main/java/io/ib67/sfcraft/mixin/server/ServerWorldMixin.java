package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.mixin.server.bridge.ServerWorldBridge;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin {
    @ModifyArg(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setTimeOfDay(J)V"))
    public long setTimeOfDay(long timeOfDay) {
        int i = ((ServerWorld) (Object) this).getGameRules().getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE);
        if (((ServerWorldBridge) this).getSleepManager().canSkipNight(i)) {
            return timeOfDay + 10;
        }
        return timeOfDay;
    }
}
