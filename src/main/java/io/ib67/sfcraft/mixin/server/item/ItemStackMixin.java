package io.ib67.sfcraft.mixin.server.item;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.SFItems;
import io.ib67.sfcraft.SFRegistries;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.ComponentMapImpl;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ComponentHolder {
    @Shadow
    @Final
    @Deprecated
    @Nullable
    private Item item;

    @Shadow @Final private static Logger LOGGER;
    @Unique
    private Item sfItem;

    @Shadow public abstract boolean isEmpty();

    @Inject(method = "<init>(Lnet/minecraft/item/ItemConvertible;ILnet/minecraft/component/ComponentMapImpl;)V", at = @At("TAIL"))
    private void initSFItem(ItemConvertible item, int count, ComponentMapImpl components, CallbackInfo ci) {
        var _id = get(DataComponentTypes.CUSTOM_DATA);
        if (_id != null && _id.contains(SFItems.SF_ITEM_TYPE_KEY)) {
            var id = _id.copyNbt().getString(SFItems.SF_ITEM_TYPE_KEY);
            try {
                sfItem = SFRegistries.ITEMS.get(Identifier.of(SFCraft.MOD_ID, id));
            }catch(Exception e){
                e.printStackTrace();
            }
            if(sfItem == null){
                LOGGER.warn("A invalid sfItem is being created: {}", id);
                new IllegalStateException("Just stacktrace").printStackTrace();
            }
        }
    }

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void beforeGetItem(CallbackInfoReturnable<Item> cir) {
        if (sfItem != null) cir.setReturnValue(sfItem);
    }

    @Redirect(method = "getRegistryEntry", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    private Item sf$getMappedEntry(ItemStack instance) {
        return isEmpty() ? Items.AIR : this.item;
    }

    @Redirect(method = "copy", at= @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    private Item sf$getCopySubject(ItemStack instance){
        return isEmpty() ? Items.AIR : this.item;
    }
}
