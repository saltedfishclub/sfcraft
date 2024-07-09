package io.ib67.sfcraft.mixin.server;

import com.mojang.datafixers.util.Either;
import io.ib67.sfcraft.SFCraft;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "onDeath", at = @At("TAIL"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        SFCraft.getInstance().getListener().onPlayerDeath((ServerPlayerEntity) (Object) this, damageSource);
    }

    @Inject(method = "trySleep", at = @At("HEAD"), cancellable = true)
    public void onSleep(BlockPos pos, CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> cir) {
        if (cir.isCancellable())
            cir.setReturnValue(SFCraft.getInstance().getListener().onPlayerSleep((ServerPlayerEntity) (Object) this, pos));
    }
}
