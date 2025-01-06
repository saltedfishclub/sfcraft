package io.ib67.sfcraft.config;

import org.apache.commons.lang3.RandomStringUtils;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SFConfig {
    public boolean enableOfflineExempt = true;
    public String domain = "localhost";
    public String serverSecret = RandomStringUtils.random(32);
    public int httpPort = 8080;
    public long maxSchematicSize = 100000;
}
