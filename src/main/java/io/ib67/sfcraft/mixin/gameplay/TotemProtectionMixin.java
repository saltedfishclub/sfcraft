package io.ib67.sfcraft.mixin.gameplay;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.game.totem.TotemModule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让「屹立不倒」图腾接管原版死亡保护判定。原版死亡保护是数据组件驱动的
 * ({@code DataComponents.DEATH_PROTECTION}),持有本图腾的玩家在这里被完全接管——
 * 从而阻止原版把它当普通图腾消耗掉,并插入冷却/经验判定。逻辑见 {@link TotemModule#tryProtect}。
 */
@Mixin(LivingEntity.class)
public abstract class TotemProtectionMixin {
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void sfcraft$standingFirm(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        switch (SFCraft.getInjector().getInstance(TotemModule.class).tryProtect(player, source)) {
            case PROTECTED -> cir.setReturnValue(true);
            case DECLINED -> cir.setReturnValue(false);
            case NOT_APPLICABLE -> {
                // 玩家没拿本图腾:交回原版逻辑(普通图腾/其它死亡保护物品照常)
            }
        }
    }
}
