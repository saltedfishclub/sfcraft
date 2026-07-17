package io.ib67.sfcraft.mixin.common.bridge;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Item.class)
public interface ItemBridge {
    @Accessor
    void setBuiltInRegistryHolder(Holder.Reference<Item> value);
}
