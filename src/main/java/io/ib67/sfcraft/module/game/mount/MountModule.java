package io.ib67.sfcraft.module.game.mount;

import com.google.inject.Inject;
import io.ib67.sfcraft.ServerModule;
import io.ib67.sfcraft.config.GameConfigService;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * 驯服坐骑的交互入口:
 * <ul>
 *   <li>{@link UseEntityCallback}:喂食驯服 / 上鞍 / 骑乘</li>
 *   <li>{@link UseItemCallback}:骑乘劫掠兽时用胡萝卜钓竿向前冲刺</li>
 * </ul>
 */
public class MountModule extends ServerModule {
    @Inject
    private GameConfigService config;

    @Override
    public void onInitialize() {
        UseEntityCallback.EVENT.register(this::onUseEntity);
        UseItemCallback.EVENT.register(this::onUseItem);
    }

    private InteractionResult onUseEntity(Player player, Level level, InteractionHand hand,
                                          Entity entity, EntityHitResult hit) {
        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(entity instanceof MountAccess mount) || !(entity instanceof Mob mob)) return InteractionResult.PASS;

        var mountConfig = config.get().mount;
        ItemStack held = player.getItemInHand(hand);

        // 未驯服:喂食驯服(需满足虚弱/无掠夺者/非袭击)
        if (mount.sfcraft$getOwner() == null) {
            Item food = MountLogic.resolveItem(
                    mob instanceof Hoglin ? mountConfig.hoglinTameFood : mountConfig.ravagerTameFood, Items.GOLDEN_APPLE);
            if (!held.is(food)) return InteractionResult.PASS;
            Component reason = MountLogic.tameBlockReason(mob);
            if (reason != null) {
                if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendOverlayMessage(reason);
                return InteractionResult.FAIL;
            }
            // 喂食消耗;仅有一定概率(默认 20%)驯服成功
            if (!player.getAbilities().instabuild) held.consume(1, player);
            if (mob.getRandom().nextFloat() < mountConfig.tameSuccessChance) {
                mount.sfcraft$setOwner(player.getUUID());
                mob.setPersistenceRequired();
                celebrate(mob);
            } else {
                tameFailed(mob);
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.sendOverlayMessage(Component.translatable("message.sfcraft.mount.tame_failed"));
                }
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        // 已驯服:仅主人可交互
        if (!player.getUUID().equals(mount.sfcraft$getOwner())) return InteractionResult.PASS;

        // 上鞍
        if (!mount.sfcraft$isSaddled() && held.is(Items.SADDLE)) {
            mount.sfcraft$setSaddled(true);
            if (!player.getAbilities().instabuild) held.consume(1, player);
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1.0F, 1.0F);
            return InteractionResult.SUCCESS_SERVER;
        }

        // 骑乘:空手右键(需已上鞍,若配置要求)
        boolean canRide = !mountConfig.requireSaddleToRide || mount.sfcraft$isSaddled();
        if (canRide && held.isEmpty() && !player.isSecondaryUseActive()) {
            player.startRiding(mob);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    private InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.CARROT_ON_A_STICK)) return InteractionResult.PASS;
        // 仅骑乘已驯服劫掠兽时冲刺
        if (!(player.getVehicle() instanceof Ravager ravager) || !(player.getVehicle() instanceof MountAccess mount)) {
            return InteractionResult.PASS;
        }
        if (!player.getUUID().equals(mount.sfcraft$getOwner())) return InteractionResult.PASS;
        if (player.getCooldowns().isOnCooldown(held)) return InteractionResult.PASS;

        var dash = config.get().mount.ravagerDash;
        // 冲刺 = 冲刺窗口内强制全速前进 + 提速(见 MountLogic.riddenInput / riddenSpeed);
        // 不能用一次性速度脉冲,因为骑乘 travel() 每 tick 会覆盖 deltaMovement
        mount.sfcraft$setDashTicks(dash.boostTicks);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    ravager.getX(), ravager.getY() + 0.3, ravager.getZ(), 12, 0.3, 0.1, 0.3, 0.1);
            serverLevel.playSound(null, ravager.getX(), ravager.getY(), ravager.getZ(),
                    SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL, 0.6F, 1.4F);
        }
        player.getCooldowns().addCooldown(held, dash.cooldownTicks);
        if (dash.durabilityCost > 0) held.hurtAndBreak(dash.durabilityCost, player, hand);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void celebrate(Mob mob) {
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART,
                    mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 7, 0.4, 0.4, 0.4, 0.1);
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.7F, 1.4F);
        }
    }

    private static void tameFailed(Mob mob) {
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 7, 0.3, 0.3, 0.3, 0.02);
            serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 0.8F, 1.0F);
        }
    }
}
