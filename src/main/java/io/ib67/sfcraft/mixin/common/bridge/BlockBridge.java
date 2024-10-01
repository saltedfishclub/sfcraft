package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.block.Block;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Block.class)
public interface BlockBridge {
    @Accessor
    void setRegistryEntry(RegistryEntry.Reference<Block> value);
}
