package io.ib67.sfcraft.mixin.common;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 仅承载 shadow 供子 mixin(如 fixes.EndermanEntityMixin)复用;
 * gameplay 相关的注入见 io.ib67.sfcraft.mixin.gameplay。
 */
@Mixin(Mob.class)
public abstract class MobEntityMixin {
    @Shadow
    public boolean requiresCustomPersistence() {
        throw new AssertionError();
    }
}
