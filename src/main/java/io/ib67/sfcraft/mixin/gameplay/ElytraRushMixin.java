package io.ib67.sfcraft.mixin.gameplay;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.module.game.portal.ElytraPortalModule;
import io.ib67.sfcraft.module.game.portal.ElytraRushHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 鞘翅冲门在「传送门后面有遮挡」这一情况下的两处补偿:
 * <ol>
 *   <li>{@code travelFallFlying} 头部——此处还在 {@code move()} 之前,deltaMovement 尚未被墙裁剪,
 *       把速度存进 {@link ElytraRushHolder} 快照,供下一 tick 的传送门判定使用;</li>
 *   <li>{@code handleFallFlyingCollisions} 头部——原版在 {@code move()} 之后按碰撞前后的速度差扣动能伤害,
 *       玩家高速冲进传送门方块又撞上门后墙体的这一下不该算他的,直接取消。</li>
 * </ol>
 * 判定与配置读取都在 {@link ElytraPortalModule}。
 */
@Mixin(LivingEntity.class)
public abstract class ElytraRushMixin implements ElytraRushHolder {
    @Unique
    private Vec3 sfcraft$rushDelta;

    @Unique
    private int sfcraft$rushTick;

    @Inject(method = "travelFallFlying", at = @At("HEAD"))
    private void sfcraft$recordRushSpeed(Vec3 input, CallbackInfo ci) {
        if ($this() instanceof ServerPlayer player) {
            sfcraft$module().mixin$recordRushSpeed(player);
        }
    }

    @Inject(method = "handleFallFlyingCollisions", at = @At("HEAD"), cancellable = true)
    private void sfcraft$skipPortalWallImpact(double moveHorLength, double newMoveHorLength, CallbackInfo ci) {
        if ($this() instanceof ServerPlayer player && sfcraft$module().shouldIgnoreWallImpact(player, moveHorLength)) {
            ci.cancel();
        }
    }

    @Override
    public void sfcraft$recordElytraRush(Vec3 delta, int tick) {
        sfcraft$rushDelta = delta;
        sfcraft$rushTick = tick;
    }

    @Override
    public Vec3 sfcraft$elytraRushDelta() {
        return sfcraft$rushDelta;
    }

    @Override
    public int sfcraft$elytraRushTick() {
        return sfcraft$rushTick;
    }

    @Unique
    private ElytraPortalModule sfcraft$module() {
        return SFCraft.getInjector().getInstance(ElytraPortalModule.class);
    }

    @Unique
    private LivingEntity $this() {
        return (LivingEntity) (Object) this;
    }
}
