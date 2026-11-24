package com.busbooking;

import com.busbooking.dto.TrafficResponse;
import com.busbooking.dto.TripTrafficResponse;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.model.Route;
import com.busbooking.model.Trip;
import com.busbooking.model.TripStatus;
import com.busbooking.repository.RouteRepository;
import com.busbooking.repository.TripRepository;
import com.busbooking.traffic.DailyQuota;
import com.busbooking.traffic.RoutePoints;
import com.busbooking.traffic.TrafficLevel;
import com.busbooking.traffic.TrafficLookupException;
import com.busbooking.traffic.TrafficProperties;
import com.busbooking.traffic.TrafficProvider;
import com.busbooking.traffic.TrafficReading;
import com.busbooking.traffic.TrafficService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Caching, the daily limit, stale fallback and arrival estimates, without a network or database. */
class TrafficServiceTests {

    /** Answers from a script: a reading (minutes now, minutes on an empty road) or a failure. */
    static class ScriptedProvider implements TrafficProvider {
        final Deque<Object> answers = new ArrayDeque<>();
        int calls;

        ScriptedProvider then(int currentMin, int typicalMin) {
            answers.add(new TrafficReading(currentMin * 60L, typicalMin * 60L, 100_000, "poly", null));
            return this;
        }

        ScriptedProvider thenFail() {
            answers.add(new TrafficLookupException("Google Maps could not be reached"));
            return this;
        }

        @Override public String source() { return "TEST"; }
        @Override public boolean billed() { return true; }

        @Override
        public TrafficReading read(RoutePoints route) {
            calls++;
            Object next = answers.poll();
            if (next instanceof RuntimeException e) throw e;
            if (next == null) throw new AssertionError("provider called more often than expected");
            return (TrafficReading) next;
        }
    }

    private final RouteRepository routes = mock(RouteRepository.class);
    private final TripRepository trips = mock(TripRepository.class);
    private final Route route = route(1L);

    private TrafficService service(ScriptedProvider provider, Duration ttl, int dailyLimit) {
        when(routes.findById(1L)).thenReturn(Optional.of(route));
        var props = new TrafficProperties(TrafficProperties.Mode.GOOGLE, null, "key", null, null, null, ttl,
                null, dailyLimit, false, Duration.ofHours(3));
        return new TrafficService(provider, props, routes, trips);
    }

    private static Route route(Long id) {
        Route r = new Route("TP. Hồ Chí Minh", "Vũng Tàu", "Bến xe Miền Đông mới", "Bến xe Vũng Tàu", 125, 150,
                new BigDecimal("160000"), true);
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }

    @Test
    void oneAnswerPerRouteIsReusedUntilItExpires() {
        var provider = new ScriptedProvider().then(150, 120);
        TrafficService service = service(provider, Duration.ofMinutes(5), 100);

        TrafficResponse first = service.forRoute(1L);
        TrafficResponse second = service.forRoute(1L);

        assertEquals(1, provider.calls);
        assertEquals(150, second.currentMinutes());
        assertEquals(120, first.typicalMinutes());
        assertEquals(30, first.delayMinutes());
        assertEquals(TrafficLevel.MODERATE, first.level());
        assertEquals(100.0, first.distanceKm());
        assertFalse(first.stale());
    }

    @Test
    void whenGoogleFailsTheLastReadingIsShownAsStale() {
        var provider = new ScriptedProvider().then(200, 120).thenFail();
        TrafficService service = service(provider, Duration.ZERO, 100); // never fresh: always asks again

        service.forRoute(1L);
        TrafficResponse stale = service.forRoute(1L);

        assertEquals(2, provider.calls);
        assertTrue(stale.stale());
        assertEquals(200, stale.currentMinutes());
        assertEquals(TrafficLevel.HEAVY, stale.level());
        assertTrue(stale.note().startsWith("Google Maps could not be reached; showing the reading from"));
    }

    @Test
    void withNothingToFallBackOnTheCallFailsWith503() {
        TrafficService service = service(new ScriptedProvider().thenFail(), Duration.ofMinutes(5), 100);
        ApiException e = assertThrows(ApiException.class, () -> service.forRoute(1L));
        assertEquals(ErrorCode.TRAFFIC_UNAVAILABLE, e.getCode());
        assertEquals(503, e.getCode().status().value());
    }

    @Test
    void theDailyLimitStopsCallsToGoogle() {
        var provider = new ScriptedProvider().then(130, 120).then(999, 120);
        TrafficService service = service(provider, Duration.ZERO, 1);

        service.forRoute(1L);
        TrafficResponse second = service.forRoute(1L);

        assertEquals(1, provider.calls); // the second reading was never requested
        assertTrue(second.stale());
        assertTrue(second.note().startsWith("Today's live-traffic limit has been reached"));
        assertEquals(1, service.status().requestsToday());
    }

    @Test
    void tripsLeavingSoonGetAnArrivalEstimate() {
        TrafficService service = service(new ScriptedProvider().then(180, 120), Duration.ofMinutes(5), 100);
        LocalDateTime dep = LocalDateTime.now().plusHours(1).withNano(0);
        Trip soon = new Trip(route, null, dep, dep.plusMinutes(150), new BigDecimal("160000"));
        Trip later = new Trip(route, null, dep.plusDays(3), dep.plusDays(3).plusMinutes(150), new BigDecimal("160000"));
        Trip cancelled = new Trip(route, null, dep, dep.plusMinutes(150), new BigDecimal("160000"));
        cancelled.setStatus(TripStatus.CANCELLED);
        when(trips.findById(10L)).thenReturn(Optional.of(soon));
        when(trips.findById(11L)).thenReturn(Optional.of(later));
        when(trips.findById(12L)).thenReturn(Optional.of(cancelled));

        TripTrafficResponse s = service.forTrip(10L);
        assertEquals(dep.plusMinutes(180), s.estimatedArrival());
        assertEquals(30, s.estimatedDelayMinutes()); // timetable says 150 min, traffic says 180
        assertNull(s.note());

        TripTrafficResponse l = service.forTrip(11L);
        assertNull(l.estimatedArrival());
        assertEquals("An arrival estimate from live traffic is given for trips leaving in the next 3 hours", l.note());

        ApiException e = assertThrows(ApiException.class, () -> service.forTrip(12L));
        assertEquals(ErrorCode.TRIP_CANCELLED, e.getCode());
    }

    @Test
    void dailyQuotaStartsAgainTheNextDay() {
        Instant start = Instant.parse("2026-11-23T10:00:00Z");
        Clock[] clock = {Clock.fixed(start, ZoneOffset.UTC)};
        DailyQuota quota = new DailyQuota(2, new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return clock[0].instant(); }
        });

        assertTrue(quota.tryUse());
        assertTrue(quota.tryUse());
        assertFalse(quota.tryUse());
        clock[0] = Clock.fixed(start.plus(Duration.ofDays(1)), ZoneOffset.UTC);
        assertTrue(quota.tryUse());
        assertEquals(1, quota.usedToday());
    }
}
