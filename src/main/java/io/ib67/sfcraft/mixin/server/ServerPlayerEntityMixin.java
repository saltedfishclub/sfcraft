package io.ib67.sfcraft.mixin.server;

import com.mojang.datafixers.util.Either;
import io.ib67.sfcraft.callback.SFCallbacks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin {
    @Shadow
    private long lastActionTime;

    @Inject(method = "die", at = @At("TAIL"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        SFCallbacks.PLAYER_DEATH.invoker().onPlayerDeath((ServerPlayer) (Object) this, damageSource);
    }

    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    public void onSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        var result = SFCallbacks.PLAYER_SLEEP.invoker().onPlayerSleep((ServerPlayer) (Object) this, pos);
        if (result.left().isPresent()) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "resetLastActionTime", at = @At("HEAD"))
    public void leaveAFK(CallbackInfo ci) {
        if (lastActionTime == Long.MAX_VALUE) {
            SFCallbacks.PLAYER_IDLE.invoker().onSwitchIdle((ServerPlayer) (Object) this, false);
        }
    }
}
