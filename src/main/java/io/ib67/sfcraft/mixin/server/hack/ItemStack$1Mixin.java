package io.ib67.sfcraft.mixin.server.hack;

import com.llamalad7.mixinextras.sugar.Local;
import io.ib67.sfcraft.SFItemType;
import io.ib67.sfcraft.item.SFItem;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Debug(export = true)
@Mixin(targets = "net.minecraft.item.ItemStack$1")
public class ItemStack$1Mixin {
    @Shadow
    @Final
    private static PacketCodec<RegistryByteBuf, RegistryEntry<Item>> ITEM_PACKET_CODEC;
    @Unique
    private PacketCodec<RegistryByteBuf, RegistryEntry<Item>> codec;

    @Inject(at = @At("TAIL"), method = "<init>")
    private void sf$initCodec(CallbackInfo ci) {
        var originalCodec = ITEM_PACKET_CODEC;
        codec = new PacketCodec<>() {

            @Override
            public void encode(RegistryByteBuf buf, RegistryEntry<Item> value) {
                if (value.getKey().isEmpty()) {
                    originalCodec.encode(buf, value);
                    return;
                }

                var mapped = SFItemType.INSTANCE.mapId(value.getIdAsString());
                if (mapped != null) {
                    originalCodec.encode(buf, mapped.getRegistryEntry());
                } else {
                    originalCodec.encode(buf, value);
                }

            }

            @Override
            public RegistryEntry<Item> decode(RegistryByteBuf buf) {
                return originalCodec.decode(buf);
            }
        };
    }

    @Redirect(method = "encode(Lnet/minecraft/network/RegistryByteBuf;Lnet/minecraft/item/ItemStack;)V", at = @At(value = "FIELD", target = "Lnet/minecraft/item/ItemStack$1;ITEM_PACKET_CODEC:Lnet/minecraft/network/codec/PacketCodec;"))
    private PacketCodec<RegistryByteBuf, RegistryEntry<Item>> sf$hijackCodec() {
        return codec;
    }

    @Inject(method = "decode(Lnet/minecraft/network/RegistryByteBuf;)Lnet/minecraft/item/ItemStack;",
            cancellable = true,
            at = @At(value = "NEW", target = "(Lnet/minecraft/registry/entry/RegistryEntry;ILnet/minecraft/component/ComponentChanges;)Lnet/minecraft/item/ItemStack;",
                    shift = At.Shift.BEFORE))
    public void sf$mapItem(RegistryByteBuf registryByteBuf, CallbackInfoReturnable<ItemStack> cir,
                           @Local int i, @Local RegistryEntry<Item> itemType, @Local ComponentChanges component) {
        var _itemType = component.get(SFItemType.SF_TYPE); //todo optimize
        if (_itemType != null && !_itemType.isEmpty()) { // whats wrong with you mojang
            var a = SFItemType.INSTANCE.mapId(_itemType.get());
            System.out.println(a);
            cir.setReturnValue(new ItemStack(a.getRegistryEntry(), i,component));
        }
    }

    @Inject(
            method = "encode(Lnet/minecraft/network/RegistryByteBuf;Lnet/minecraft/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/codec/PacketCodec;encode(Ljava/lang/Object;Ljava/lang/Object;)V",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void sf$insertItemType(
            RegistryByteBuf registryByteBuf,
            ItemStack itemStack, CallbackInfo ci) {
        if (itemStack.getItem() instanceof SFItem sfItem) {
            var value = sfItem.getItem().getRegistryEntry().getKey().get().getValue().toString();
            itemStack.apply(SFItemType.SF_TYPE, value, it -> value);
        }
    }
}
