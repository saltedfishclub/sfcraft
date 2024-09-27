package io.ib67.sfcraft.item;

import net.minecraft.item.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class AeroBackpack extends ArmorItem implements SFItem{
    public AeroBackpack(Settings settings) {
        super(ArmorMaterials.IRON, Type.CHESTPLATE, settings);
    }

    @Override
    public Text getName() {
        return Text.literal("喷气背包").withColor(Colors.LIGHT_YELLOW);
    }

    @Override
    public Item getItem() {
        return this;
    }

    @Override
    public Item getMappedItem() {
        return Items.IRON_CHESTPLATE;
    }
}
