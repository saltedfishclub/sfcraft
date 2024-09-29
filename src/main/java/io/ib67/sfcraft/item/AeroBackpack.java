package io.ib67.sfcraft.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

public class AeroBackpack extends ArmorItem implements TickableItem, SFItem {
    private static final Text STATUS_MESSAGE = Text.of("You're wearing aero backpack!");

    public AeroBackpack(Settings settings) {
        super(ArmorMaterials.IRON, Type.CHESTPLATE, settings);
    }

    @Override
    public void onUpdate(PlayerEntity player, ItemStack stack) {
        player.sendMessage(STATUS_MESSAGE, true);
    }

    @Override
    public Item getMappedItem() {
        return Items.IRON_CHESTPLATE;   
    }
}
