package io.ib67.sfcraft.entity;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FollowMobGoal;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class SFGuiderEntity extends AllayEntity {
    public SFGuiderEntity(EntityType<SFGuiderEntity> entityEntityType, World world) {
        super(entityEntityType, world);
        this.clearGoalsAndTasks();
        this.goalSelector.add(0, new FollowMobGoal());
    }

    @Override
    public Text getName() {
        return Text.of("guider");
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public boolean shouldSave() {
        return false;
    }
}
