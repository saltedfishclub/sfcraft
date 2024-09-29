package io.ib67.sfcraft.item.internal;

import io.ib67.sfcraft.SFItemRegistry;
import io.ib67.sfcraft.SFItems;
import io.ib67.sfcraft.item.SFItem;
import net.minecraft.component.ComponentChanges;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class WrappedItemStack extends ItemStack {
    public WrappedItemStack(RegistryEntry<Item> item, int count, ComponentChanges changes) {
        super(wrappedItem(item, changes), count, changes);
    }

    private static RegistryEntry<Item> wrappedItem(RegistryEntry<Item> item, ComponentChanges changes) {
        var id = changes.get(SFItemRegistry.SF_ITEM_TYPE);
        if (id == null || id.isEmpty()) return item;
        var sfIdentifier = id.get();
        var entry = SFItemRegistry.ITEMS.get(Identifier.tryParse(sfIdentifier));
        return ((SFItem)entry.asItem()).getMappedItem().getRegistryEntry();
    }

    public static WrappedItemStack of(ItemStack itemStack) {
        return new WrappedItemStack(itemStack.getRegistryEntry(), itemStack.getCount(), itemStack.getComponentChanges());
    }
}
