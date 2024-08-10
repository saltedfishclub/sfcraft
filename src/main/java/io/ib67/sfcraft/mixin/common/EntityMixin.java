package io.ib67.sfcraft.mixin.common;

import io.ib67.sfcraft.callback.SFCallbacks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "setSneaking", at = @At("HEAD"))
    public void onSneaking(boolean sneaking, CallbackInfo ci) {
        if ($this() instanceof PlayerEntity pe) {
            SFCallbacks.PLAYER_SNEAKING.invoker().onSneaking(pe, sneaking);
        }
    }

    @Unique
    private Entity $this() {
        return (Entity) (Object) this;
    }
}
