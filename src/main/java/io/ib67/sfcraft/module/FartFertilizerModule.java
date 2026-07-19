package io.ib67.sfcraft.module;

import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.callback.SFCallbacks;
import io.ib67.sfcraft.util.Helper;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealableBlock;

public class FartFertilizerModule extends ServerModule {
    private final Object2LongMap<Player> lastSneaked = new Object2LongOpenHashMap<>();

    @Override
    public void onInitialize() {
        SFCallbacks.PLAYER_SNEAKING.register(this::onSneaking);
    }

    private void onSneaking(Player player, boolean sneak) {
        if (sneak) {
            var lastSneak = lastSneaked.getOrDefault(player, 0);
            var dlta = Math.min(System.currentTimeMillis() - lastSneak, 100);
            if (player.getRandom().nextIntBetweenInclusive(50, 150) > dlta) {
                var playerPos = player.blockPosition();
                var start = playerPos.offset(-5, -2, -5);
                var end = playerPos.offset(5, 2, 5);
                for (int dX = 0; dX < end.getX() - start.getX(); dX++) {
                    for (int dZ = 0; dZ < end.getZ() - start.getZ(); dZ++) {
                        for (int dY = 0; dY < end.getY() - start.getY(); dY++) {
                            var blockPos = start.offset(dX, dY, dZ);
                            var state = player.level().getBlockState(blockPos);
                            var block = state.getBlock();
                            if (Helper.canFertilize(block)) {
                                var wld = (ServerLevel) player.level();
                                wld.sendParticles(
                                        ParticleTypes.COMPOSTER,
                                        blockPos.getX(), blockPos.getY(), blockPos.getZ(), 5,
                                        Math.max(0.5, Math.random()), Math.max(0.5, Math.random()), Math.max(0.5, Math.random()),
                                        0.4
                                );
                                for (ServerPlayer serverPlayerEntity : PlayerLookup.around(wld, blockPos, 6f)) {
                                    Helper.playNotifySound(
                                            serverPlayerEntity,
                                            SoundEvents.COMPOSTER_FILL,
                                            SoundSource.BLOCKS,
                                            ((6 - serverPlayerEntity.distanceTo(player) + 1) / 6) * 50,
                                            3f
                                    );
                                }
                                if (player.getRandom().nextInt(10) < 3) {
                                    ((BonemealableBlock) block).performBonemeal(wld,
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
