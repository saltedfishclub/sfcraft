package io.ib67.sfcraft.module.compat;

import io.ib67.sfcraft.SFEntityType;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.entity.SFGuiderEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public class ModCompatModule extends ServerModule {
    @Override
    public void onInitialize() {
    }
}
