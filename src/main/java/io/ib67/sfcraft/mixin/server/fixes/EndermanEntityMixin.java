package io.ib67.sfcraft.mixin.server.fixes;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public class EndermanEntityMixin {
    @Mixin(targets = "net.minecraft.entity.mob.EndermanEntity$ChasePlayerGoal")
    public static class ChaseGoalMixin {
        @Shadow
        @Final
        private EndermanEntity enderman;

        @Inject(method = "canStart", at = @At("HEAD"), cancellable = true)
        public void fixCrossDimTeleport(CallbackInfoReturnable<Boolean> cir) {
            var target = enderman.getTarget();
            if (target!=null && enderman.getWorld() != target.getWorld()) {
                cir.setReturnValue(false);
            }
        }

    }
}
