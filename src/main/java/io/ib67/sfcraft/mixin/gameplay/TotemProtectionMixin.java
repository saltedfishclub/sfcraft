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
 * 让「屹立不倒」图腾在死亡保护判定里插一脚。仅当本图腾**成功触发**时才接管并返回 true;
 * 冷却/经验不足(DECLINED)或玩家没拿本图腾(NOT_APPLICABLE)一律**放行原版**——本图腾不挂
 * DEATH_PROTECTION 组件,原版不认它,于是既不会误消耗本图腾,也不会连带取消掉玩家另一只手里
 * 的原版不死图腾。逻辑见 {@link TotemModule#tryProtect}。
 */
@Mixin(LivingEntity.class)
public abstract class TotemProtectionMixin {
    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void sfcraft$standingFirm(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        // 只有触发成功才拦截返回 true;其余情形不动 cir,让原版逻辑继续(原版图腾等照常兜底)
        if (SFCraft.getInjector().getInstance(TotemModule.class).tryProtect(player, source) == TotemModule.Result.PROTECTED) {
            cir.setReturnValue(true);
        }
    }
}
