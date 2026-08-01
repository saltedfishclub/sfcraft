package io.ib67.sfcraft.mixin.gameplay;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.game.mapart.MapArtAnvilAccess;
import io.ib67.sfcraft.module.game.mapart.MapArtModule;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 地图画的铁砧入口:把(空)地图重命名为 http(s) URL 时,由 {@link MapArtModule#onAnvilResult}
 * 接管结果槽(压制原版重命名结果,下载渲染完成后置入地图画)。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin implements MapArtAnvilAccess {
    @Shadow
    @Final
    protected Container inputSlots;
    @Shadow
    @Final
    protected ResultContainer resultSlots;
    @Shadow
    @Final
    protected Player player;
    @Shadow
    private @Nullable String itemName;
    @Shadow
    @Final
    private DataSlot cost;

    @Override
    public ItemStack sfcraft$getInput() {
        return this.inputSlots.getItem(AnvilMenu.INPUT_SLOT);
    }

    @Override
    public @Nullable String sfcraft$getItemName() {
        return this.itemName;
    }

    @Override
    public void sfcraft$setResult(ItemStack stack, int xpCost) {
        this.resultSlots.setItem(0, stack);
        this.cost.set(xpCost);
        ((AbstractContainerMenu) (Object) this).broadcastChanges();
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void sfcraft$mapArtResult(CallbackInfo ci) {
        SFCraft.getInjector().getInstance(MapArtModule.class)
                .onAnvilResult((AnvilMenu) (Object) this, this.player);
    }
}
