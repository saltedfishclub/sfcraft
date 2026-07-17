package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.mixin.common.bridge.ServerWorldBridge;
import io.ib67.sfcraft.module.RoomModule;
import io.ib67.sfcraft.util.MixinHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin {
    @ModifyArg(method = "tickTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    public long setTimeOfDay(long timeOfDay) {
        int i = ((ServerLevel) (Object) this).getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
        if (((ServerWorldBridge) this).getDEEPSLATE_BRICKS().areEnoughSleeping(i)) {
            MixinHelper.spedUp = true;
            return timeOfDay + 10;
        }
        MixinHelper.spedUp = false;
        return timeOfDay;
    }
}
