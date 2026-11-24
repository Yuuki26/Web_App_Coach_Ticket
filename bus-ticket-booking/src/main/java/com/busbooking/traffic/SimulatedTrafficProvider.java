package com.busbooking.traffic;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Made-up traffic for development and demos without a Google key: the route's timetable duration,
 * slowed down at Saigon rush hours, with a small variation that changes every 10 minutes.
 * Responses say {@code "source": "SIMULATED"} so nobody mistakes them for real data.
 */
public class SimulatedTrafficProvider implements TrafficProvider {

    private final Clock clock;

    public SimulatedTrafficProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String source() {
        return "SIMULATED";
    }

    @Override
    public boolean billed() {
        return false;
    }

    @Override
    public TrafficReading read(RoutePoints route) {
        LocalDateTime now = LocalDateTime.now(clock);
        long typical = route.scheduledMinutes() * 60L;
        int slot = now.getHour() * 6 + now.getMinute() / 10;
        double wobble = Math.floorMod(31 * route.routeId() + 17L * slot, 11) / 100.0; // 0 to 10 %
        double factor = rushHour(now.getHour()) + wobble;
        return new TrafficReading(Math.round(typical * factor), typical, route.distanceKm() * 1000, null, null);
    }

    /** Morning and evening peaks are the slowest. */
    static double rushHour(int hour) {
        if (hour >= 6 && hour < 9) return 1.30;
        if (hour >= 16 && hour < 19) return 1.40;
        if (hour >= 11 && hour < 13) return 1.10;
        if (hour >= 22 || hour < 5) return 1.00;
        return 1.05;
    }
}
