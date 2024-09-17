package io.ib67.sfcraft.mixin.server.item;

import io.ib67.sfcraft.SFItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
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

    @Shadow
    public abstract PlayerAbilities getAbilities();

    @Unique
    private boolean wearingBackpack;

    @Inject(at = @At("HEAD"), method = "updateTurtleHelmet")
    public void sf$updateBackpack(CallbackInfo ci) {
        var item = this.getEquippedStack(EquipmentSlot.CHEST);
        if (getAbilities().creativeMode) return;
        var query = item.isOf(SFItem.INSTANCE.AERO_BACKPACK);;
        if(wearingBackpack != query){
            System.out.println("State change!");
            this.getAbilities().flying = item.isOf(SFItem.INSTANCE.AERO_BACKPACK);
            wearingBackpack = query;
            ((ServerPlayerEntity)(Object)this).networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(getAbilities()));
        }
    }
}
