package com.busbooking.traffic;

/** Used when live traffic is turned off (no Google key and no simulation). */
public class DisabledTrafficProvider implements TrafficProvider {

    @Override
    public String source() {
        return "OFF";
    }

    @Override
    public boolean billed() {
        return false;
    }

    @Override
    public TrafficReading read(RoutePoints route) {
        throw new TrafficLookupException("Live traffic is not set up on this server");
    }
}
