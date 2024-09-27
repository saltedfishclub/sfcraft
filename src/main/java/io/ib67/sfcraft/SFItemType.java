package io.ib67.sfcraft;

import com.mojang.serialization.Codec;
import io.ib67.sfcraft.item.AeroBackpack;
import io.ib67.sfcraft.item.SFItem;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class SFItemType {
    public static final ComponentType<String> SF_TYPE = register("sf_item_type",
            builder -> builder.codec(Codec.STRING).packetCodec(PacketCodecs.STRING));
    public static SFItemType INSTANCE;
    public final Item AERO_BACKPACK;
    private final Map<String, Item> mappedItems;

    public SFItemType() {
        mappedItems = new HashMap<>();
        AERO_BACKPACK = register("aero_backpack", new AeroBackpack(new Item.Settings()), "iron_chestplate");
    }

    public <T extends Item & SFItem> T register(String path, T item, String key) {
        var id = Identifier.of("sfcraft", path);
        mappedItems.put(id.toString(), item.getMappedItem());
        return Registry.register(Registries.ITEM, id, item);
    }

    private static <T> ComponentType<T> register(String id, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, id, builderOperator.apply(ComponentType.builder()).build());
    }

    public Item mapId(String id) {
        return mappedItems.get(id);
    }
}
