package io.ib67.sfcraft.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;

public class AeroBackpack extends ArmorItem {
    public AeroBackpack(Settings settings) {
        super(ArmorMaterials.IRON, Type.CHESTPLATE, settings);
    }

    @Override
    public Text getName() {
        return Text.literal("喷气背包").withColor(Colors.LIGHT_YELLOW);
    }
}
