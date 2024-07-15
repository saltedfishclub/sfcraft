package io.ib67.sfcraft.geoip;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import lombok.SneakyThrows;

import java.io.Closeable;
import java.net.InetAddress;
import java.time.Clock;

public interface GeoIPService extends Closeable {
    Clock clockOf(InetAddress address) throws GeoIp2Exception;

    City cityOf(InetAddress address) throws GeoIp2Exception;

    Country countryOf(InetAddress address) throws GeoIp2Exception;
}
