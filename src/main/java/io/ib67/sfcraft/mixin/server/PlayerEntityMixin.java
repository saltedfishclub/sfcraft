package io.ib67.sfcraft.mixin.server;

import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.mixin.server.bridge.EntityBridge;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    private static final int THRESHLD_OF_ELYTRA_FLY = 2;
    @Unique
    private long flyTime;

    @Inject(method = "tickMovement", at = @At("TAIL"))
    public void tickMovement(CallbackInfo ci) {
        var eb = ((EntityBridge) $this());
        if (eb.sfcraft$getFlag(EntityBridge.sfcraft$getFlyingFlagIndex())) {
            flyTime++;
            if (flyTime > 20 * THRESHLD_OF_ELYTRA_FLY) { // todo config
                SFCallbacks.PLAYER_FLYING.invoker().onFlyingTick($this(), flyTime - 20*THRESHLD_OF_ELYTRA_FLY, true);
            }
        } else {
            if (flyTime != 0) {
                SFCallbacks.PLAYER_FLYING.invoker().onFlyingTick($this(), flyTime, false);
            }
            flyTime = 0;
        }
    }

    @Unique
    public PlayerEntity $this() {
        return (PlayerEntity) (Object) this;
    }

}
