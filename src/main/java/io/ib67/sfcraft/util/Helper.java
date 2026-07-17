package io.ib67.sfcraft.util;

import com.github.houbb.pinyin.api.impl.Pinyin;
import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum;
import com.github.houbb.pinyin.util.PinyinHelper;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.geoip.GeoIPService;
import lombok.SneakyThrows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.material.EmptyFluid;
import net.minecraft.world.level.material.WaterFluid;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class Helper {
    private static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[./\\\\#%!@&*]");
    private static final Pattern NON_ASCII = Pattern.compile("[^a-zA-Z0-9_+()]");
    public static final char COLOR = '§';

    public static boolean canBack(ServerPlayer player) {
        if (!SFConsts.COMMAND_BACK.hasPermission(player)) return false;
        var pos = player.getLastDeathLocation().get();
        var wld = player.level().getServer().getLevel(pos.dimension());
        var _pos = pos.pos();
        if (wld == null) return false;
        var nearby = wld.getNearestPlayer(_pos.getX(), _pos.getY(), _pos.getZ(), 100, true);
        return nearby != null;
    }

    public static String cleanFileName(String filename) {
        var name = PinyinHelper.toPinyin(filename, PinyinStyleEnum.NORMAL, "_");
        name = ILLEGAL_CHARACTERS.matcher(name).replaceAll("");
        if (name.length() > 32) {
            name = name.substring(0, 32);
        }
        return NON_ASCII.matcher(name).replaceAll("");
    }

    public static boolean teleportSafely(ServerPlayer player, ServerLevel world, int x, int y, int z, float yaw, float pitch) {
        if (player.gameMode() == GameType.CREATIVE) return true;
        var pos = new BlockPos(x, y, z);
        var stand = world.getBlockState(pos);
        if (!stand.isAir() && !stand.isRedstoneConductor(world, pos)) {
            // check block type
            var fluid = stand.getFluidState().getType();
            if (!(fluid instanceof EmptyFluid) && !(fluid instanceof WaterFluid)) {
                player.sendSystemMessage(Component.literal("§c传送目的地具有非水流体, 因此拒绝传送。"));
                return false;
            }
        }
        var groundPos = new BlockPos(x, y - 1, z);
        var ground = world.getBlockState(groundPos);
        if (!ground.entityCanStandOn(world, groundPos, player)
                && !stand.entityCanStandOnFace(world, pos, player, Direction.DOWN)) {
            player.sendSystemMessage(Component.literal("§c传送目的地没有可靠落地点，且为非空气方块, 因此拒绝传送。"));
            return false;
        } else {
            var deltaY = 0.0;
            var deltaX = 0.5;
            var deltaZ = 0.5;
            if (!stand.isAir()) {
                deltaY = stand.getCollisionShape(world, pos).max(Direction.Axis.Y);
                deltaY = Double.isFinite(deltaY) ? deltaY + 0.1 : 0;
                deltaX = stand.getCollisionShape(world, pos).max(Direction.Axis.X);
                deltaX = Double.isFinite(deltaX) ? deltaX / 2 : 0.5;
                deltaZ = stand.getCollisionShape(world, pos).max(Direction.Axis.X);
                deltaZ = Double.isFinite(deltaZ) ? deltaZ / 2 : 0.5;
            }
            player.teleportTo(world, x + deltaX, y + deltaY, z + deltaZ, Set.of(), yaw, pitch, true);
        }
        return true;
    }

    @SneakyThrows
    public static Optional<String> getConfigResource(Path root, String resource) {
        var r = root.resolve(resource);
        if (Files.exists(r)) {
            return Optional.of(Files.readString(r));
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

    /**
     * Plays a sound only to the given player, at their own position.
     * Reproduces the pre-26.2 {@code ServerPlayer#playNotifySound}.
     */
    public static void playNotifySound(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                source,
                player.getX(), player.getY(), player.getZ(),
                volume, pitch,
                player.getRandom().nextLong()
        ));
    }

    public static String hideIp(InetAddress addr) {
        var _addr = addr.getHostAddress();
        var q = _addr.split("\\.");
        if (q.length == 0) {
            // ipv6 is not supported
            return _addr;
        }
        return q[0] + "." + q[1] + ".*.*";
    }
}
