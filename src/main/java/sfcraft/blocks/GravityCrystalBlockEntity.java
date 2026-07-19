package sfcraft.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import sfcraft.GameConfig;
import sfcraft.SFBlockEntities;

public class GravityCrystalBlockEntity extends BlockEntity {
    // 静止判定阈值(纯实现细节);数值类参数见 GameConfig.gravityCrystal
    private static final double MIN_SPEED_SQ = 1.0e-4;
    private static final int PARTICLE_INTERVAL = 5;

    public GravityCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(SFBlockEntities.GRAVITY_CRYSTAL, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GravityCrystalBlockEntity crystal) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        int charge = state.getValue(GravityCrystalBlock.CHARGE);
        if (charge <= 0) return;
        long time = level.getGameTime();
        var config = GameConfig.get().gravityCrystal;
        if (time % Math.max(1, config.boostIntervalTicks) == 0) {
            boostEntities(serverLevel, pos, charge, config);
        }
        if (time % PARTICLE_INTERVAL == 0) {
            sendConvergingParticles(serverLevel, pos, config);
        }
    }

    private static void boostEntities(ServerLevel level, BlockPos pos, int charge, GameConfig.GravityCrystal config) {
        int index = Math.min(charge, config.boostPerCharge.length - 1);
        double factor = 1.0 + config.boostPerCharge[index];
        double maxSpeed = config.maxSpeedMetersPerSecond / 20.0;
        double maxSpeedSq = maxSpeed * maxSpeed;
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(config.range))) {
            var velocity = entity.getDeltaMovement();
            double speedSq = velocity.lengthSqr();
            if (speedSq < MIN_SPEED_SQ || speedSq >= maxSpeedSq) continue;
            var boosted = velocity.scale(factor);
            if (boosted.lengthSqr() > maxSpeedSq) {
                boosted = boosted.normalize().scale(maxSpeed);
            }
            entity.setDeltaMovement(boosted);
            entity.hurtMarked = true;
        }
    }

    private static void sendConvergingParticles(ServerLevel level, BlockPos pos, GameConfig.GravityCrystal config) {
        var center = Vec3.atCenterOf(pos);
        var random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 3.0 + random.nextDouble() * Math.max(0.5, config.range - 3.0);
            double x = center.x + Math.cos(angle) * distance;
            double y = center.y + (random.nextDouble() - 0.3) * 3.0;
            double z = center.z + Math.sin(angle) * distance;
            var direction = center.subtract(x, y, z).normalize();
            // count=0 时 dx/dy/dz 为粒子速度方向,粒子朝水晶本体飞行
            level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 0,
                    direction.x, direction.y, direction.z, 2.0);
        }
    }
}
