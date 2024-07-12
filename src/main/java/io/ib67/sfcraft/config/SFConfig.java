package io.ib67.sfcraft.config;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

public class SFConfig {
    public boolean enableOfflineExempt = true;
    public int maintainceStartHour = 13;
    public int maintainceEndHour = 17;
    public boolean disableTemporarily;

    public boolean isClosed(Clock clock) {
        if (disableTemporarily) return false;
        var hourOfDay = ZonedDateTime.now(clock).getHour();
        var startHr = maintainceStartHour;
        var endHr = maintainceEndHour;
        return startHr < hourOfDay && hourOfDay < endHr;
    }
}
