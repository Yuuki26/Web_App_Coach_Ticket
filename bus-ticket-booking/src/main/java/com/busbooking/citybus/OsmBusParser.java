package com.busbooking.citybus;

import com.busbooking.traffic.Polyline;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns an Overpass API answer into a {@link CityBusSnapshot}. The answer must contain the bus route
 * relations ({@code out body}), their route_master relations, their member nodes ({@code out body})
 * and member ways with geometry ({@code out geom}) — see {@link CityBusImportService#OVERPASS_QUERY}.
 * <p>
 * One line per network + ref (e.g. "01"); each route relation is one direction. Stops are the
 * relation's {@code platform*} (or {@code stop*}) member nodes in order.
 */
public class OsmBusParser {

    /** Paths are simplified to this many metres, which keeps the data small and looks the same on a map. */
    static final double SIMPLIFY_METERS = 10;

    private final Set<String> networks;

    /** @param networks only routes whose {@code network} tag is one of these are kept */
    public OsmBusParser(Set<String> networks) {
        this.networks = networks;
    }

    public CityBusSnapshot parse(JsonNode overpass, String fetchedAt) {
        Map<Long, JsonNode> nodes = new HashMap<>();
        Map<Long, JsonNode> ways = new HashMap<>();
        List<JsonNode> routes = new ArrayList<>();
        List<JsonNode> masters = new ArrayList<>();
        for (JsonNode e : overpass.path("elements")) {
            String type = e.path("type").asString();
            long id = e.path("id").asLong();
            switch (type) {
                case "node" -> nodes.put(id, e);
                case "way" -> ways.put(id, e);
                case "relation" -> {
                    String relType = tag(e, "type");
                    if ("route".equals(relType) && "bus".equals(tag(e, "route"))) routes.add(e);
                    else if ("route_master".equals(relType)) masters.add(e);
                }
                default -> { }
            }
        }

        // route relation id -> (master id, position in the master), to keep the master's direction order
        Map<Long, long[]> masterOf = new HashMap<>();
        for (JsonNode m : masters) {
            int pos = 0;
            for (JsonNode member : m.path("members")) {
                if ("relation".equals(member.path("type").asString())) {
                    masterOf.putIfAbsent(member.path("ref").asLong(), new long[]{m.path("id").asLong(), pos++});
                }
            }
        }

        Map<String, List<JsonNode>> byLine = new LinkedHashMap<>();
        for (JsonNode r : routes) {
            String network = tag(r, "network");
            String ref = tag(r, "ref");
            if (network == null || ref == null || !networks.contains(network)) continue;
            byLine.computeIfAbsent(network + "|" + ref, k -> new ArrayList<>()).add(r);
        }

        Map<Long, CityBusSnapshot.Stop> stops = new LinkedHashMap<>();
        List<CityBusSnapshot.Line> lines = new ArrayList<>();
        for (List<JsonNode> group : byLine.values()) {
            group.sort(Comparator.comparingLong((JsonNode r) -> {
                long[] m = masterOf.get(r.path("id").asLong());
                return m == null ? Long.MAX_VALUE : m[1];
            }).thenComparingLong(r -> r.path("id").asLong()));

            List<CityBusSnapshot.Direction> directions = new ArrayList<>();
            for (JsonNode r : group) {
                CityBusSnapshot.Direction d = direction(r, nodes, ways, stops);
                if (d != null) directions.add(d);
            }
            if (directions.isEmpty()) continue;

            JsonNode first = group.get(0);
            long[] master = masterOf.get(first.path("id").asLong());
            lines.add(new CityBusSnapshot.Line(
                    tag(first, "network"), tag(first, "ref"),
                    firstTag(group, "operator"), firstTag(group, "opening_hours"), firstTag(group, "interval"),
                    firstTag(group, "charge"), firstTag(group, "colour"),
                    master == null ? null : master[0], directions));
        }
        return new CityBusSnapshot("© OpenStreetMap contributors, ODbL", fetchedAt, lines, new ArrayList<>(stops.values()));
    }

    private CityBusSnapshot.Direction direction(JsonNode r, Map<Long, JsonNode> nodes, Map<Long, JsonNode> ways,
                                                Map<Long, CityBusSnapshot.Stop> stops) {
        List<List<double[]>> pieces = new ArrayList<>();
        List<Long> stopIds = new ArrayList<>();
        for (JsonNode m : r.path("members")) {
            String role = m.path("role").asString().trim();
            String type = m.path("type").asString();
            long ref = m.path("ref").asLong();
            boolean isStop = role.startsWith("platform") || role.startsWith("stop");
            if ("way".equals(type) && !isStop) {
                JsonNode way = ways.get(ref);
                if (way != null) pieces.add(points(way.path("geometry")));
            } else if ("node".equals(type) && isStop) {
                JsonNode node = nodes.get(ref);
                if (node == null || !node.has("lat")) continue;
                String name = tag(node, "name");
                stops.putIfAbsent(ref, new CityBusSnapshot.Stop(ref, name == null ? "Trạm dừng" : name,
                        node.path("lat").asDouble(), node.path("lon").asDouble()));
                // A stop listed twice in a row is kept once; loop lines may still pass a stop twice.
                if (stopIds.isEmpty() || !stopIds.get(stopIds.size() - 1).equals(ref)) stopIds.add(ref);
            }
        }
        List<double[]> path = Geo.stitch(pieces);
        if (path.size() < 2) return null;
        if (!isLoop(r) && stopIds.size() >= 2) {
            stopIds = inTravelOrder(stopIds, path, stops);
            // Some relations carry extra road past the terminals (e.g. part of the return trip): cut it off.
            CityBusSnapshot.Stop first = stops.get(stopIds.get(0)), last = stops.get(stopIds.get(stopIds.size() - 1));
            double from = Geo.alongPath(path, new double[]{first.lat(), first.lng()});
            double to = Geo.alongPath(path, new double[]{last.lat(), last.lng()});
            if (to - from > 200) path = Geo.slice(path, from, to);
        }
        return new CityBusSnapshot.Direction(r.path("id").asLong(), tag(r, "name"), tag(r, "from"), tag(r, "to"),
                Polyline.encode(Geo.simplify(path, SIMPLIFY_METERS)), (int) Math.round(Geo.length(path)),
                stopIds);
    }

    /** A line that ends where it starts; its stops can't be ordered by position along the road. */
    private static boolean isLoop(JsonNode r) {
        String from = tag(r, "from");
        return "yes".equals(tag(r, "roundtrip")) || (from != null && from.equals(tag(r, "to")));
    }

    /**
     * Stops sorted by where they are along the road. OSM's member lists are sometimes out of order
     * (e.g. a mid-route stop added at the end), which would put it last in the stop list.
     */
    static List<Long> inTravelOrder(List<Long> stopIds, List<double[]> path, Map<Long, CityBusSnapshot.Stop> stops) {
        Map<Long, Double> along = new HashMap<>();
        for (Long id : stopIds) {
            CityBusSnapshot.Stop s = stops.get(id);
            along.putIfAbsent(id, Geo.alongPath(path, new double[]{s.lat(), s.lng()}));
        }
        List<Long> sorted = new ArrayList<>(stopIds);
        sorted.sort(Comparator.comparingDouble(along::get)); // stable: ties keep OSM's order
        List<Long> out = new ArrayList<>();
        for (Long id : sorted) if (out.isEmpty() || !out.get(out.size() - 1).equals(id)) out.add(id);
        return out;
    }

    private static List<double[]> points(JsonNode geometry) {
        List<double[]> out = new ArrayList<>();
        for (JsonNode p : geometry) {
            if (p.has("lat")) out.add(new double[]{p.path("lat").asDouble(), p.path("lon").asDouble()});
        }
        return out;
    }

    private static String tag(JsonNode e, String key) {
        JsonNode v = e.path("tags").path(key);
        if (v.isMissingNode() || v.isNull()) return null;
        String s = v.asString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String firstTag(List<JsonNode> group, String key) {
        for (JsonNode r : group) {
            String v = tag(r, key);
            if (v != null) return v;
        }
        return null;
    }
}
