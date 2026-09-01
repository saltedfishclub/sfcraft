package io.ib67.sfcraft.network;

import io.ib67.sfcraft.config.SFConfig;
import io.ib67.sfcraft.mixin.common.bridge.ServerCommonNetworkHandlerBridge;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * 玩家在握手包里自报的服务器地址,也就是他实际用来进服的那个地址。
 * 由 {@code ServerHandshakeNetworkHandlerMixin} 记到 {@link net.minecraft.network.Connection} 上,
 * transfer 时原样回显,让玩家重连回原本的入口而不是配置里写死的那个。
 */
public record HandshakeAddress(String host, int port) {
    /**
     * 代理(BungeeCord/Velocity legacy forwarding、FML)会把转发数据拼在 hostName 后面,以 '\0' 分隔。
     */
    private static final char FORWARDING_SEPARATOR = '\0';

    /**
     * 挂在 {@link net.minecraft.network.Connection} 上的握手地址,由 ConnectionMixin 实现。
     */
    public interface Holder {
        @Nullable
        HandshakeAddress sfcraft$handshakeAddress();

        void sfcraft$setHandshakeAddress(@Nullable HandshakeAddress address);
    }

    /**
     * 清洗握手包里的 hostName / port,不可用时返回 null。
     */
    public static @Nullable HandshakeAddress sanitize(String hostName, int port) {
        if (hostName == null || port <= 0 || port > 65535) return null;
        var host = hostName;
        var separator = host.indexOf(FORWARDING_SEPARATOR);
        if (separator >= 0) host = host.substring(0, separator);
        // SRV 解析出来的 FQDN 可能带尾点
        while (host.endsWith(".")) host = host.substring(0, host.length() - 1);
        host = host.trim();
        return host.isEmpty() ? null : new HandshakeAddress(host, port);
    }

    /**
     * 读玩家当前连接上记的握手地址,拿不到返回 null。
     */
    public static @Nullable HandshakeAddress of(ServerPlayer player) {
        var connection = ((ServerCommonNetworkHandlerBridge) player.connection).sfcraft$getConnection();
        return connection instanceof Holder holder ? holder.sfcraft$handshakeAddress() : null;
    }

    /**
     * 优先把玩家送回他自己用的地址,拿不到才回退配置里的 domain / port。
     */
    public static ClientboundTransferPacket transferPacketFor(ServerPlayer player, SFConfig fallback) {
        var address = of(player);
        return address == null
                ? new ClientboundTransferPacket(fallback.domain, fallback.port)
                : new ClientboundTransferPacket(address.host(), address.port());
    }
}
