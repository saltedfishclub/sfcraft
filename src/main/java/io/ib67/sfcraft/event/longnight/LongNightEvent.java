package io.ib67.sfcraft.event.longnight;

import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.event.RandomEvent;
import lombok.RequiredArgsConstructor;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

@RequiredArgsConstructor
public class LongNightEvent extends RandomEvent {
    static boolean justExpirencedLongNight;
    static boolean isAtLongNight;
    private final World world;
    private boolean doDaylightCycle;
    private boolean doTraderSpawning;

    @Override
    public int start() {
        isAtLongNight = true;
        world.getServer().getPlayerManager().broadcast(Text.literal("月亮停滞在空中...").withColor(Colors.BLUE), false);
        this.doDaylightCycle = world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE);
        this.doTraderSpawning = world.getGameRules().getBoolean(GameRules.DO_TRADER_SPAWNING);
        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, world.getServer());
        world.getGameRules().get(GameRules.DO_TRADER_SPAWNING).set(false, world.getServer());
        return world.random.nextBetween(8400, 2 * 8400);
    }

    @Override
    public void onUpdate(int ticks) {
    }

    @Override
    public void end() {
        isAtLongNight = false;
        justExpirencedLongNight = true;
        world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(doDaylightCycle, world.getServer());
        world.getGameRules().get(GameRules.DO_TRADER_SPAWNING).set(doTraderSpawning, world.getServer());
    }

    public static boolean isRunning() {
        return isAtLongNight;
    }
}
