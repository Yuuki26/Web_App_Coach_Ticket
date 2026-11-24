package com.busbooking.traffic;

import com.busbooking.dto.TrafficResponse;
import com.busbooking.dto.TripTrafficResponse;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.Route;
import com.busbooking.model.Trip;
import com.busbooking.model.TripStatus;
import com.busbooking.repository.RouteRepository;
import com.busbooking.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live traffic per route, kept cheap:
 * <ul>
 *   <li>one answer per route is reused for {@code app.traffic.cache-ttl} (5 minutes), however many people ask;</li>
 *   <li>at most {@code app.traffic.daily-limit} billed requests a day;</li>
 *   <li>when Google can't answer or the limit is reached, the last reading is shown marked {@code stale}.</li>
 * </ul>
 * Users never wait on Google twice for the same route at the same moment: one request goes out and the
 * others use its answer.
 */
@Service
public class TrafficService {

    private static final Logger log = LoggerFactory.getLogger(TrafficService.class);

    private record Cached(TrafficReading reading, String source, LocalDateTime checkedAt) {}

    private final TrafficProvider provider;
    private final TrafficProperties props;
    private final RouteRepository routes;
    private final TripRepository trips;
    private final Clock clock;
    private final DailyQuota quota;
    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    public TrafficService(TrafficProvider provider, TrafficProperties props, RouteRepository routes,
                          TripRepository trips) {
        this.provider = provider;
        this.props = props;
        this.routes = routes;
        this.trips = trips;
        this.clock = Clock.system(TrafficConfig.VIETNAM);
        this.quota = new DailyQuota(props.dailyLimit(), clock);
    }

    public TrafficResponse forRoute(Long routeId) {
        Route route = routes.findById(routeId).filter(Route::isActive)
                .orElseThrow(() -> new NotFoundException("Route " + routeId + " not found"));
        return lookup(RoutePoints.from(route));
    }

    public TripTrafficResponse forTrip(Long tripId) {
        Trip trip = trips.findById(tripId).orElseThrow(() -> new NotFoundException("Trip " + tripId + " not found"));
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new ApiException(ErrorCode.TRIP_CANCELLED, "This trip is cancelled");
        }
        TrafficResponse traffic = lookup(RoutePoints.from(trip.getRoute()));

        // Trip times are stored as local wall-clock times, so compare with the server's local time.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime departure = trip.getDepartureTime();
        if (!departure.isAfter(now)) {
            return new TripTrafficResponse(tripId, departure, trip.getArrivalTime(), null, null,
                    "This trip has already left; the traffic shown is for its route now", traffic);
        }
        if (departure.isAfter(now.plus(props.tripWindow()))) {
            return new TripTrafficResponse(tripId, departure, trip.getArrivalTime(), null, null,
                    "An arrival estimate from live traffic is given for trips leaving in the next "
                            + describe(props.tripWindow()), traffic);
        }
        LocalDateTime estimated = departure.plusMinutes(traffic.currentMinutes());
        int late = (int) Math.max(0, Duration.between(trip.getArrivalTime(), estimated).toMinutes());
        return new TripTrafficResponse(tripId, departure, trip.getArrivalTime(), estimated, late, null, traffic);
    }

    /** For managers: where traffic comes from and how much of today's allowance is used. */
    public Status status() {
        return new Status(props.effectiveMode().name(), provider.billed(), quota.usedToday(),
                quota.limit() < 0 ? null : quota.limit(), props.cacheTtl().toMinutes(), cache.size());
    }

    public record Status(String provider, boolean billed, int requestsToday, Integer dailyLimit,
                         long cacheMinutes, int routesCached) {}

    private TrafficResponse lookup(RoutePoints points) {
        Cached hit = cache.get(points.routeId());
        if (isFresh(hit)) return toResponse(points, hit, false, null);

        synchronized (locks.computeIfAbsent(points.routeId(), id -> new Object())) {
            hit = cache.get(points.routeId());
            if (isFresh(hit)) return toResponse(points, hit, false, null); // someone else just fetched it

            if (provider.billed() && !quota.tryUse()) {
                log.warn("Live traffic daily limit ({}) reached", quota.limit());
                return staleOrFail(points, hit, "Today's live-traffic limit has been reached");
            }
            try {
                TrafficReading reading = provider.read(points);
                Cached fresh = new Cached(reading, provider.source(), LocalDateTime.now(clock));
                cache.put(points.routeId(), fresh);
                return toResponse(points, fresh, false, null);
            } catch (TrafficLookupException e) {
                return staleOrFail(points, hit, e.getMessage());
            }
        }
    }

    private boolean isFresh(Cached c) {
        return c != null && c.checkedAt().plus(props.cacheTtl()).isAfter(LocalDateTime.now(clock));
    }

    private TrafficResponse staleOrFail(RoutePoints points, Cached last, String why) {
        if (last == null) throw new ApiException(ErrorCode.TRAFFIC_UNAVAILABLE, why);
        return toResponse(points, last, true, why + "; showing the reading from " + last.checkedAt().toLocalTime().withNano(0));
    }

    private static TrafficResponse toResponse(RoutePoints p, Cached c, boolean stale, String note) {
        TrafficReading r = c.reading();
        int typical = minutes(r.typicalSeconds());
        int current = minutes(r.currentSeconds());
        Double km = r.distanceMeters() == null ? null : Math.round(r.distanceMeters() / 100.0) / 10.0;
        return new TrafficResponse(p.routeId(), p.origin(), p.destination(), p.originStation(),
                p.destinationStation(), c.source(), p.scheduledMinutes(), typical, current,
                Math.max(0, current - typical), TrafficLevel.of(r.currentSeconds(), r.typicalSeconds()), km,
                r.encodedPolyline(), r.speedIntervals(), c.checkedAt(), stale, note);
    }

    private static int minutes(long seconds) {
        return (int) Math.round(seconds / 60.0);
    }

    private static String describe(Duration d) {
        long h = d.toHours();
        return h > 0 && d.toMinutesPart() == 0 ? h + (h == 1 ? " hour" : " hours") : d.toMinutes() + " minutes";
    }
}
