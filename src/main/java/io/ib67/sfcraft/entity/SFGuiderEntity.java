package io.ib67.sfcraft.entity;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.Level;

public class SFGuiderEntity extends Allay {
    public SFGuiderEntity(EntityType<SFGuiderEntity> entityEntityType, Level world) {
        super(entityEntityType, world);
    }

    @Override
    public Component getName() {
        return Component.nullToEmpty("guider");
    }

    @Override
    public void tick() {
        super.tick();
        var nearbyPlayer = PlayerLookup.around((ServerLevel) level(), position(), 20D);
        for (ServerPlayer serverPlayerEntity : nearbyPlayer) {
            this.navigation.moveTo(serverPlayerEntity, 2f);
            break;
        }
    }
}
