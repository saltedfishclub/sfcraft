package io.ib67.sfcraft.mixin.common.fixes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "pushEntities", at=@At("HEAD"), cancellable = true)
    private void sf$disableBeeCramming(CallbackInfo ci) {
        if($this() instanceof Bee) {
            ci.cancel();
        }
    }

    @Unique
    private LivingEntity $this(){
        return (LivingEntity) (Object) this;
    }
}
