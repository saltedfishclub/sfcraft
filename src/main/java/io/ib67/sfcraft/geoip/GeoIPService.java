package io.ib67.sfcraft.geoip;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import lombok.SneakyThrows;

import java.io.Closeable;
import java.net.InetAddress;
import java.time.Clock;

public interface GeoIPService extends Closeable {
    Clock clockOf(InetAddress address) throws GeoIp2Exception;
}
