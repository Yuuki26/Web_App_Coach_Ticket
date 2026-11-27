package com.busbooking.citybus;

import java.util.List;

/**
 * City bus data in a compact form: what the importer reads from OpenStreetMap, what is bundled in
 * {@code src/main/resources/citybus/hcmc-bus-osm.json}, and what GET /api/v1/admin/city-buses/snapshot
 * returns. Data © OpenStreetMap contributors, available under the Open Database License (ODbL).
 */
public record CityBusSnapshot(String source, String fetchedAt, List<Line> lines, List<Stop> stops) {

    public record Line(String network, String ref, String operator, String openingHours, String headway,
                       String fare, String colour, Long osmMasterId, List<Direction> directions) {}

    /** {@code stops} are OSM node ids of the stops in travel order. */
    public record Direction(long osmId, String name, String from, String to, String polyline, int lengthMeters,
                            List<Long> stops) {}

    public record Stop(long osmId, String name, double lat, double lng) {}
}
