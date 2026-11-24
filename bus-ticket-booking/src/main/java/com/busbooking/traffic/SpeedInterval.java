package com.busbooking.traffic;

/**
 * Traffic on one stretch of the route line: points {@code start} (inclusive) to {@code end} (exclusive) of
 * the decoded polyline. {@code speed} is NORMAL, SLOW or TRAFFIC_JAM.
 */
public record SpeedInterval(int start, int end, String speed) {}
