package io.ib67.sfcraft.module.game.portal;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 由 {@code ElytraRushMixin} 混入 LivingEntity:记住玩家本 tick 移动「被碰撞裁剪之前」的滑翔速度。
 * <p>
 * 传送门后面有遮挡时,玩家进门那一 tick 就被墙撞停(deltaMovement 归零),而传送门是在下一 tick
 * 开头的 {@code Entity#handlePortal} 里才问「还要等多久」,那时候速度早就没了、判不出冲门。
 * 这份快照就是给那一刻补判用的,见 {@link ElytraPortalModule#shouldTeleportNow}。
 */
public interface ElytraRushHolder {
    /** 记下够快的滑翔速度及其发生的 tickCount;低速 tick 不会来覆盖,快照只靠 tick 窗口过期。 */
    void sfcraft$recordElytraRush(Vec3 delta, int tick);

    /** 最近一次记录的速度;从未记录过则为 null。 */
    @Nullable
    Vec3 sfcraft$elytraRushDelta();

    /** 上一次记录发生在哪个 {@code Entity#tickCount}。 */
    int sfcraft$elytraRushTick();
}
