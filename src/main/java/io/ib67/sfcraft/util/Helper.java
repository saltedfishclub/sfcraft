package io.ib67.sfcraft.util;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.geoip.GeoIPService;
import lombok.SneakyThrows;
import net.minecraft.block.Block;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.SaplingBlock;
import net.minecraft.fluid.EmptyFluid;
import net.minecraft.fluid.LavaFluid;
import net.minecraft.fluid.WaterFluid;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class Helper {
    public static final char COLOR = '§';

    public static boolean canBack(ServerPlayerEntity player) {
        if (!SFConsts.COMMAND_BACK.hasPermission(player)) return false;
        var pos = player.getLastDeathPos().get();
        var wld = player.server.getWorld(pos.dimension());
        var _pos = pos.pos();
        if (wld == null) return false;
        var nearby = wld.getClosestPlayer(_pos.getX(), _pos.getY(), _pos.getZ(), 20, true);
        return nearby != null;
    }

    public static boolean teleportSafely(ServerPlayerEntity player, ServerWorld world, int x, int y, int z, float yaw, float pitch) {
        var pos = new BlockPos(x, y, z);
        var stand = world.getBlockState(pos);
        if (!stand.isAir() && !stand.isSolidBlock(world, pos)) {
            // check block type
            var fluid = stand.getFluidState().getFluid();
            if (!(fluid instanceof EmptyFluid) && !(fluid instanceof WaterFluid)) {
                player.sendMessage(Text.literal("§c传送目的地具有非水流体, 因此拒绝传送。"));
                return false;
            }
        }
        var groundPos = new BlockPos(x, y - 1, z);
        var ground = world.getBlockState(groundPos);
        if (!ground.hasSolidTopSurface(world, groundPos, player)
                && !stand.isSolidSurface(world, pos, player, Direction.DOWN)) {
            player.sendMessage(Text.literal("§c传送目的地没有可靠落地点，且为非空气方块, 因此拒绝传送。"));
            return false;
        } else {
            var deltaY = 0.0;
            var deltaX = 0.5;
            var deltaZ = 0.5;
            if (!stand.isAir()) {
                deltaY = stand.getCollisionShape(world, pos).getMax(Direction.Axis.Y);
                deltaY = Double.isFinite(deltaY) ? deltaY + 0.1 : 0;
                deltaX = stand.getCollisionShape(world, pos).getMax(Direction.Axis.X);
                deltaX = Double.isFinite(deltaX) ? deltaX / 2 : 0.5;
                deltaZ = stand.getCollisionShape(world, pos).getMax(Direction.Axis.X);
                deltaZ = Double.isFinite(deltaZ) ? deltaZ / 2 : 0.5;
            }
            player.teleport(world, x + deltaX, y + deltaY, z + deltaZ, yaw, pitch);
        }
        return true;
    }

    public static String getVersionString(InetSocketAddress address) {
        var geoIp = SFCraft.getInjector().getInstance(GeoIPService.class);
        var config = SFCraft.getInjector().getInstance(SFConfig.class);
        try {
            var clock = geoIp.clockOf(address.getAddress());
            if (config.isClosed(clock)) {
                return "防沉迷: " + config.maintainceStartHour + "~" + config.maintainceEndHour;
            }
        } catch (GeoIp2Exception ignored) {
            return "Not Available!";
        }
        return null;
    }

    @SneakyThrows
    public static Optional<String> getConfigResource(Path root, String resource) {
        if (Files.exists(root)) {
            return Optional.of(Files.readString(root.resolve(resource)));
        }
        return Optional.empty();
    }

    public static int fromRgb(int r, int g, int b) {
        return ((r & 0x0ff) << 16) | ((g & 0x0ff) << 8) | (b & 0x0ff);
    }

    public static boolean canFertilize(Block block) {
        return block instanceof SaplingBlock
                || block instanceof CropBlock;
    }
}