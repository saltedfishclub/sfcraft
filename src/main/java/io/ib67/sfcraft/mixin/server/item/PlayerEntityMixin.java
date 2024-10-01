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
    @Shadow
    public abstract ItemStack getEquippedStack(EquipmentSlot slot);

    @Inject(method = "updateTurtleHelmet", at = @At("TAIL"))
    public void sf$tickMyItems(CallbackInfo ci) {
        checkAndUpdate(EquipmentSlot.OFFHAND);
        checkAndUpdate(EquipmentSlot.MAINHAND);
        checkAndUpdate(EquipmentSlot.HEAD);
        checkAndUpdate(EquipmentSlot.CHEST);
        checkAndUpdate(EquipmentSlot.LEGS);
        checkAndUpdate(EquipmentSlot.FEET);
        checkAndUpdate(EquipmentSlot.BODY);
    }

    @Unique
    private void checkAndUpdate(EquipmentSlot equipmentSlot) {
        var item = getEquippedStack(equipmentSlot);
        if (item.getItem() instanceof TickableItem tickableItem) {
            tickableItem.onUpdate($this(), item, equipmentSlot);
        }
    }

    @Unique
    private PlayerEntity $this() {
        return (PlayerEntity) (Object) this;
    }
}
