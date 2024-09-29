package io.ib67.sfcraft.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public interface TickableItem {
    void onUpdate(PlayerEntity player, ItemStack stack);
}
