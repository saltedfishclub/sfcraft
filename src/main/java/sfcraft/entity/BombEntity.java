package sfcraft.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
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
import sfcraft.GameConfig;
import sfcraft.SFEntities;
import sfcraft.SFItems;

public class BombEntity extends ThrowableItemProjectile implements PolymerEntity {
    // 纯视觉的粒子密度;数值类参数见 GameConfig.bomb
    private static final int SLOW_CIRCLE_POINTS = 16;

    private BombType type = BombType.NORMAL;
    private boolean landed;
    private int fuseTicks;
    // 落地后烟雾喷射方向:落地面朝外法线(默认向上)
    private Direction landNormal = Direction.UP;

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
        landNormal = result.getDirection();
        land(result.getLocation().add(Vec3.atLowerCornerOf(result.getDirection().getUnitVec3i()).scale(0.05)));
    }

    private void land(Vec3 restPos) {
        landed = true;
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
        }
        if (!(level() instanceof ServerLevel serverLevel)) return;
        fuseTicks++;
        emitSmoke(serverLevel);
        if (landed && type == BombType.OBSIDIAN) {
            if (fuseTicks % 5 == 0) drawSlowCircle(serverLevel);
            if (fuseTicks % 10 == 0) applySlowness(serverLevel);
        }
        if (fuseTicks >= GameConfig.get().bomb.fuseTicks) explode();
    }

    private void emitSmoke(ServerLevel serverLevel) {
        var config = GameConfig.get().bomb;
        // 飞行时朝速度反方向;落地后朝落地面朝外法线("从表面冒出")
        Vec3 direction;
        if (landed) {
            direction = Vec3.atLowerCornerOf(landNormal.getUnitVec3i());
        } else {
            var velocity = getDeltaMovement();
            if (velocity.lengthSqr() < 1.0e-6) return;
            direction = velocity.normalize().scale(-1);
        }
        // 黑色粉尘沿方向单侧铺开形成拖尾/升腾烟柱(dust 不吃粒子速度,故靠位置偏移造型)
        var dust = new DustParticleOptions(config.smokeColor, config.smokeParticleScale);
        var random = getRandom();
        for (int i = 0; i < config.smokeParticlesPerTick; i++) {
            double t = random.nextDouble() * 0.6;
            double jitter = 0.08;
            double px = getX() + direction.x * t + (random.nextDouble() - 0.5) * jitter;
            double py = getY() + direction.y * t + (random.nextDouble() - 0.5) * jitter;
            double pz = getZ() + direction.z * t + (random.nextDouble() - 0.5) * jitter;
            serverLevel.sendParticles(dust, px, py, pz, 1, 0.0, 0.0, 0.0, config.smokeSpeed);
        }
    }

    private void drawSlowCircle(ServerLevel serverLevel) {
        double radius = GameConfig.get().bomb.slowCircleRadius;
        for (int i = 0; i < SLOW_CIRCLE_POINTS; i++) {
            double angle = (Math.PI * 2 / SLOW_CIRCLE_POINTS) * i;
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    getX() + Math.cos(angle) * radius,
                    getY() + 0.1,
                    getZ() + Math.sin(angle) * radius,
                    1, 0, 0, 0, 0);
        }
    }

    private void applySlowness(ServerLevel serverLevel) {
        var config = GameConfig.get().bomb;
        double radius = config.slowCircleRadius;
        var box = getBoundingBox().inflate(radius, 1.0, radius);
        for (var entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box)) {
            double dx = entity.getX() - getX();
            double dz = entity.getZ() - getZ();
            if (dx * dx + dz * dz > radius * radius) continue;
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, config.slownessAmplifier));
        }
    }

    private void explode() {
        if (!(level() instanceof ServerLevel serverLevel)) return;
        var config = GameConfig.get().bomb;
        serverLevel.explode(this, getX(), getY(), getZ(), config.explosionPower, Level.ExplosionInteraction.NONE);
        if (type == BombType.BLAZE) {
            // 灰色浓烟为主、少量橙色火花,从爆炸中心向外爆发
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 40, 0, 0, 0, 0.25);
            serverLevel.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 12, 0, 0, 0, 0.3);
            for (var entity : serverLevel.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(config.blazeIgniteRadius))) {
                entity.igniteForTicks(config.blazeIgniteTicks);
            }
            spreadFire(serverLevel);
        }
        discard();
    }

    private void spreadFire(ServerLevel serverLevel) {
        var config = GameConfig.get().bomb;
        var random = getRandom();
        int target = config.blazeFireSpreadMin
                + random.nextInt(Math.max(1, config.blazeFireSpreadMax - config.blazeFireSpreadMin + 1));
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
        out.putInt("fuse_ticks", fuseTicks);
        out.putInt("land_normal", landNormal.get3DDataValue());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        type = BombType.byName(in.getStringOr("bomb_type", BombType.NORMAL.name()));
        landed = in.getBooleanOr("landed", false);
        fuseTicks = in.getIntOr("fuse_ticks", 0);
        landNormal = Direction.from3DDataValue(in.getIntOr("land_normal", Direction.UP.get3DDataValue()));
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
