package io.ib67.sfcraft.module.randomevt.longnight;

import io.ib67.sfcraft.module.randomevt.RandomEvent;
import lombok.RequiredArgsConstructor;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.world.World;

import java.awt.*;

@RequiredArgsConstructor
public class DawnAfterLongNightEvent extends RandomEvent {
    private final World world;

    @Override
    public int start() {
        if (LongNightEvent.justExpirencedLongNight) {
            LongNightEvent.justExpirencedLongNight = false;
            world.getServer().getPlayerManager().broadcast(
                    Text.literal("\"月亮\"离开了。")
                            .withColor(new Color(235, 91, 0).getRGB())
                            .append(Text.literal("（永夜事件结束）").withColor(Colors.LIGHT_GRAY))
                    , false);
        }
        return -1;
    }

    @Override
    public void onUpdate(int ticks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void end() {
        throw new UnsupportedOperationException();
    }
}
