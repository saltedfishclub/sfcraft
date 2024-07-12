package io.ib67.sfcraft.util;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import io.ib67.sfcraft.SFCraft;
import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.geoip.GeoIPService;
import lombok.SneakyThrows;
import net.minecraft.server.network.ServerPlayerEntity;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class Helper {
    public static boolean canBack(ServerPlayerEntity player) {
        if (!SFConsts.COMMAND_BACK.hasPermission(player)) return false;
        var pos = player.getLastDeathPos().get();
        var wld = player.server.getWorld(pos.dimension());
        var _pos = pos.pos();
        if (wld == null) return false;
        var nearby = wld.getClosestPlayer(_pos.getX(), _pos.getY(), _pos.getZ(), 20, true);
        return nearby != null;
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
}
