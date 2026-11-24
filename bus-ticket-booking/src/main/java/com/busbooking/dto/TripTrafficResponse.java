package com.busbooking.dto;

import java.time.LocalDateTime;

/**
 * A trip's timetable next to live traffic on its route.
 *
 * @param estimatedArrival      departure plus today's driving time; only for trips leaving soon, otherwise null
 * @param estimatedDelayMinutes how much later than the timetable that is (0 when on time); null without an estimate
 * @param note                  why there is no estimate, when there isn't one
 */
public record TripTrafficResponse(
        Long tripId,
        LocalDateTime departureTime,
        LocalDateTime scheduledArrival,
        LocalDateTime estimatedArrival,
        Integer estimatedDelayMinutes,
        String note,
        TrafficResponse traffic
) {}
