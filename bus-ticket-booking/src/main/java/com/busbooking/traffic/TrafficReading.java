package com.busbooking.traffic;

import java.util.List;

/**
 * One answer from a traffic provider.
 *
 * @param currentSeconds  driving time with traffic right now
 * @param typicalSeconds  driving time without traffic
 * @param distanceMeters  road distance, or null if unknown
 * @param encodedPolyline the road path in Google's encoded polyline format, or null
 * @param speedIntervals  traffic per stretch of the polyline (empty unless requested)
 */
public record TrafficReading(long currentSeconds, long typicalSeconds, Integer distanceMeters,
                             String encodedPolyline, List<SpeedInterval> speedIntervals) {
    public TrafficReading {
        speedIntervals = speedIntervals == null ? List.of() : List.copyOf(speedIntervals);
    }
}
