package io.ib67.sfcraft.module.game.totem;

import net.minecraft.world.entity.player.Player;

/**
 * 计算玩家的总经验点数。玩家的 {@code totalExperience} 字段有已知的不同步问题(如附魔消耗后不减),
 * 这里改用 等级 + 进度条 精确重算,与原版经验条显示一致。
 */
public final class ExperienceHelper {
    private ExperienceHelper() {
    }

    public static int total(Player player) {
        return xpAtLevelStart(player.experienceLevel)
                + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    // 从 0 级累积到 level 级所需的总经验点数(原版分段公式)
    private static int xpAtLevelStart(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }
}
