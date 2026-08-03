package io.ib67.sfcraft.mixin.gameplay;

import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.module.game.mapart.MapArtAnvilAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 铁砧钩子:在 {@code createResult} 尾部发布 {@link SFCallbacks#ANVIL_CREATE_RESULT} 事件。
 * 附带实现 {@link MapArtAnvilAccess}(鸭接口),向监听器暴露输入/命名/结果槽的读写能力——
 * 地图画模块据此在重命名为图片 URL 时接管结果槽。
 *
 * <p>26.2 中 {@code inputSlots}/{@code resultSlots}/{@code player} 已上移至
 * {@link net.minecraft.world.inventory.ItemCombinerMenu},@Shadow 无法解析父类字段,
 * 故输入/结果槽改走公开的槽位表访问,player 在构造时从 {@link Inventory} 捕获。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin implements MapArtAnvilAccess {
    @Shadow
    private @Nullable String itemName;
    @Shadow
    @Final
    private DataSlot cost;

    @Unique
    private Player sfcraft$player;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("TAIL"))
    private void sfcraft$capturePlayer(int syncId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.sfcraft$player = inventory.player;
    }

    @Override
    public ItemStack sfcraft$getInput() {
        return ((AbstractContainerMenu) (Object) this).slots.get(AnvilMenu.INPUT_SLOT).getItem();
    }

    @Override
    public @Nullable String sfcraft$getItemName() {
        return this.itemName;
    }

    @Override
    public void sfcraft$setResult(ItemStack stack, int xpCost) {
        ((AbstractContainerMenu) (Object) this).slots.get(AnvilMenu.RESULT_SLOT).container.setItem(0, stack);
        this.cost.set(xpCost);
        ((AbstractContainerMenu) (Object) this).broadcastChanges();
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void sfcraft$mapArtResult(CallbackInfo ci) {
        SFCallbacks.ANVIL_CREATE_RESULT.invoker()
                .onAnvilCreateResult((AnvilMenu) (Object) this, this.sfcraft$player);
    }
}
