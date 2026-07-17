package io.ib67.sfcraft.mixin.common;

import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.mixin.common.bridge.EntityBridge;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {
    @Unique
    private long flyTime;

    @Inject(method = "aiStep", at = @At("TAIL"))
    public void tickMovement(CallbackInfo ci) {
        var eb = ((EntityBridge) $this());
        if (eb.sfcraft$getFlag(EntityBridge.sfcraft$getFlyingFlagIndex())) {
            SFCallbacks.PLAYER_FLYING.invoker().onFlyingTick($this(), flyTime++, true);
        } else {
            if (flyTime != 0) {
                SFCallbacks.PLAYER_FLYING.invoker().onFlyingTick($this(), flyTime, false);
            }
            flyTime = 0;
        }
    }

    @Unique
    public Player $this() {
        return (Player) (Object) this;
    }

}
