package io.ib67.sfcraft.mixin.server;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.SleepManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SleepManager.class)
public abstract class SleepManagerMixin {
    @Inject(method = "canResetTime", at = @At("HEAD"), cancellable = true)
    public void canResetTime(int percentage, List<ServerPlayerEntity> players, CallbackInfoReturnable<Boolean> cir) {
        if (players.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        var world = players.getFirst().getServerWorld();
        if (world.getTimeOfDay() % 24000 >= 12544) {
            if(!world.isRaining()){
                cir.setReturnValue(false);
            }
        }
    }
}
