package io.ib67.sfcraft.geoip;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import io.ib67.sfcraft.inject.ConfigRoot;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.time.Clock;
import java.util.TimeZone;

import static java.net.http.HttpResponse.BodyHandlers.ofFile;

@Log4j2
@Singleton
public class MaxMindGeoIPService implements Module, GeoIPService {
    private static final URI GEOIP_DOWNLOAD_URL = URI.create("https://sfclub.cc/GeoLite2-City.mmdb");
    private Path mmdbPath;
    private final DatabaseReader databaseReader;

    @Inject
    public MaxMindGeoIPService(@ConfigRoot Path configRoot) throws InterruptedException {
        mmdbPath = configRoot.resolve("GeoLite2-City.mmdb");
        databaseReader = loadData();
    }

    @Override
    @SneakyThrows
    public Clock clockOf(InetAddress address) throws GeoIp2Exception {
        return Clock.system(TimeZone.getTimeZone(databaseReader.city(address).getLocation().getTimeZone()).toZoneId());
    }

    @Override
    @SneakyThrows
    public City cityOf(InetAddress address) throws GeoIp2Exception {
        return databaseReader.city(address).getCity();
    }

    @Override
    @SneakyThrows
    public Country countryOf(InetAddress address) throws GeoIp2Exception {
        return databaseReader.country(address).getCountry();
    }

    @SneakyThrows
    private DatabaseReader loadData() throws InterruptedException {
        try {
            return new DatabaseReader.Builder(mmdbPath.toFile()).withCache(new CHMCache()).build();
        } catch (IOException ignored) {
            return updateData();
        }
    }

    private DatabaseReader updateData() throws InterruptedException {
        var client = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder(GEOIP_DOWNLOAD_URL).GET().build();
        int retries = 0;
        do {
            try {
                log.info("Downloading GeoIP database, please wait....");
                client.send(req, ofFile(mmdbPath));
                return new DatabaseReader.Builder(mmdbPath.toFile()).build();
            } catch (IOException | InterruptedException e) {
                log.error("Failed to download or the database is corrupted!", e);
                int sleep = (int) Math.pow(2, ++retries);
                log.info("Retrying after " + sleep + "sec.");
                Thread.sleep(sleep);
            } finally {
                client.close();
            }
        } while (retries < 4);
        throw new InterruptedException("Cannot load GeoIP data.");
    }

    @Override
    public void close() throws IOException {
        databaseReader.close();
    }

    @Override
    public void configure(Binder binder) {

    }
}
