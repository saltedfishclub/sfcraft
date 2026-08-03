package io.ib67.sfcraft.mixin.server;

import com.mojang.datafixers.util.Either;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.module.game.lunchbox.LunchBoxItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerEntityMixin {
    @Shadow
    private long lastActionTime;

    @Inject(method = "die", at = @At("TAIL"))
    public void onDeath(DamageSource damageSource, CallbackInfo ci) {
        SFCallbacks.PLAYER_DEATH.invoker().onPlayerDeath((ServerPlayer) (Object) this, damageSource);
    }

    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    public void onSleep(BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        var result = SFCallbacks.PLAYER_SLEEP.invoker().onPlayerSleep((ServerPlayer) (Object) this, pos);
        if (result.left().isPresent()) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "resetLastActionTime", at = @At("HEAD"))
    public void leaveAFK(CallbackInfo ci) {
        if (lastActionTime == Long.MAX_VALUE) {
            SFCallbacks.PLAYER_IDLE.invoker().onSwitchIdle((ServerPlayer) (Object) this, false);
        }
    }

    /**
     * 每 tick 检查玩家当前打开的菜单:若已不是默认背包(InventoryMenu),就把所有午餐盒 stack 切到 BAG 模式
     * (伪装成 Items.BUNDLE,客户端袋式原生),否则切回 NORMAL 模式(伪装成 Items.BOWL,能吃)。
     * <p>
     * 慢路径(每 tick 扫一次 41 格的 inventory)开销可忽略;代价是打开/关闭背包与真正切 mode 之间至多滞后 1 tick。
     */
    private static final org.slf4j.Logger LB_LOG = org.slf4j.LoggerFactory.getLogger("sfcraft.lunchbox.mixin");

    @Inject(method = "doTick", at = @At("TAIL"))
    public void sfcraft$tickLunchBoxMode(CallbackInfo ci) {
        var self = (ServerPlayer) (Object) this;
        boolean inBag = self.containerMenu != self.inventoryMenu;
        var inventory = self.getInventory();
        int size = inventory.getContainerSize();
        int lb = 0;
        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!(stack.getItem() instanceof LunchBoxItem)) continue;
            lb++;
            if (LunchBoxItem.isBagMode(stack) == inBag) continue;
            LunchBoxItem.setBagMode(stack, inBag);
            inventory.setChanged();
            LB_LOG.info("[mode] slot={} switched to {}", i, inBag ? "BAG" : "NORMAL");
        }
        if (lb > 0 && LB_LOG.isDebugEnabled()) {
            LB_LOG.debug("[mode] scan {} lunchbox, inBag={}", lb, inBag);
        }
    }
}
