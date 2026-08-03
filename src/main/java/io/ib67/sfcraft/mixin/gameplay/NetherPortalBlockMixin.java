package io.ib67.sfcraft.mixin.gameplay;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.game.portal.ElytraPortalModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 鞘翅冲出下界传送门时跳过等待:本方法每 tick 被 {@code PortalProcessor} 调用来决定
 * 「还要在门里待多久」,命中条件时覆写为 1 tick(与原版创造模式一致),下一 tick 即跨维度;
 * 否则不动 cir,等待时间完全由原版游戏规则决定。判定逻辑见 {@link ElytraPortalModule#shouldTeleportNow}。
 */
@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalBlockMixin {
    @Inject(method = "getPortalTransitionTime", at = @At("HEAD"), cancellable = true)
    private void sfcraft$elytraRushPortal(ServerLevel level, Entity entity, CallbackInfoReturnable<Integer> cir) {
        var module = SFCraft.getInjector().getInstance(ElytraPortalModule.class);
        if (module.shouldTeleportNow(entity)) {
            cir.setReturnValue(module.getInstantTransitionTicks());
        }
    }
}
