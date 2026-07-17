package io.ib67.sfcraft.mixin.common.fixes;

import io.ib67.sfcraft.mixin.common.MobEntityMixin;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class EndermanEntityMixin extends MobEntityMixin {
    @Inject(at = @At("HEAD"), method = "requiresCustomPersistence", cancellable = true)
    private void sf$cannotDespawn(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(super.requiresCustomPersistence());
    }

}
