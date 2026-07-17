package io.ib67.sfcraft.mixin.common;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Mob.class)
public abstract class MobEntityMixin {
    @Shadow
    public boolean requiresCustomPersistence() {
        throw new AssertionError();
    }
}
