package io.ib67.sfcraft.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;

@Mixin(SleepStatus.class)
public abstract class SleepManagerMixin {
    @Inject(method = "areEnoughDeepSleeping", at = @At("HEAD"), cancellable = true)
    public void canResetTime(int percentage, List<ServerPlayer> players, CallbackInfoReturnable<Boolean> cir) {
        if (players.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        var world = players.getFirst().level();
        if (world.getDayTime() % 24000 >= 12544) {
            if(!world.isRaining()){
                cir.setReturnValue(false);
            }
        }
    }
}
