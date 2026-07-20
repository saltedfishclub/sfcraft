package io.ib67.sfcraft.mixin.gameplay;

import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieMixin {
    @Inject(method = "canHoldItem", at = @At("HEAD"), cancellable = true)
    public void canHoldItem(final ItemStack itemStack, final CallbackInfoReturnable<Boolean> cir) {
        // 命中黑名单才禁止拾取(返回 false),其余物品交回原版判定
        if (itemStack.is(Items.EGG)
                || itemStack.is(Items.ROTTEN_FLESH)
                || itemStack.is(Items.STRING)
                || itemStack.is(Items.BONE)
                || itemStack.is(Items.ARROW)
                || itemStack.is(Items.WHEAT_SEEDS)) {
            cir.setReturnValue(false);
        }
    }
}
