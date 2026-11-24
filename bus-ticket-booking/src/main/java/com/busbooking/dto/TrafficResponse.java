package com.busbooking.dto;

import com.busbooking.traffic.SpeedInterval;
import com.busbooking.traffic.TrafficLevel;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Traffic on a route right now.
 *
 * @param source           GOOGLE, or SIMULATED when the server runs without a Google key
 * @param scheduledMinutes travel time in the timetable
 * @param typicalMinutes   driving time on an empty road
 * @param currentMinutes   driving time with traffic now
 * @param delayMinutes     currentMinutes minus typicalMinutes (never negative)
 * @param encodedPolyline  road path in Google's encoded polyline format, for drawing on a map; null when unknown
 * @param speedIntervals   traffic per stretch of the polyline (empty unless enabled on the server)
 * @param checkedAt        when this was read (Vietnam time)
 * @param stale            true when fresh data could not be had and an older reading is shown; see note
 */
public record TrafficResponse(
        Long routeId,
        String origin,
        String destination,
        String originStation,
        String destinationStation,
        String source,
        int scheduledMinutes,
        int typicalMinutes,
        int currentMinutes,
        int delayMinutes,
        TrafficLevel level,
        Double distanceKm,
        String encodedPolyline,
        List<SpeedInterval> speedIntervals,
        LocalDateTime checkedAt,
        boolean stale,
        String note
) {}
