package io.ib67.sfcraft.module.game.portal;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.GameConfigService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * 鞘翅冲门:玩家使用鞘翅滑翔、以不低于配置阈值(米/秒)的速度冲进下界传送门时,
 * 跳过原本的传送门等待(生存默认 80 tick),下一 tick 直接跨维度。
 * <p>
 * 本模块不注册任何注册表内容;行为由 {@code NetherPortalBlockMixin} 在
 * {@code NetherPortalBlock#getPortalTransitionTime} 头部委托 {@link #shouldTeleportNow}
 * 实现:命中条件时把等待时间覆写为 1 tick,否则放行原版(游戏规则)逻辑。
 * 配置经 {@link GameConfigService} 每次现读,/sfcraft reload 即时生效。
 */
public class ElytraPortalModule extends ServerModule {
    /** 冲门判定时覆写的等待 tick 数:与原版的创造模式进一致。 */
    private static final int INSTANT_TRANSITION_TICKS = 1;

    @Inject
    private GameConfigService config;

    @Override
    public void onInitialize() {
        // 无注册表内容、无事件订阅:特性完全由 mixin 委托驱动
    }

    /** mixin 在下界传送门询问等待时间时调用;命中即把本次等待覆写为 {@link #INSTANT_TRANSITION_TICKS}。 */
    public boolean shouldTeleportNow(Entity entity) {
        if (!(entity instanceof ServerPlayer player) || !player.isFallFlying()) {
            return false;
        }
        double threshold = config.get().elytraPortal.minSpeedBlocksPerSecond;
        // 速度阈值 <= 0 视为关闭本特性
        if (threshold <= 0) {
            return false;
        }
        // deltaMovement 单位为 方块/tick,乘 20 换算成米/秒与配置同单位
        return player.getDeltaMovement().length() * 20.0 >= threshold;
    }

    public int getInstantTransitionTicks() {
        return INSTANT_TRANSITION_TICKS;
    }
}
