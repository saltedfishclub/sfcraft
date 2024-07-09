package io.ib67.sfcraft.geoip;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.record.Country;
import io.ib67.sfcraft.SFCraft;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

import static java.net.http.HttpResponse.BodyHandlers.ofFile;

@Log4j2
public class GeoIPService implements Closeable {
    private static final URI GEOIP_DOWNLOAD_URL = URI.create("http://sfclub.cc/GeoLite2-City.mmdb");
    private static final Path COUNTRY_MMDB_PATH = SFCraft.getInstance().getRoot().resolve("GeoLite2-City.mmdb");
    private final DatabaseReader databaseReader;

    public GeoIPService() throws InterruptedException {
        databaseReader = loadData();
    }

    @SneakyThrows
    public Clock clockOf(InetAddress address) throws GeoIp2Exception {
        return Clock.system(TimeZone.getTimeZone(databaseReader.city(address).getLocation().getTimeZone()).toZoneId());
    }

    private DatabaseReader loadData() throws InterruptedException {
        try {
            return new DatabaseReader.Builder(COUNTRY_MMDB_PATH.toFile()).withCache(new CHMCache()).build();
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
                client.send(req, ofFile(COUNTRY_MMDB_PATH));
                return new DatabaseReader.Builder(COUNTRY_MMDB_PATH.toFile()).build();
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
}
