package io.ib67.sfcraft.item;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;

public class SFBlockItem extends BlockItem implements SFItem {
    private final Item mappedItem;
    public SFBlockItem(Block block, Item mappedItem, Settings settings) {
        super(block, settings);
        this.mappedItem = mappedItem;
    }

    @Override
    public Item getMappedItem() {
        return mappedItem;
    }
}
