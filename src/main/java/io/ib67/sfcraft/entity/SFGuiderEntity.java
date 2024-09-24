package io.ib67.sfcraft.entity;

import io.ib67.sfcraft.entity.ai.GuideGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SFGuiderEntity extends AllayEntity {

    private int lifeRemain = 200;

    @Nullable
    private BlockPos targetPos;

    @Nullable
    private ServerPlayerEntity player;

    public SFGuiderEntity(EntityType<SFGuiderEntity> entityEntityType, World world) {
        super(entityEntityType, world);
        this.clearGoalsAndTasks();

        if (!world.isClient) {
            initGoals();
        }
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new GuideGoal(this));
    }

    public SFGuiderEntity(EntityType<SFGuiderEntity> entityEntityType, World world,
                          @NotNull ServerPlayerEntity player, @NotNull BlockPos targetPos) {
        this(entityEntityType, world);
        this.targetPos = targetPos;
        this.player = player;
    }

    public @Nullable BlockPos getTargetPos() {
        return targetPos;
    }

    public void setTargetPos(@Nullable BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    public @Nullable ServerPlayerEntity getPlayer() {
        return player;
    }

    @Override
    public void tick() {
        super.tick();

        if (!stillValid()) {
            if (lifeRemain > 0) {
                lifeRemain -= 1;
            } else {
                this.discard();
            }
        }
    }

    @Override
    public Text getName() {
        return Text.of("guider");
    }

    public boolean stillValid() {
        return targetPos != null && player != null && !player.isDisconnected();
    }
}
