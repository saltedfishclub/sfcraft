package io.ib67.sfcraft.mixin.server.hack;

import io.ib67.sfcraft.SFItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
                var mapped = SFItem.INSTANCE.mapId(value.getIdAsString());
                if (mapped != null) {
                    originalCodec.encode(buf, mapped);
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
}
