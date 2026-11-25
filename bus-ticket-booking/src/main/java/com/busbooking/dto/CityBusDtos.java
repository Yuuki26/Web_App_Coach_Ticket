package com.busbooking.dto;

import java.util.List;

/**
 * City bus API shapes. Text fields keep OpenStreetMap's format: openingHours "Mo-Su 05:00-22:00",
 * headway "00:08-00:20" (every 8–20 minutes), fare "6000 VND" (fareVnd 6000).
 */
public final class CityBusDtos {

    private CityBusDtos() {}

    public static final String ATTRIBUTION = "Bus data © OpenStreetMap contributors (ODbL)";

    /** One line in the list. {@code from}/{@code to} are the first direction's ends. */
    public record Summary(Long id, String ref, String name, String from, String to, String operator,
                          String openingHours, String headway, String fare, Integer fareVnd, String colour,
                          int directions, int stops, double lengthKm) {}

    public record Stop(Long id, String name, double lat, double lng) {}

    public record Direction(Long id, String name, String from, String to, double lengthKm, String polyline,
                            List<Stop> stops) {}

    public record Detail(Summary line, List<Direction> directions, String attribution) {}

    public record LineRef(Long id, String ref, String colour) {}

    public record NearbyStop(Long id, String name, double lat, double lng, int distanceMeters, List<LineRef> lines) {}
}
