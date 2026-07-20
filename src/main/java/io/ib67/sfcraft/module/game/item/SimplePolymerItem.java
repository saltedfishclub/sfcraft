package io.ib67.sfcraft.module.game.item;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SimplePolymerItem extends Item implements PolymerItem {
    private final Item polymerItem;

    public SimplePolymerItem(Properties properties, Item polymerItem) {
        super(properties);
        this.polymerItem = polymerItem;
    }

    @Override
    public Item getPolymerItem(ItemStack stack, PacketContext context) {
        return polymerItem;
    }
}
