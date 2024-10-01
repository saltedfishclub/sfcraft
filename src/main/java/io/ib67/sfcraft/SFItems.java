package io.ib67.sfcraft;

import io.ib67.sfcraft.item.AeroBackpack;
import io.ib67.sfcraft.mixin.common.bridge.ItemBridge;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class SFItems {
    public static final Item AERO_BACKPACK = register(
            new AeroBackpack(new Item.Settings().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(15))), "aero_backpack");
    public static final String SF_ITEM_TYPE_KEY = "sf_type";

    public static void init() {
    }

    public static Item register(Item item, String id) {
        Identifier itemID = Identifier.of(SFCraft.MOD_ID, id);
        Item registeredItem = Registry.register(SFRegistries.ITEMS, itemID, item);
        ((ItemBridge) registeredItem).setRegistryEntry(SFRegistries.ITEMS.getEntry(itemID).get());
        return registeredItem;
    }

    public static Item getById(Identifier item) {
        return SFRegistries.ITEMS.get(item);
    }
}
