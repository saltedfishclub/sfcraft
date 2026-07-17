package io.ib67.sfcraft.module.randomevt.longnight;

import io.ib67.sfcraft.module.randomevt.RandomEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.CommonColors;
import net.minecraft.world.level.GameRules;

@RequiredArgsConstructor
public class LongNightEvent extends RandomEvent {
    static boolean justExpirencedLongNight;
    static boolean isAtLongNight;
    @Getter
    private static int remainingTicks;
    private final ServerLevel world;
    private boolean doDaylightCycle;
    private boolean doTraderSpawning;

    @Override
    public int start() {
        var pm = world.getServer().getPlayerList();
        isAtLongNight = true;
        pm.broadcastSystemMessage(Component.literal("月亮停滞在空中...").withColor(CommonColors.BLUE), false);
        this.doDaylightCycle = world.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        this.doTraderSpawning = world.getGameRules().getBoolean(GameRules.RULE_DO_TRADER_SPAWNING);
        world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, world.getServer());
        world.getGameRules().getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, world.getServer());
        remainingTicks = world.random.nextIntBetweenInclusive(8400, 2 * 8400);
        return remainingTicks;
    }

    @Override
    public void onUpdate(int ticks) {
        remainingTicks--;
    }

    @Override
    public void end() {
        isAtLongNight = false;
        justExpirencedLongNight = true;
        remainingTicks = 0;
        world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(doDaylightCycle, world.getServer());
        world.getGameRules().getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(doTraderSpawning, world.getServer());
    }

    public static boolean isRunning() {
        return isAtLongNight;
    }
}
