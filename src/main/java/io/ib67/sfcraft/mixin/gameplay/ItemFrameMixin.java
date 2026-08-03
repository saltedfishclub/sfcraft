package io.ib67.sfcraft.mixin.gameplay;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.game.mapart.MapArtModule;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 地图画离开物品框即连框消失(不掉落画也不掉落框),见 {@link MapArtModule#onItemFrameDrop}。
 * 私有方法 {@code dropItem(ServerLevel, Entity, boolean)} 是内容物与框本体掉落物的唯一出口。
 * 玩家把地图画放入空框时以该框为左下角自动铺满整幅墙画,见 {@link MapArtModule#onMapArtPlaced};
 * 墙面铺不下(或框朝上/朝下)则在放入前就否掉这次交互,见 {@link MapArtModule#canPlaceMapArt}。
 */
@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
    @Unique
    private boolean sfcraft$wasEmptyBeforeInteract;
    /** interact HEAD 阶段剪出、RETURN 阶段粘贴的"是不是在放地图画"判定结果,避免同一 tick 里二次解析组件。 */
    @Unique
    private boolean sfcraft$placingMapArt;

    @Inject(method = "dropItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void sfcraft$vanishMapArt(ServerLevel level, Entity causedBy, boolean withFrame, CallbackInfo ci) {
        if (SFCraft.getInjector().getInstance(MapArtModule.class)
                .onItemFrameDrop((ItemFrame) (Object) this)) {
            ci.cancel();
        }
    }

    /**
     * 记下交互前框是否为空(TAIL 处判断"这次是放入"用),顺带在原版 {@code setItem}/{@code consume}
     * 之前否掉铺不下的地图画:此时什么都还没发生,拒绝是干净的,不必事后回滚。
     */
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void sfcraft$beforeInteract(Player player, InteractionHand hand, Vec3 pos,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        var frame = (ItemFrame) (Object) this;
        this.sfcraft$wasEmptyBeforeInteract = frame.getItem().isEmpty();
        this.sfcraft$placingMapArt = false; // RETURN 总会回到这里,不复用上一跳的结果
        if (frame.level().isClientSide() || !this.sfcraft$wasEmptyBeforeInteract
                || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        var held = player.getItemInHand(hand);
        if (!MapArtModule.isMapArt(held)) {
            return; // 非地图画:不打扰原版,也不留 placingMapArt 痕迹
        }
        if (!SFCraft.getInjector().getInstance(MapArtModule.class)
                .canPlaceMapArt(frame, held, serverPlayer)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        this.sfcraft$placingMapArt = true;
    }

    // RETURN 而非 TAIL:interact 有多个 return,放画走的是"框空+手上有物品"那条早退分支,
    // TAIL 只会绑到方法末尾的旋转分支(那里 wasEmpty 必为 false,守卫永不成立)。
    @Inject(method = "interact", at = @At("RETURN"))
    private void sfcraft$onMapArtPlaced(Player player, InteractionHand hand, Vec3 pos,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        try {
            if (!this.sfcraft$placingMapArt || ((ItemFrame) (Object) this).level().isClientSide()
                    || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            // HEAD 里已确认是地图画,这里不再二次解析组件。
            if (this.sfcraft$wasEmptyBeforeInteract && !((ItemFrame) (Object) this).getItem().isEmpty()) {
                SFCraft.getInjector().getInstance(MapArtModule.class)
                        .onMapArtPlaced((ItemFrame) (Object) this, serverPlayer);
            }
        } finally {
            // 单条 interact 调用结束就要擦掉,避免堆栈帧复用到下一跳。
            this.sfcraft$placingMapArt = false;
        }
    }
}
