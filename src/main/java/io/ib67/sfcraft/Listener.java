package io.ib67.sfcraft;

import com.maxmind.geoip2.record.City;
import lombok.SneakyThrows;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.text.Text;

import java.net.InetSocketAddress;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

public class Listener {
    private final SFCraft core;

    public Listener(SFCraft core) {
        this.core = core;
    }

    @SneakyThrows
    public boolean onPlayerLogin(String currentPlayer, ClientConnection connection, Consumer<Text> disconnect) {
        if (connection.getAddress() instanceof InetSocketAddress address) {
            var clock = core.getGeoIPService().clockOf(address.getAddress());
            var hourOfDay = ZonedDateTime.now(clock).getHour();
            var startHr = SFCraft.getInstance().getConfig().maintainceStartHour;
            var endHr = SFCraft.getInstance().getConfig().maintainceEndHour;
            if (startHr > hourOfDay && hourOfDay > endHr) {
                final var msg = "服务器尚未开放\n服务器维护时间： %1$d~%2$d\n你的下午不值得浪费在游戏上，晚点再来吧！";
                disconnect.accept(Text.of(String.format(msg, startHr, endHr)));
                return false;
            }
            var canJoin = core.getAccessController().checkAccess(currentPlayer, address.getAddress().getHostAddress());
            if (!canJoin) {
                final var msg = "";
                disconnect.accept(Text.of(String.format(msg, startHr, endHr)));
            }
        }
        return true;
    }
}
