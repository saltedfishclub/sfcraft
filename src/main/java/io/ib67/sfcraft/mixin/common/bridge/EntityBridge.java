package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityBridge {
    @Accessor("FALL_FLYING_FLAG_INDEX")
    static int sfcraft$getFlyingFlagIndex() {
        throw new AssertionError();
    }

    @Invoker("getFlag")
    boolean sfcraft$getFlag(int index);
}
