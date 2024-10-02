package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.world.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PalettedContainer.class)
public interface PalettedContainerBridge {
    @Accessor
    PalettedContainer.PaletteProvider getPaletteProvider();
}
