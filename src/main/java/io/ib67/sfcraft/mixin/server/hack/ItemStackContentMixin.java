package io.ib67.sfcraft.mixin.server.hack;

import io.ib67.sfcraft.SFItemType;
import io.ib67.sfcraft.item.SFItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.HoverEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HoverEvent.ItemStackContent.class)
public class ItemStackContentMixin {
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getRegistryEntry()Lnet/minecraft/registry/entry/RegistryEntry;"), method = "<init>(Lnet/minecraft/item/ItemStack;)V")
    private static RegistryEntry<Item> sf$hijack(ItemStack instance) {
        if (instance.getItem() instanceof SFItem sfi) {
            return sfi.getMappedItem().getRegistryEntry();
        }
        return instance.getRegistryEntry();
    }
}
