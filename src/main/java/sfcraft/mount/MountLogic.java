package sfcraft.mount;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import sfcraft.GameConfig;

import java.util.Set;
import java.util.UUID;

/**
 * 驯服坐骑的共享逻辑,供 RavagerMountMixin / HoglinMountMixin 与 MountModule 复用。
 */
public final class MountLogic {
    private MountLogic() {
    }

    // ---- 骑乘移动(mixin 覆写 LivingEntity 骑乘方法后委托到此) ----

    public static LivingEntity controllingPassenger(Mob mob, MountAccess mount) {
        if (mount.sfcraft$getOwner() == null) return null;
        var config = GameConfig.get().mount;
        if (config.requireSaddleToRide && !mount.sfcraft$isSaddled()) return null;
        if (mob.getFirstPassenger() instanceof Player player
                && player.getUUID().equals(mount.sfcraft$getOwner())) {
            return player;
        }
        return null;
    }

    public static Vec3 riddenInput(MountAccess mount, Player player) {
        // 纯服务端坐骑:玩家 xxa/zza 在服务端恒为 0,须从输入包(getLastClientInput)读转向
        float strafe = 0.0F;
        float forward = 0.0F;
        if (player instanceof ServerPlayer serverPlayer) {
            Input input = serverPlayer.getLastClientInput();
            strafe = (input.left() ? 1.0F : 0.0F) - (input.right() ? 1.0F : 0.0F);
            forward = (input.forward() ? 1.0F : 0.0F) - (input.backward() ? 1.0F : 0.0F);
        }
        if (mount.sfcraft$getDashTicks() > 0) forward = 1.0F; // 冲刺:强制全速前进
        strafe *= 0.5F;
        if (forward <= 0.0F) forward *= 0.25F;
        return new Vec3(strafe, 0.0, forward);
    }

    public static float riddenSpeed(Mob mob, MountAccess mount) {
        var config = GameConfig.get().mount;
        double speed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED) * config.rideSpeedFactor;
        if (mount.sfcraft$getDashTicks() > 0) speed *= config.ravagerDash.speedMultiplier;
        return (float) speed;
    }

    public static void tickRidden(Mob mob, MountAccess mount, Player player) {
        // 朝向对齐骑手视线
        mob.setYRot(player.getYRot());
        mob.yRotO = mob.getYRot();
        mob.setXRot(player.getXRot() * 0.5F);
        mob.setYBodyRot(mob.getYRot());
        mob.setYHeadRot(mob.getYRot());
        if (mount.sfcraft$getDashTicks() > 0) {
            mount.sfcraft$setDashTicks(mount.sfcraft$getDashTicks() - 1);
        }
        mob.setTarget(null);
    }

    /** 每 tick 的 AI 抑制:骑乘时清目标;已驯服则不再以任何玩家为目标(不止是主人)。 */
    public static void tickAiSuppression(Mob mob, MountAccess mount) {
        if (mob.getControllingPassenger() instanceof Player) {
            mob.setTarget(null);
            return;
        }
        if (mount.sfcraft$getOwner() != null && mob.getTarget() instanceof Player) {
            mob.setTarget(null);
        }
    }

    /** 是否应拦截这次攻击:骑乘中一律拦截;已驯服则永不攻击任何玩家。 */
    public static boolean blocksAttack(Mob mob, MountAccess mount, Entity target) {
        if (mob.hasControllingPassenger()) return true;
        return mount.sfcraft$getOwner() != null && target instanceof Player;
    }

    /** 劫掠兽冲刺期间撞到的生物受到一次性撞击伤害,同一次冲刺内每个生物只命中一次。 */
    public static void tickRavagerDashCollision(Ravager ravager, MountAccess mount, Set<UUID> hitThisDash) {
        if (mount.sfcraft$getDashTicks() <= 0) {
            hitThisDash.clear();
            return;
        }
        if (!(ravager.level() instanceof ServerLevel serverLevel)) return;
        double damage = GameConfig.get().mount.ravagerDash.collisionDamage;
        var box = ravager.getBoundingBox().inflate(0.2);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != ravager && !ravager.hasPassenger(e))) {
            if (!hitThisDash.add(victim.getUUID())) continue;
            victim.hurtServer(serverLevel, ravager.damageSources().mobAttack(ravager), (float) damage);
        }
    }

    // ---- 持久化 ----

    public static void save(ValueOutput out, MountAccess mount) {
        if (mount.sfcraft$getOwner() != null) {
            out.putString("sfcraft_owner", mount.sfcraft$getOwner().toString());
        }
        out.putBoolean("sfcraft_saddled", mount.sfcraft$isSaddled());
    }

    public static void load(ValueInput in, MountAccess mount) {
        String owner = in.getStringOr("sfcraft_owner", "");
        mount.sfcraft$setOwner(owner.isEmpty() ? null : parseUuid(owner));
        mount.sfcraft$setSaddled(in.getBooleanOr("sfcraft_saddled", false));
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---- 驯服前置条件 ----

    /** 返回 null 表示可驯服,否则返回失败原因(用于反馈)。 */
    public static Component tameBlockReason(Mob mob) {
        if (!mob.hasEffect(MobEffects.WEAKNESS)) {
            return Component.literal("它必须处于虚弱状态才能被驯服");
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return Component.literal("无法在此驯服");
        }
        if (level.getRaidAt(mob.blockPosition()) != null) {
            return Component.literal("袭击进行中,无法驯服");
        }
        if (mob instanceof Raider raider && raider.hasActiveRaid()) {
            return Component.literal("袭击进行中,无法驯服");
        }
        var box = mob.getBoundingBox().inflate(GameConfig.get().mount.tameNearbyRaiderRadius);
        for (Raider other : level.getEntitiesOfClass(Raider.class, box)) {
            if (other != mob) return Component.literal("附近有掠夺者时无法驯服");
        }
        return null;
    }

    public static Item resolveItem(String id, Item fallback) {
        try {
            return BuiltInRegistries.ITEM.get(Identifier.parse(id))
                    .map(Holder.Reference::value).orElse(fallback);
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
