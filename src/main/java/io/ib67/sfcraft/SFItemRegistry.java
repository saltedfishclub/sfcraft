package io.ib67.sfcraft;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.function.UnaryOperator;

public class SFItemRegistry {
    public static final ComponentType<String> SF_ITEM_TYPE = register("sf_item_type",
            builder -> builder.codec(Codec.STRING).packetCodec(PacketCodecs.STRING));
    public static final Registry<Item> ITEMS = FabricRegistryBuilder
            .<Item>createSimple(RegistryKey.ofRegistry(Identifier.of(SFCraft.MOD_ID, "item")))
            .attribute(RegistryAttribute.MODDED)
            .buildAndRegister();
    public static void init() {
        SFItems.init();
    }
    private static <T> ComponentType<T> register(String id, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, id, builderOperator.apply(ComponentType.builder()).build());
    }


}
