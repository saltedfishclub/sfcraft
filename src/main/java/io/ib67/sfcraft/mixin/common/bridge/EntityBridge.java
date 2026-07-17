package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityBridge {
    @Accessor("FLAG_FALL_FLYING")
    static int sfcraft$getFlyingFlagIndex() {
        throw new AssertionError();
    }

    @Invoker("getSharedFlag")
    boolean sfcraft$getFlag(int index);
}
