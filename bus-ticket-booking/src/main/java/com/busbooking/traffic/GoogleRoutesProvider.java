package com.busbooking.traffic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Live driving times from the Google Maps Routes API (computeRoutes, {@code TRAFFIC_AWARE}).
 * Google bills these requests at its "Pro" rate, so {@link TrafficService} caches answers and caps
 * requests per day. The API key stays on the server.
 */
public class GoogleRoutesProvider implements TrafficProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleRoutesProvider.class);
    private static final String FIELDS =
            "routes.duration,routes.staticDuration,routes.distanceMeters,routes.polyline.encodedPolyline";
    private static final String SPEED_FIELDS = ",routes.travelAdvisory.speedReadingIntervals";

    private final RestClient http;
    private final String endpoint;
    private final boolean speedIntervals;

    public GoogleRoutesProvider(TrafficProperties props) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(props.timeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(props.timeout());
        this.http = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("X-Goog-Api-Key", props.googleApiKey())
                .build();
        this.endpoint = props.endpoint();
        this.speedIntervals = props.speedIntervals();
    }

    @Override
    public String source() {
        return "GOOGLE";
    }

    @Override
    public boolean billed() {
        return true;
    }

    @Override
    public TrafficReading read(RoutePoints route) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("origin", waypoint(route.hasOriginPosition(), route.originLat(), route.originLng(),
                route.originStation(), route.origin()));
        body.put("destination", waypoint(route.hasDestinationPosition(), route.destinationLat(),
                route.destinationLng(), route.destinationStation(), route.destination()));
        body.put("travelMode", "DRIVE");
        body.put("routingPreference", "TRAFFIC_AWARE"); // no departureTime = leaving now
        body.put("languageCode", "vi");
        body.put("units", "METRIC");
        if (speedIntervals) body.put("extraComputations", List.of("TRAFFIC_ON_POLYLINE"));

        Response res;
        try {
            res = http.post().uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Goog-FieldMask", speedIntervals ? FIELDS + SPEED_FIELDS : FIELDS)
                    .body(body)
                    .retrieve()
                    .body(Response.class);
        } catch (RestClientResponseException e) {
            // Google explains the problem (bad key, API not enabled, quota) in the body; it never echoes the key.
            log.warn("Google Routes API answered {} for route {}: {}", e.getStatusCode().value(), route.routeId(),
                    e.getResponseBodyAsString());
            throw new TrafficLookupException("Google Maps could not give live traffic right now", e);
        } catch (RestClientException e) {
            log.warn("Google Routes API unreachable for route {}: {}", route.routeId(), e.getMessage());
            throw new TrafficLookupException("Google Maps could not be reached", e);
        }

        if (res == null || res.routes() == null || res.routes().isEmpty() || res.routes().get(0).duration() == null) {
            throw new TrafficLookupException("Google Maps found no driving route between these stations");
        }
        GRoute r = res.routes().get(0);
        long current = seconds(r.duration());
        long typical = r.staticDuration() == null ? current : seconds(r.staticDuration());
        List<SpeedInterval> intervals = r.travelAdvisory() == null || r.travelAdvisory().speedReadingIntervals() == null
                ? List.of()
                : r.travelAdvisory().speedReadingIntervals().stream()
                        .map(i -> new SpeedInterval(nz(i.startPolylinePointIndex()), nz(i.endPolylinePointIndex()),
                                i.speed() == null ? "SPEED_UNSPECIFIED" : i.speed()))
                        .toList();
        return new TrafficReading(current, typical, r.distanceMeters(),
                r.polyline() == null ? null : r.polyline().encodedPolyline(), intervals);
    }

    /** A station by position when known, otherwise by name ("Bến xe Miền Tây, TP. Hồ Chí Minh, Việt Nam"). */
    private static Map<String, Object> waypoint(boolean hasPosition, Double lat, Double lng, String station, String city) {
        if (hasPosition) {
            return Map.of("location", Map.of("latLng", Map.of("latitude", lat, "longitude", lng)));
        }
        return Map.of("address", station + ", " + city + ", Việt Nam");
    }

    /** Google durations look like "8460s" or "3.5s". */
    static long seconds(String duration) {
        String n = duration.endsWith("s") ? duration.substring(0, duration.length() - 1) : duration;
        try {
            return new BigDecimal(n).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException | NumberFormatException e) {
            throw new TrafficLookupException("Google Maps sent a duration that could not be read", e);
        }
    }

    private static int nz(Integer i) {
        return i == null ? 0 : i;
    }

    // Only the parts of Google's response named in the field mask.
    record Response(List<GRoute> routes) {}

    record GRoute(String duration, String staticDuration, Integer distanceMeters, GPolyline polyline,
                  GAdvisory travelAdvisory) {}

    record GPolyline(String encodedPolyline) {}

    record GAdvisory(List<GInterval> speedReadingIntervals) {}

    record GInterval(Integer startPolylinePointIndex, Integer endPolylinePointIndex, String speed) {}
}
