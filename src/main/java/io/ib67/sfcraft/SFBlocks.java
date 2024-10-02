package io.ib67.sfcraft;

import io.ib67.sfcraft.block.ColorWool;
import io.ib67.sfcraft.item.SFBlockItem;
import io.ib67.sfcraft.mixin.common.bridge.BlockBridge;
import io.ib67.sfcraft.util.HackedBlockStateIdList;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class SFBlocks {
    public static final Block COLOR_WOOL = register(
            new ColorWool(AbstractBlock.Settings.create()),
            "color_wool",
            true
    );
    public static final HackedBlockStateIdList HACKED_STATE_IDS = new HackedBlockStateIdList();

    public static void init() {

    }

    public static Block register(Block block, String name, boolean shouldRegisterItem) {
        Identifier id = Identifier.of(SFCraft.MOD_ID, name);

        if (shouldRegisterItem) {
            BlockItem blockItem = new SFBlockItem(block, Items.IRON_BLOCK, new Item.Settings());
            SFItems.register(blockItem, name);
        }
        return Registry.register(Registries.BLOCK, id, block);
    }
}
