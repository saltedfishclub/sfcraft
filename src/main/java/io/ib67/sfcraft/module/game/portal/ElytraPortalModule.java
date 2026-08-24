package io.ib67.sfcraft.module.game.portal;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.GameConfigService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 鞘翅冲门:玩家使用鞘翅滑翔、以不低于配置阈值(米/秒)的速度冲进下界传送门时,
 * 跳过原本的传送门等待(生存默认 80 tick),当场跨维度。
 * <p>
 * 本模块不注册任何注册表内容;行为由两个 mixin 委托驱动:
 * <ul>
 *   <li>{@code NetherPortalBlockMixin} 在 {@code NetherPortalBlock#getPortalTransitionTime} 头部委托
 *       {@link #shouldTeleportNow},命中条件时把等待时间覆写掉,否则放行原版(游戏规则)逻辑;</li>
 *   <li>{@code ElytraRushMixin} 负责记录撞墙前的速度({@link #mixin$recordRushSpeed})、免掉门后撞墙的
 *       动能伤害({@link #shouldIgnoreWallImpact})。</li>
 * </ul>
 * 配置经 {@link GameConfigService} 每次现读,/sfcraft reload 即时生效。
 */
public class ElytraPortalModule extends ServerModule {
    /** 冲门判定时覆写的等待 tick 数:0 = 本次 handlePortal 就传送,不再多贴一 tick 在墙上。 */
    private static final int INSTANT_TRANSITION_TICKS = 0;

    /**
     * 撞墙前速度快照的有效期(tick)。进门发生在第 N tick 的 aiStep 末尾(applyEffectsFromBlocks),
     * 传送门判定在第 N+1 tick 开头的 handlePortal,理论上只差 1 tick,留点余量应付被墙磨蹭几 tick 的情况。
     */
    private static final int RUSH_MEMORY_TICKS = 3;

    @Inject
    private GameConfigService config;

    @Override
    public void onInitialize() {
        // 无注册表内容、无事件订阅:特性完全由 mixin 委托驱动
    }

    /**
     * {@code LivingEntity#travelFallFlying} 头部调用:此刻还在 {@code move()} 之前、deltaMovement 尚未被
     * 墙裁剪,速度够快就记下来。只记够快的——这样玩家撞墙后那些近零速的 tick 不会把快照冲掉。
     */
    public void mixin$recordRushSpeed(ServerPlayer player) {
        double threshold = threshold();
        if (threshold <= 0) {
            return;
        }
        Vec3 delta = player.getDeltaMovement();
        if (metersPerSecond(delta) < threshold) {
            return;
        }
        ((ElytraRushHolder) player).sfcraft$recordElytraRush(delta, player.tickCount);
    }

    /** mixin 在下界传送门询问等待时间时调用;命中即把本次等待覆写为 {@link #INSTANT_TRANSITION_TICKS}。 */
    public boolean shouldTeleportNow(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return false;
        }
        double threshold = threshold();
        // 速度阈值 <= 0 视为关闭本特性
        if (threshold <= 0) {
            return false;
        }
        // 门后没遮挡的常规情况:当前速度就够快
        if (player.isFallFlying() && metersPerSecond(player.getDeltaMovement()) >= threshold) {
            return true;
        }
        // 门后有遮挡:进门那一 tick 已经被墙撞停(连 isFallFlying 都可能因随后落地而取消),只能看快照
        return recentRushDelta(player) != null;
    }

    /**
     * {@code LivingEntity#handleFallFlyingCollisions} 头部调用:玩家高速冲进传送门方块、又撞上门后墙体时,
     * 这一下动能伤害不该算在他头上——他本来就是要穿过去的。
     *
     * @param preCollisionSpeedPerTick 原版传进来的碰撞前水平速度,单位 方块/tick
     */
    public boolean shouldIgnoreWallImpact(ServerPlayer player, double preCollisionSpeedPerTick) {
        double threshold = threshold();
        return threshold > 0
                && metersPerSecond(preCollisionSpeedPerTick) >= threshold
                && isTouchingNetherPortal(player);
    }

    /**
     * 传送前把被墙撞掉的速度补回来。原版的 {@code TeleportTransition} 带 {@code Relative.DELTA}
     * (含 {@code ROTATE_DELTA}),会保留并按出口朝向旋转玩家速度,所以这里只要把快照写回去,
     * 出门那一侧就能继续原速滑翔,而不是零速掉下去。
     */
    public void restoreRushVelocity(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        double threshold = threshold();
        // 只在速度确实掉到阈值以下(被墙吃了)时才补;门后没遮挡的正常冲门不去动它
        if (threshold <= 0 || metersPerSecond(player.getDeltaMovement()) >= threshold) {
            return;
        }
        Vec3 recorded = recentRushDelta(player);
        if (recorded == null) {
            return;
        }
        player.setDeltaMovement(recorded);
        // 玩家移动是客户端权威的,而客户端同样预测到了这次撞墙。不补一个速度包的话,
        // 服务端这边写回去的速度会立刻被客户端的位置汇报盖掉。
        player.hurtMarked = true;
    }

    public int getInstantTransitionTicks() {
        return INSTANT_TRANSITION_TICKS;
    }

    /** 仍在有效期内的撞墙前速度快照;过期或从未记录过返回 null。 */
    private @Nullable Vec3 recentRushDelta(ServerPlayer player) {
        var holder = (ElytraRushHolder) player;
        Vec3 delta = holder.sfcraft$elytraRushDelta();
        if (delta == null || player.tickCount - holder.sfcraft$elytraRushTick() > RUSH_MEMORY_TICKS) {
            return null;
        }
        return delta;
    }

    /** 玩家碰撞箱是否正压在传送门方块上——撞墙那一 tick 传送门还没被登记,只能直接看方块。 */
    private boolean isTouchingNetherPortal(ServerPlayer player) {
        var level = player.level();
        // 稍微收一点碰撞箱,避免只是紧贴着门的隔壁方块也被算进来
        return BlockPos.betweenClosedStream(player.getBoundingBox().deflate(1.0E-6))
                .anyMatch(pos -> level.getBlockState(pos).is(Blocks.NETHER_PORTAL));
    }

    private double threshold() {
        return config.get().elytraPortal.minSpeedBlocksPerSecond;
    }

    /** deltaMovement 单位为 方块/tick,乘 20 换算成米/秒与配置同单位。 */
    private static double metersPerSecond(double blocksPerTick) {
        return blocksPerTick * 20.0;
    }

    private static double metersPerSecond(Vec3 delta) {
        return metersPerSecond(delta.length());
    }
}
