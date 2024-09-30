package io.ib67.sfcraft.item.internal;

import io.ib67.sfcraft.SFItemRegistry;
import io.ib67.sfcraft.SFItems;
import io.ib67.sfcraft.item.SFItem;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentMapImpl;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class WrappedItemStack<T extends Item & SFItem> extends ItemStack {
    protected final T sfItem;
    protected final ItemStack mimicStack;

    public static <T extends Item & SFItem> WrappedItemStack<T> create(RegistryEntry<Item> itemKey) {
        var item = (T) itemKey.value();
        var mapped = item.getMappedItem();
        var _item = new ItemStack(mapped);
        _item.set(SFItemRegistry.SF_ITEM_TYPE, item.getRegistryEntry().getIdAsString());
        return new WrappedItemStack<>(_item);
    }

    public WrappedItemStack(ItemStack toCopy) {
        super(toCopy.getItem(), toCopy.getCount(), new ComponentMapImpl(toCopy.getComponents()));
        this.mimicStack = toCopy;
        var originalComponents = toCopy.getComponents();
        if (originalComponents.contains(SFItemRegistry.SF_ITEM_TYPE)) {
            var identifier = originalComponents.get(SFItemRegistry.SF_ITEM_TYPE);
            sfItem = (T) SFItemRegistry.ITEMS.get(Identifier.tryParse(identifier));
        } else {
            this.sfItem = null;
        }
    }

    @Override
    public RegistryEntry<Item> getRegistryEntry() {
        return mimicStack.getRegistryEntry();
    }

    @Override
    public Item getItem() {
        return sfItem == null ? super.getItem() : sfItem;
    }
}
