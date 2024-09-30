package io.ib67.sfcraft;

import io.ib67.sfcraft.item.AeroBackpack;
import io.ib67.sfcraft.item.SFItem;
import io.ib67.sfcraft.mixin.server.bridge.ItemBridge;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class SFItems {
    public static final Item AERO_BACKPACK = register(
            new AeroBackpack(new Item.Settings().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(15))), "aero_backpack");

    public static void init() {
    }

    private static Item register(Item item, String id) {
        Identifier itemID = Identifier.of(SFCraft.MOD_ID, id);
        Item registeredItem = Registry.register(SFItemRegistry.ITEMS, itemID, item);
        ((ItemBridge)registeredItem).setRegistryEntry(SFItemRegistry.ITEMS.getEntry(itemID).get());
        return registeredItem;
    }
}
