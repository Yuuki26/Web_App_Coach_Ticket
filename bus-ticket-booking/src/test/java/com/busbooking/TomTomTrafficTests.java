package com.busbooking;

import com.busbooking.traffic.Polyline;
import com.busbooking.traffic.RoutePoints;
import com.busbooking.traffic.SpeedInterval;
import com.busbooking.traffic.TomTomTrafficProvider;
import com.busbooking.traffic.TrafficLookupException;
import com.busbooking.traffic.TrafficProperties;
import com.busbooking.traffic.TrafficReading;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The TomTom provider against a stand-in for TomTom's Routing API on localhost. */
class TomTomTrafficTests {

    private HttpServer server;
    private volatile int replyStatus = 200;
    private volatile String replyBody;
    private volatile String lastPath, lastQuery;
    private TomTomTrafficProvider provider;

    private static final RoutePoints ROUTE = new RoutePoints(7L, "TP. Hồ Chí Minh", "Vũng Tàu",
            "Bến xe Miền Đông mới", "Bến xe Vũng Tàu", 10.879, 106.8155, 10.352, 107.0857, 125, 150);

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/routing/1/calculateRoute/", exchange -> {
            lastPath = URLDecoder.decode(exchange.getRequestURI().getRawPath(), StandardCharsets.UTF_8);
            lastQuery = exchange.getRequestURI().getQuery();
            byte[] out = replyBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(replyStatus, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        });
        server.start();
        var props = new TrafficProperties(TrafficProperties.Mode.TOMTOM, null, null, "tt-key", null,
                "http://127.0.0.1:" + server.getAddress().getPort(), null, Duration.ofSeconds(3), null, false, null);
        provider = new TomTomTrafficProvider(props);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void readsTrafficTimesPathAndJams() {
        replyBody = """
                {"formatVersion":"0.0.12","routes":[{
                  "summary":{"lengthInMeters":96000,"travelTimeInSeconds":9000,"trafficDelayInSeconds":1500,
                             "noTrafficTravelTimeInSeconds":7200,"historicTrafficTravelTimeInSeconds":8000},
                  "legs":[{"points":[{"latitude":38.5,"longitude":-120.2},{"latitude":40.7,"longitude":-120.95},
                                     {"latitude":43.252,"longitude":-126.453},{"latitude":43.3,"longitude":-126.5}]}],
                  "sections":[{"startPointIndex":0,"endPointIndex":3,"sectionType":"TRAVEL_MODE","travelMode":"car"},
                              {"startPointIndex":1,"endPointIndex":2,"sectionType":"TRAFFIC",
                               "simpleCategory":"JAM","magnitudeOfDelay":3,"delayInSeconds":900}]}]}""";

        TrafficReading r = provider.read(ROUTE);

        assertEquals(9000, r.currentSeconds());
        assertEquals(7200, r.typicalSeconds());
        assertEquals(96000, r.distanceMeters());
        assertTrue(r.encodedPolyline().startsWith("_p~iF~ps|U_ulLnnqC_mqNvxq`@"));
        assertEquals(List.of(new SpeedInterval(0, 1, "NORMAL"), new SpeedInterval(1, 2, "TRAFFIC_JAM"),
                new SpeedInterval(2, 3, "NORMAL")), r.speedIntervals());

        // What TomTom received: both stations by position, live traffic on, our key.
        assertTrue(lastPath.endsWith("/10.879000,106.815500:10.352000,107.085700/json"), lastPath);
        assertTrue(lastQuery.contains("key=tt-key"));
        assertTrue(lastQuery.contains("traffic=true"));
        assertTrue(lastQuery.contains("computeTravelTimeFor=all"));
        assertTrue(lastQuery.contains("sectionType=traffic"));
    }

    @Test
    void minorDelaysAreSlowAndMissingNoTrafficTimeUsesTheDelay() {
        replyBody = """
                {"routes":[{"summary":{"lengthInMeters":5000,"travelTimeInSeconds":600,"trafficDelayInSeconds":120},
                  "legs":[{"points":[{"latitude":10.8,"longitude":106.7},{"latitude":10.81,"longitude":106.71}]}],
                  "sections":[{"startPointIndex":0,"endPointIndex":1,"sectionType":"TRAFFIC",
                               "simpleCategory":"JAM","magnitudeOfDelay":1}]}]}""";

        TrafficReading r = provider.read(ROUTE);

        assertEquals(480, r.typicalSeconds());
        assertEquals(List.of(new SpeedInterval(0, 1, "SLOW")), r.speedIntervals());
    }

    @Test
    void errorsBecomeReadableMessages() {
        replyStatus = 403;
        replyBody = "{\"detailedError\":{\"code\":\"Forbidden\",\"message\":\"Developer Inactive\"}}";
        assertEquals("TomTom could not give live traffic right now",
                assertThrows(TrafficLookupException.class, () -> provider.read(ROUTE)).getMessage());

        replyStatus = 400;
        replyBody = "{\"detailedError\":{\"code\":\"MAP_MATCHING_FAILURE\",\"message\":\"no road\"}}";
        assertEquals("TomTom could not find a road route between these stations",
                assertThrows(TrafficLookupException.class, () -> provider.read(ROUTE)).getMessage());

        RoutePoints noPosition = new RoutePoints(8L, "A", "B", "Bến A", "Bến B", null, null, null, null, 10, 20);
        assertTrue(assertThrows(TrafficLookupException.class, () -> provider.read(noPosition))
                .getMessage().startsWith("This route has no map positions"));
    }

    @Test
    void polylineMatchesGooglesFormat() {
        // Google's documented example
        assertEquals("_p~iF~ps|U_ulLnnqC_mqNvxq`@", Polyline.encode(List.of(
                new double[]{38.5, -120.2}, new double[]{40.7, -120.95}, new double[]{43.252, -126.453})));
    }
}
