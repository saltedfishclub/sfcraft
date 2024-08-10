package io.ib67.sfcraft.mixin.server.fixes;

import io.ib67.sfcraft.mixin.common.MobEntityMixin;
import net.minecraft.entity.mob.EndermanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin extends MobEntityMixin {
    @Inject(at = @At("HEAD"), method = "cannotDespawn", cancellable = true)
    private void sf$cannotDespawn(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(super.cannotDespawn());
    }

}
