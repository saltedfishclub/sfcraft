package io.ib67.sfcraft.mixin.server.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapDecoder;
import io.ib67.sfcraft.SFItemRegistry;
import io.ib67.sfcraft.item.SFItem;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ComponentHolder {
    @Shadow
    @Final
    @Deprecated
    @Nullable
    private Item item;

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void beforeGetItem(CallbackInfoReturnable<Item> cir) {
        var id = get(DataComponentTypes.CUSTOM_DATA);
        if (id != null && id.contains("sf_type")) {
            cir.setReturnValue(SFItemRegistry.ITEMS.get(Identifier.tryParse(id.copyNbt().getString("sf_type"))));
        }
    }

    @Redirect(method = "getRegistryEntry", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    private Item sf$getMappedEntry(ItemStack instance) {
        if (this.item instanceof SFItem sfi) {
            return sfi.getMappedItem();
        }
        return item;
    }
}
