package io.ib67.sfcraft.module.randomevt.longnight;

import io.ib67.sfcraft.module.randomevt.RandomEvent;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.world.level.Level;
import java.awt.*;

@RequiredArgsConstructor
public class DawnAfterLongNightEvent extends RandomEvent {
    private final Level world;

    @Override
    public int start() {
        if (LongNightEvent.justExpirencedLongNight) {
            LongNightEvent.justExpirencedLongNight = false;
            world.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("\"月亮\"离开了。")
                            .withColor(new Color(235, 91, 0).getRGB())
                            .append(Component.literal("（永夜事件结束）").withColor(CommonColors.LIGHT_GRAY))
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
