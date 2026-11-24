package com.busbooking.traffic;

/** Something that can tell how long a route takes to drive right now. */
public interface TrafficProvider {

    /** Shown to clients as {@code source}, e.g. GOOGLE. */
    String source();

    /** True when each call costs money, so it counts against {@code app.traffic.daily-limit}. */
    boolean billed();

    /** @throws TrafficLookupException when no answer can be had (bad key, network, no road route…) */
    TrafficReading read(RoutePoints route);
}
