package io.ib67.sfcraft.mixin.gameplay;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.game.commander.CommanderModule;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 新生成的敌怪有 config 概率(默认 2%)携带「统帅」效果,概率与行为均由 {@link CommanderModule} 驱动。
 */
@Mixin(Mob.class)
public abstract class MobCommanderMixin {
    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void sfcraft$maybeCommander(ServerLevelAccessor world, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData data,
                                        CallbackInfoReturnable<SpawnGroupData> cir) {
        SFCraft.getInjector().getInstance(CommanderModule.class)
                .onMobSpawned((Mob) (Object) this, reason);
    }
}
