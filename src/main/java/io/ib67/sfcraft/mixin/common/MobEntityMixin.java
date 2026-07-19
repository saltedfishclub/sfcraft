package io.ib67.sfcraft.mixin.common;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sfcraft.GameConfig;
import sfcraft.SFMobEffects;

@Mixin(Mob.class)
public abstract class MobEntityMixin {
    @Shadow
    public boolean requiresCustomPersistence() {
        throw new AssertionError();
    }

    // 新生成的敌怪有 config 概率(默认 2%)携带「统帅」效果,后续行为由 CommanderAuraManager 驱动
    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void sfcraft$maybeCommander(ServerLevelAccessor world, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data,
                                        CallbackInfoReturnable<SpawnGroupData> cir) {
        if (reason != EntitySpawnReason.NATURAL) return;
        var self = (Mob) (Object) this;
        if (!(self instanceof Enemy)) return;
        double chance = GameConfig.get().commander.spawnChance;
        if (chance <= 0) return;
        if (self.getRandom().nextDouble() >= chance) return;
        self.addEffect(new MobEffectInstance(SFMobEffects.COMMANDER,
                MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
    }
}
