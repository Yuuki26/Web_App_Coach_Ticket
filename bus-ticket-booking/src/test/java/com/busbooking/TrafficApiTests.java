package com.busbooking;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Live traffic through the real HTTP client, against a stand-in for Google's Routes API running on
 * localhost: what we send to Google, how its answer is read, and the public and manager endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class TrafficApiTests {

    static final HttpServer GOOGLE;
    static final AtomicInteger CALLS = new AtomicInteger();
    static volatile int replyStatus = 200;
    static volatile String replyBody = "{}";
    static volatile String lastKey, lastFieldMask, lastBody;

    static {
        try {
            GOOGLE = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        GOOGLE.createContext("/directions/v2:computeRoutes", exchange -> {
            CALLS.incrementAndGet();
            lastKey = exchange.getRequestHeaders().getFirst("X-Goog-Api-Key");
            lastFieldMask = exchange.getRequestHeaders().getFirst("X-Goog-FieldMask");
            lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] out = replyBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(replyStatus, out.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(out);
            }
        });
        GOOGLE.start();
    }

    @DynamicPropertySource
    static void useFakeGoogle(DynamicPropertyRegistry registry) {
        registry.add("app.traffic.provider", () -> "google");
        registry.add("app.traffic.google-api-key", () -> "test-key");
        registry.add("app.traffic.endpoint",
                () -> "http://127.0.0.1:" + GOOGLE.getAddress().getPort() + "/directions/v2:computeRoutes");
    }

    @AfterAll
    static void stopFakeGoogle() {
        GOOGLE.stop(0);
    }

    @Autowired MockMvc mvc;

    @BeforeEach
    void googleAnswersNormally() {
        replyStatus = 200;
        replyBody = """
                {"routes":[{"duration":"9000s","staticDuration":"7200s","distanceMeters":305400,
                  "polyline":{"encodedPolyline":"_p~iF~ps|U_ulLnnqC"}}]}""";
    }

    private List<Integer> popularRouteIds() throws Exception {
        String json = mvc.perform(get("/api/v1/routes/popular")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$[*].id");
    }

    @Test
    void routeTrafficComesFromGoogleAndIsReused() throws Exception {
        int routeId = popularRouteIds().get(0);
        int before = CALLS.get();

        mvc.perform(get("/api/v1/routes/{id}/traffic", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(routeId))
                .andExpect(jsonPath("$.source").value("GOOGLE"))
                .andExpect(jsonPath("$.currentMinutes").value(150))
                .andExpect(jsonPath("$.typicalMinutes").value(120))
                .andExpect(jsonPath("$.delayMinutes").value(30))
                .andExpect(jsonPath("$.level").value("MODERATE"))
                .andExpect(jsonPath("$.distanceKm").value(305.4))
                .andExpect(jsonPath("$.encodedPolyline").value("_p~iF~ps|U_ulLnnqC"))
                .andExpect(jsonPath("$.stale").value(false));

        // What Google received: our key, a field mask, live-traffic routing between the two stations.
        assertEquals("test-key", lastKey);
        assertTrue(lastFieldMask.contains("routes.duration") && lastFieldMask.contains("routes.staticDuration"));
        assertTrue(lastBody.contains("\"routingPreference\":\"TRAFFIC_AWARE\""), lastBody);
        assertTrue(lastBody.contains("\"travelMode\":\"DRIVE\""), lastBody);
        assertTrue(lastBody.contains("\"latitude\""), "seeded stations are sent by position: " + lastBody);

        // A second visitor within 5 minutes gets the same answer without another (billed) request.
        mvc.perform(get("/api/v1/routes/{id}/traffic", routeId)).andExpect(status().isOk());
        assertEquals(before + 1, CALLS.get());
    }

    @Test
    void routesPageListsEveryRouteWithPositionsAndAllowsTheLiveServerFrontend() throws Exception {
        mvc.perform(get("/api/v1/routes").header("Origin", "http://127.0.0.1:5500"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5500"))
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(24)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].originStation").value("Bến xe Miền Đông mới"))
                .andExpect(jsonPath("$[0].distanceKm").value(308))
                .andExpect(jsonPath("$[0].originLat").isNumber())
                .andExpect(jsonPath("$[0].destinationLng").isNumber());
    }

    @Test
    void googleErrorsBecome503WhenThereIsNothingCached() throws Exception {
        int routeId = popularRouteIds().get(1);
        replyStatus = 403;
        replyBody = "{\"error\":{\"code\":403,\"status\":\"PERMISSION_DENIED\"}}";

        mvc.perform(get("/api/v1/routes/{id}/traffic", routeId))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("TRAFFIC_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value("Google Maps could not give live traffic right now"));

        mvc.perform(get("/api/v1/routes/{id}/traffic", 999_999)).andExpect(status().isNotFound());
    }

    @Test
    void tripTrafficLinksATripToItsRoute() throws Exception {
        int routeId = popularRouteIds().get(2);
        String search = mvc.perform(get("/api/v1/routes/popular")).andReturn().getResponse().getContentAsString();
        String from = JsonPath.read(search, "$[2].origin");
        String to = JsonPath.read(search, "$[2].destination");
        String trips = mvc.perform(get("/api/v1/trips/search").param("from", from).param("to", to)
                        .param("date", java.time.LocalDate.now().plusDays(3).toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int tripId = JsonPath.read(trips, "$[0].id");
        assertEquals(routeId, (int) JsonPath.read(trips, "$[0].routeId"));

        mvc.perform(get("/api/v1/trips/{id}/traffic", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(tripId))
                .andExpect(jsonPath("$.estimatedArrival").doesNotExist())
                .andExpect(jsonPath("$.note").value(
                        "An arrival estimate from live traffic is given for trips leaving in the next 3 hours"))
                .andExpect(jsonPath("$.traffic.routeId").value(routeId))
                .andExpect(jsonPath("$.traffic.currentMinutes").value(150));
    }

    @Test
    void managersCanSeeTrafficStatusAndStationPositions() throws Exception {
        mvc.perform(get("/api/v1/manage/traffic/status")).andExpect(status().isUnauthorized());

        String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"manager@test.com\",\"password\":\"Manager123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String manager = "Bearer " + JsonPath.read(login, "$.accessToken");

        mvc.perform(get("/api/v1/manage/traffic/status").header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.billed").value(true))
                .andExpect(jsonPath("$.dailyLimit").value(160));

        int routeId = popularRouteIds().get(0);
        mvc.perform(get("/api/v1/manage/routes/{id}", routeId).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originLat").isNumber())
                .andExpect(jsonPath("$.destinationLng").isNumber());

        // A position needs both numbers.
        mvc.perform(post("/api/v1/manage/routes").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"origin":"Biên Hòa","destination":"Long Khánh","originStation":"Bến xe Biên Hòa",
                                 "destinationStation":"Bến xe Long Khánh","distanceKm":50,"durationMinutes":70,
                                 "fromPrice":60000,"originLat":10.95}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
