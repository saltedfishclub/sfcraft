package io.ib67.sfcraft;

import io.ib67.sfcraft.item.AeroBackpack;
import lombok.SneakyThrows;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class SFItem {
    public static SFItem INSTANCE;
    public final Item AERO_BACKPACK;
    private final Map<String, RegistryEntry<Item>> mappedItems;

    public SFItem() {
        mappedItems = new HashMap<>();
        AERO_BACKPACK = register("aero_backpack", new AeroBackpack(new Item.Settings()), "iron_chestplate");
    }

    public <T extends Item> T register(String path, T item, String key) {
        var id = Identifier.of("sfcraft", path);
        mappedItems.put(id.toString(), Registries.ITEM.entryOf(RegistryKey.of(Registries.ITEM.getKey(), Identifier.ofVanilla(key))));
        return Registry.register(Registries.ITEM, id, item);

    }

    public RegistryEntry<Item> mapId(String id) {
        return mappedItems.get(id);
    }
}
