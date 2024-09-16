package io.ib67.sfcraft.mixin.server.optimize;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.BeeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "tickCramming", at=@At("HEAD"), cancellable = true)
    private void sf$disableBeeCramming(CallbackInfo ci) {
        if($this() instanceof BeeEntity) {
            ci.cancel();
        }
    }

    @Unique
    private LivingEntity $this(){
        return (LivingEntity) (Object) this;
    }
}
