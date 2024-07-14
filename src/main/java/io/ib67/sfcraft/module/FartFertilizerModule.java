package io.ib67.sfcraft.module;

import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.util.Helper;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.Fertilizable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class FartFertilizerModule extends ServerModule {
    private final Object2LongMap<PlayerEntity> lastSneaked = new Object2LongOpenHashMap<>();

    @Override
    public void onInitialize() {
        SFCallbacks.PLAYER_SNEAKING.register(this::onSneaking);
    }

    private void onSneaking(PlayerEntity player, boolean sneak) {
        if (sneak) {
            var lastSneak = lastSneaked.getOrDefault(player, 0);
            var dlta = Math.min(System.currentTimeMillis() - lastSneak, 100);
            if (player.getRandom().nextBetween(50, 150) > dlta) {
                var playerPos = player.getBlockPos();
                var start = playerPos.add(-2, 0, -2);
                var end = playerPos.add(2, 1, 2);
                for (int dX = 0; dX < end.getX() - start.getX(); dX++) {
                    for (int dZ = 0; dZ < end.getZ() - start.getZ(); dZ++) {
                        for (int dY = 0; dY < end.getY() - start.getY(); dY++) {
                            var blockPos = start.add(dX, dY, dZ);
                            var state = player.getWorld().getBlockState(blockPos);
                            var block = state.getBlock();
                            if (Helper.canFertilize(block)) {
                                var wld = (ServerWorld) player.getWorld();
                                wld.spawnParticles(
                                        ParticleTypes.COMPOSTER,
                                        blockPos.getX(), blockPos.getY(), blockPos.getZ(), 5,
                                        Math.max(0.5, Math.random()), Math.max(0.5, Math.random()), Math.max(0.5, Math.random()),
                                        0.4
                                );
                                for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.around(wld, blockPos, 6f)) {
                                    serverPlayerEntity.playSoundToPlayer(
                                            SoundEvents.BLOCK_COMPOSTER_FILL,
                                            SoundCategory.BLOCKS,
                                            ((6 - serverPlayerEntity.distanceTo(player) + 1) / 6) * 50,
                                            3f
                                    );
                                }
                                if (player.getRandom().nextInt(10) < 3) {
                                    ((Fertilizable) block).grow(wld,
                                            wld.getRandom(), blockPos, state);
                                }
                            }
                        }
                    }
                }
            }
            lastSneaked.put(player, System.currentTimeMillis());
        }
    }
}
