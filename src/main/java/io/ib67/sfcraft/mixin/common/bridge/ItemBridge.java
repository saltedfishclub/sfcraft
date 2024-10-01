package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.item.Item;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Item.class)
public interface ItemBridge {
    @Accessor
    void setRegistryEntry(RegistryEntry.Reference<Item> value);
}
