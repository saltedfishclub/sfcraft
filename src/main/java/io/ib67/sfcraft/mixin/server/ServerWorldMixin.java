package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.mixin.common.bridge.ServerWorldBridge;
import io.ib67.sfcraft.util.MixinHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

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
