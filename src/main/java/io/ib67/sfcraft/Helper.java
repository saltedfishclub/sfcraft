package io.ib67.sfcraft;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import net.minecraft.server.network.ServerPlayerEntity;

import java.net.InetSocketAddress;

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
        try {
            var clock = SFCraft.getInstance().getGeoIPService().clockOf(address.getAddress());
            var config = SFCraft.getInstance().getConfig();
            if (config.isClosed(clock)) {
                return "防沉迷: " + config.maintainceStartHour + "~" + config.maintainceEndHour;
            }
        } catch (GeoIp2Exception ignored) {
            return "Not Available!";
        }
        return null;
    }
}
