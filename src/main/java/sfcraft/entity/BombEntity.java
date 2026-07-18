package sfcraft.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import sfcraft.SFEntities;
import sfcraft.SFItems;

public class BombEntity extends ThrowableItemProjectile implements PolymerEntity {
    private static final int NORMAL_FUSE_TICKS = 20;
    private static final int OBSIDIAN_FUSE_TICKS = 80;
    private static final float EXPLOSION_POWER = 2.5F;
    private static final double SLOW_CIRCLE_RADIUS = 1.5;
    private static final int SLOW_CIRCLE_POINTS = 16;

    private BombType type = BombType.NORMAL;
    private boolean landed;
    private int landedTicks;

    public BombEntity(EntityType<? extends BombEntity> entityType, Level level) {
        super(entityType, level);
    }

    public BombEntity(LivingEntity shooter, Level level, ItemStack stack) {
        super(SFEntities.BOMB, shooter, level, stack);
    }

    public void setBombType(BombType type) {
        this.type = type;
    }

    @Override
    protected Item getDefaultItem() {
        return SFItems.BOMB;
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext context) {
        return EntityTypes.SNOWBALL;
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity entity) {
        // 炸弹只在碰到方块时触发,直接穿过实体
        return false;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level().isClientSide() || landed) return;
        land(result.getLocation().add(Vec3.atLowerCornerOf(result.getDirection().getUnitVec3i()).scale(0.05)));
    }

    private void land(Vec3 restPos) {
        landed = true;
        landedTicks = 0;
        setDeltaMovement(Vec3.ZERO);
        setNoGravity(true);
        setPos(restPos);
        if (type == BombType.OBSIDIAN) {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    @Override
    public void tick() {
        if (!landed) {
            super.tick();
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;
        landedTicks++;
        if (type == BombType.OBSIDIAN) {
            if (landedTicks % 5 == 0) drawSlowCircle(serverLevel);
            if (landedTicks % 10 == 0) applySlowness(serverLevel);
            if (landedTicks >= OBSIDIAN_FUSE_TICKS) explode();
        } else if (landedTicks >= NORMAL_FUSE_TICKS) {
            explode();
        }
    }

    private void drawSlowCircle(ServerLevel serverLevel) {
        for (int i = 0; i < SLOW_CIRCLE_POINTS; i++) {
            double angle = (Math.PI * 2 / SLOW_CIRCLE_POINTS) * i;
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + Math.cos(angle) * SLOW_CIRCLE_RADIUS,
                    getY() + 0.1,
                    getZ() + Math.sin(angle) * SLOW_CIRCLE_RADIUS,
                    1, 0, 0, 0, 0);
        }
    }

    private void applySlowness(ServerLevel serverLevel) {
        var box = getBoundingBox().inflate(SLOW_CIRCLE_RADIUS, 1.0, SLOW_CIRCLE_RADIUS);
        for (var entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box)) {
            double dx = entity.getX() - getX();
            double dz = entity.getZ() - getZ();
            if (dx * dx + dz * dz > SLOW_CIRCLE_RADIUS * SLOW_CIRCLE_RADIUS) continue;
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
        }
    }

    private void explode() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        serverLevel.explode(this, getX(), getY(), getZ(), EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
        if (type == BombType.BLAZE) {
            // 灰色浓烟为主、少量橙色火花,从爆炸中心向外爆发
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 40, 0, 0, 0, 0.25);
            serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 12, 0, 0, 0, 0.3);
            for (var entity : serverLevel.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.0))) {
                entity.igniteForTicks(100);
            }
            spreadFire(serverLevel);
        }
        discard();
    }

    private void spreadFire(ServerLevel serverLevel) {
        var random = getRandom();
        int target = 2 + random.nextInt(2);
        int placed = 0;
        for (int attempt = 0; attempt < 12 && placed < target; attempt++) {
            var pos = blockPosition().offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);
            var fireState = BaseFireBlock.getState(serverLevel, pos);
            if (serverLevel.getBlockState(pos).isAir() && fireState.canSurvive(serverLevel, pos)) {
                serverLevel.setBlockAndUpdate(pos, fireState);
                placed++;
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("bomb_type", type.name());
        out.putBoolean("landed", landed);
        out.putInt("landed_ticks", landedTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        type = BombType.byName(in.getStringOr("bomb_type", BombType.NORMAL.name()));
        landed = in.getBooleanOr("landed", false);
        landedTicks = in.getIntOr("landed_ticks", 0);
        if (landed) setNoGravity(true);
    }

    public enum BombType {
        NORMAL, OBSIDIAN, BLAZE;

        static BombType byName(String name) {
            for (var value : values()) {
                if (value.name().equals(name)) return value;
            }
            return NORMAL;
        }
    }
}
