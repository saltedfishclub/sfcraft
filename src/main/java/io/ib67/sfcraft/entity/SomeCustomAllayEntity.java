package io.ib67.sfcraft.entity;

import io.ib67.sfcraft.SFCraftInitializer;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class SomeCustomAllayEntity extends AllayEntity {
    public SomeCustomAllayEntity(EntityType<SomeCustomAllayEntity> entityEntityType, World world) {
        super(entityEntityType, world);
    }

    @Override
    public Text getName() {
        return Text.of("guider");
    }

    @Override
    public void tick() {
        super.tick();
        var nearbyPlayer = PlayerLookup.around((ServerWorld) getWorld(), getPos(), 20D);
        for (ServerPlayerEntity serverPlayerEntity : nearbyPlayer) {
            this.navigation.startMovingTo(serverPlayerEntity, 2f);
            break;
        }
    }
}
