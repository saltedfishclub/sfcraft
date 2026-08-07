package io.ib67.sfcraft.mixin.gameplay;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.hint.FeatureHintModule;
import io.ib67.sfcraft.module.hint.Hint;
import io.ib67.sfcraft.module.game.totem.TotemModule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
    @Unique
    private TotemModule.Result sfcraft$lastTotemResult;

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void sfcraft$standingFirm(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        var result = SFCraft.getInjector().getInstance(TotemModule.class).tryProtect(player, source);
        sfcraft$lastTotemResult = result;
        if (result == TotemModule.Result.PROTECTED) {
            cir.setReturnValue(true);
            // 玩家已有代偿图腾并且真触发过——已经领到教训,不需要 hint
            SFCraft.getInjector().getInstance(FeatureHintModule.class).markUsed(player, Hint.STANDING_FIRM_TOTEM);
        }
        // DECLINED(拿了但 CD/经验不够):不动,让玩家按原版规律死掉,不发嘲讽
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"))
    private void sfcraft$vanillaTotemSaved(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer player)) return;
        // 原版图腾已当场救活(返回值 true)且玩家没拿代偿图腾——直接提示,无需延迟等待
        if (sfcraft$lastTotemResult != TotemModule.Result.NOT_APPLICABLE) return;
        if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
        FeatureHintModule featureHintModule = SFCraft.getInjector().getInstance(FeatureHintModule.class);
        featureHintModule.tryEmitHint(player, Hint.STANDING_FIRM_TOTEM);
    }
}
