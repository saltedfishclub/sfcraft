package io.ib67.sfcraft.mixin.common;

import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
    @Shadow
    public boolean cannotDespawn() {
        throw new AssertionError();
    }
}
