package com.busbooking.traffic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Live driving times from the TomTom Routing API (Calculate Route with {@code traffic=true}).
 * TomTom's free plan needs no credit card and allows about 2,500 requests a day; {@link TrafficService}
 * still caches answers and applies {@code app.traffic.daily-limit}.
 * <p>
 * TomTom takes the key as a URL parameter, so URLs are never written to the log.
 */
public class TomTomTrafficProvider implements TrafficProvider {

    private static final Logger log = LoggerFactory.getLogger(TomTomTrafficProvider.class);

    private final RestClient http;
    private final String baseUrl;
    private final String key;

    public TomTomTrafficProvider(TrafficProperties props) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(props.timeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(props.timeout());
        this.http = RestClient.builder().requestFactory(factory).build();
        this.baseUrl = props.tomtomBaseUrl();
        this.key = props.tomtomApiKey();
    }

    @Override
    public String source() {
        return "TOMTOM";
    }

    @Override
    public boolean billed() {
        return true;
    }

    @Override
    public TrafficReading read(RoutePoints route) {
        if (!route.hasOriginPosition() || !route.hasDestinationPosition()) {
            throw new TrafficLookupException(
                    "This route has no map positions for its stations; add them in the route settings");
        }
        String locations = String.format(Locale.ROOT, "%.6f,%.6f:%.6f,%.6f",
                route.originLat(), route.originLng(), route.destinationLat(), route.destinationLng());

        Response res;
        try {
            res = http.get()
                    .uri(baseUrl + "/routing/1/calculateRoute/{locations}/json?key={key}&traffic=true"
                            + "&travelMode=car&computeTravelTimeFor=all&sectionType=traffic&routeType=fastest",
                            locations, key)
                    .retrieve()
                    .body(Response.class);
        } catch (RestClientResponseException e) {
            // TomTom's error body explains the problem (bad key, no road nearby) and does not contain the key.
            log.warn("TomTom Routing API answered {} for route {}: {}", e.getStatusCode().value(), route.routeId(),
                    e.getResponseBodyAsString());
            throw new TrafficLookupException(e.getStatusCode().value() == 400
                    ? "TomTom could not find a road route between these stations"
                    : "TomTom could not give live traffic right now", e);
        } catch (RestClientException e) {
            log.warn("TomTom Routing API unreachable for route {}: {}", route.routeId(), redact(e.getMessage()));
            throw new TrafficLookupException("TomTom could not be reached", e);
        }

        if (res == null || res.routes() == null || res.routes().isEmpty() || res.routes().get(0).summary() == null) {
            throw new TrafficLookupException("TomTom found no driving route between these stations");
        }
        TRoute r = res.routes().get(0);
        Summary s = r.summary();
        long current = s.travelTimeInSeconds();
        long typical = s.noTrafficTravelTimeInSeconds() != null ? s.noTrafficTravelTimeInSeconds()
                : Math.max(0, current - (s.trafficDelayInSeconds() == null ? 0 : s.trafficDelayInSeconds()));

        List<double[]> points = new ArrayList<>();
        if (r.legs() != null) {
            for (Leg leg : r.legs()) {
                if (leg.points() == null) continue;
                for (Point p : leg.points()) points.add(new double[]{p.latitude(), p.longitude()});
            }
        }
        return new TrafficReading(current, typical, s.lengthInMeters(),
                points.isEmpty() ? null : Polyline.encode(points), intervals(r.sections(), points.size()));
    }

    /**
     * TomTom lists only the stretches with traffic; the rest of the road is NORMAL. Like Google's
     * speed readings, the result covers the whole line without gaps.
     */
    static List<SpeedInterval> intervals(List<Section> sections, int pointCount) {
        if (pointCount < 2) return List.of();
        List<Section> traffic = sections == null ? List.of() : sections.stream()
                .filter(s -> "TRAFFIC".equals(s.sectionType()) && s.startPointIndex() != null && s.endPointIndex() != null)
                .sorted(Comparator.comparingInt(Section::startPointIndex))
                .toList();
        List<SpeedInterval> out = new ArrayList<>();
        int at = 0;
        int last = pointCount - 1;
        for (Section s : traffic) {
            int start = Math.max(at, s.startPointIndex());
            int end = Math.min(last, s.endPointIndex());
            if (end <= start) continue;
            if (start > at) out.add(new SpeedInterval(at, start, "NORMAL"));
            out.add(new SpeedInterval(start, end, speed(s)));
            at = end;
        }
        if (at < last) out.add(new SpeedInterval(at, last, "NORMAL"));
        return out;
    }

    /** Major jams and closures count as TRAFFIC_JAM; any other reported delay as SLOW. */
    private static String speed(Section s) {
        boolean major = s.magnitudeOfDelay() != null && s.magnitudeOfDelay() == 3;
        return major || "ROAD_CLOSURE".equals(s.simpleCategory()) ? "TRAFFIC_JAM" : "SLOW";
    }

    private static String redact(String message) {
        return message == null ? null : message.replaceAll("key=[^&\"\\s]+", "key=***");
    }

    // Only the parts of TomTom's response we use.
    record Response(List<TRoute> routes) {}

    record TRoute(Summary summary, List<Leg> legs, List<Section> sections) {}

    record Summary(Integer lengthInMeters, long travelTimeInSeconds, Integer trafficDelayInSeconds,
                   Integer noTrafficTravelTimeInSeconds) {}

    record Leg(List<Point> points) {}

    record Point(double latitude, double longitude) {}

    record Section(String sectionType, Integer startPointIndex, Integer endPointIndex, String simpleCategory,
                   Integer magnitudeOfDelay) {}
}
