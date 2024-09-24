package io.ib67.sfcraft.entity.ai;

import io.ib67.sfcraft.entity.SFGuiderEntity;
import net.minecraft.entity.ai.goal.Goal;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class GuideGoal extends Goal {

    @NotNull
    private final SFGuiderEntity entity;

    public GuideGoal(@NotNull SFGuiderEntity entity) {
        this.entity = entity;
        this.setControls(EnumSet.of(Control.MOVE, Control.JUMP));
    }

    @Override
    public boolean canStart() {
        return entity.stillValid();
    }

    @Override
    public boolean shouldContinue() {
        return entity.stillValid() && !entity.getNavigation().isIdle();
    }

    @Override
    public void start() {
        var target = entity.getTargetPos();
        var player = entity.getPlayer();
        if (target == null || player == null) {
            return;
        }

        var delta = target.toCenterPos()
                .subtract(player.getEyePos())
                .normalize()
                .multiply(5);
        var moveTo = player.getEyePos().add(delta);
        entity.getNavigation()
                .startMovingTo(moveTo.getX(), moveTo.getY(), moveTo.getZ(), 2);
    }

    @Override
    public void tick() {
        var target = entity.getTargetPos();
        if (target == null || !entity.stillValid()) {
            return;
        }

        if (target.isWithinDistance(entity.getPos(), 5)) {
            entity.setTargetPos(null);
        }
    }
}
