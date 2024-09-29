package io.ib67.sfcraft.mixin.server.item;

import io.ib67.sfcraft.item.TickableItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "updateTurtleHelmet", at = @At("TAIL"))
    public void sf$tickMyItems(CallbackInfo ci) {
        for (ItemStack equippedItem : $this().getEquippedItems()) {
            if (equippedItem.getItem() instanceof TickableItem tickable) {
                tickable.onUpdate($this(), equippedItem);
            }
        }
    }

    @Unique
    private PlayerEntity $this() {
        return (PlayerEntity) (Object) this;
    }
}
