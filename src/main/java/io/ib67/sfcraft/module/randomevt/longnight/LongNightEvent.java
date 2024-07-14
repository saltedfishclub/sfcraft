package io.ib67.sfcraft.module.randomevt.longnight;

import io.ib67.sfcraft.module.randomevt.RandomEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

@RequiredArgsConstructor
public class LongNightEvent extends RandomEvent {
    static boolean justExpirencedLongNight;
    static boolean isAtLongNight;
    @Getter
    private static int remainingTicks;
    private final World world;
    private boolean doDaylightCycle;
    private boolean doTraderSpawning;

    @Override
    public int start() {
        var pm = world.getServer().getPlayerManager();
        isAtLongNight = true;
        pm.broadcast(Text.literal("月亮停滞在空中...").withColor(Colors.BLUE), false);
        this.doDaylightCycle = world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE);
        this.doTraderSpawning = world.getGameRules().getBoolean(GameRules.DO_TRADER_SPAWNING);
        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, world.getServer());
        world.getGameRules().get(GameRules.DO_TRADER_SPAWNING).set(false, world.getServer());
        remainingTicks = world.random.nextBetween(8400, 2 * 8400);
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
        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(doDaylightCycle, world.getServer());
        world.getGameRules().get(GameRules.DO_TRADER_SPAWNING).set(doTraderSpawning, world.getServer());
    }

    public static boolean isRunning() {
        return isAtLongNight;
    }
}
