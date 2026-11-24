package com.busbooking.traffic;

/** How much slower than an empty road the route is right now. */
public enum TrafficLevel {
    /** Less than 10 % slower. */
    LIGHT,
    /** 10 to 30 % slower. */
    MODERATE,
    /** 30 % or more slower. */
    HEAVY;

    public static TrafficLevel of(long currentSeconds, long typicalSeconds) {
        if (typicalSeconds <= 0) return LIGHT;
        double ratio = (double) currentSeconds / typicalSeconds;
        if (ratio < 1.10) return LIGHT;
        if (ratio < 1.30) return MODERATE;
        return HEAVY;
    }
}
