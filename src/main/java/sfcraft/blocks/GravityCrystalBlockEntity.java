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
import sfcraft.SFBlockEntities;

public class GravityCrystalBlockEntity extends BlockEntity {
    // 调参项:作用半径 / 施加间隔 / 各等级加速比例 / 速度阈值与上限
    private static final double RANGE = 8.0;
    private static final int BOOST_INTERVAL = 2;
    private static final double[] BOOST_PER_CHARGE = {0.0, 0.15, 0.20, 0.25, 0.30};
    private static final double MIN_SPEED_SQ = 1.0e-4;
    // 最大速度上限 8m/s = 0.4 格/tick;已超过上限的实体不再加速
    private static final double MAX_SPEED = 8.0 / 20.0;
    private static final double MAX_SPEED_SQ = MAX_SPEED * MAX_SPEED;
    private static final int PARTICLE_INTERVAL = 5;

    public GravityCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(SFBlockEntities.GRAVITY_CRYSTAL, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GravityCrystalBlockEntity crystal) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        int charge = state.getValue(GravityCrystalBlock.CHARGE);
        if (charge <= 0) return;
        long time = level.getGameTime();
        if (time % BOOST_INTERVAL == 0) {
            boostEntities(serverLevel, pos, charge);
        }
        if (time % PARTICLE_INTERVAL == 0) {
            sendConvergingParticles(serverLevel, pos);
        }
    }

    private static void boostEntities(ServerLevel level, BlockPos pos, int charge) {
        double factor = 1.0 + BOOST_PER_CHARGE[charge];
        for (var entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(RANGE))) {
            var velocity = entity.getDeltaMovement();
            double speedSq = velocity.lengthSqr();
            if (speedSq < MIN_SPEED_SQ || speedSq >= MAX_SPEED_SQ) continue;
            var boosted = velocity.scale(factor);
            if (boosted.lengthSqr() > MAX_SPEED_SQ) {
                boosted = boosted.normalize().scale(MAX_SPEED);
            }
            entity.setDeltaMovement(boosted);
            entity.hurtMarked = true;
        }
    }

    private static void sendConvergingParticles(ServerLevel level, BlockPos pos) {
        var center = Vec3.atCenterOf(pos);
        var random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 3.0 + random.nextDouble() * (RANGE - 3.0);
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
