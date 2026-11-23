package com.busbooking.service.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.RouteDtos;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.AuditAction;
import com.busbooking.model.Route;
import com.busbooking.model.TripStatus;
import com.busbooking.repository.RouteRepository;
import com.busbooking.repository.Specs;
import com.busbooking.repository.TripRepository;
import com.busbooking.service.AuditService;
import com.busbooking.service.Changes;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/** Routes between cities. A route with upcoming trips can't be removed. */
@Service
public class RouteAdminService {

    static final String ENTITY = "Route";
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "origin", "origin", "destination", "destination", "distanceKm", "distanceKm",
            "durationMinutes", "durationMinutes", "fromPrice", "fromPrice", "createdAt", "createdAt", "id", "id");

    private final RouteRepository routes;
    private final TripRepository trips;
    private final AuditService audit;

    public RouteAdminService(RouteRepository routes, TripRepository trips, AuditService audit) {
        this.routes = routes;
        this.trips = trips;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<RouteDtos.Response> list(RouteDtos.Filter f, PageParams paging) {
        Specification<Route> spec = Specs.<Route>containsAny(f.q(), "origin", "destination", "originStation",
                        "destinationStation")
                .and(Specs.equalsIgnoreCase("origin", f.origin()))
                .and(Specs.equalsIgnoreCase("destination", f.destination()))
                .and(Specs.equalTo("active", f.active()))
                .and(Specs.equalTo("popular", f.popular()))
                .and(Specs.atLeast("fromPrice", f.minPrice()))
                .and(Specs.atMost("fromPrice", f.maxPrice()))
                .and(Specs.atMost("durationMinutes", f.maxDurationMinutes()));
        Sort defaults = Sort.by("origin").and(Sort.by("destination"));
        return PageResponse.of(routes.findAll(spec, paging.toPageable(SORT_FIELDS, defaults)), this::toResponse);
    }

    @Transactional(readOnly = true)
    public RouteDtos.Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public RouteDtos.Response create(RouteDtos.Request req) {
        String origin = req.origin().trim();
        String destination = req.destination().trim();
        if (routes.existsByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination)) {
            throw duplicate(origin, destination);
        }
        Route route = routes.save(new Route(origin, destination, req.originStation().trim(),
                req.destinationStation().trim(), req.distanceKm(), req.durationMinutes(), req.fromPrice(),
                req.isPopular()));
        audit.record(ENTITY, route.getId(), AuditAction.CREATE, origin + " → " + destination);
        return toResponse(route);
    }

    @Transactional
    public RouteDtos.Response update(Long id, RouteDtos.Request req) {
        Route route = find(id);
        String origin = req.origin().trim();
        String destination = req.destination().trim();
        boolean renamed = !origin.equalsIgnoreCase(route.getOrigin()) || !destination.equalsIgnoreCase(route.getDestination());
        if (renamed && routes.existsByOriginIgnoreCaseAndDestinationIgnoreCase(origin, destination)) {
            throw duplicate(origin, destination);
        }
        if (renamed && upcomingTrips(route) > 0) {
            throw new ApiException(ErrorCode.IN_USE,
                    "This route has upcoming trips, so its cities can't change. Create a new route instead");
        }
        Changes c = new Changes();
        route.setOrigin(c.track("origin", route.getOrigin(), origin));
        route.setDestination(c.track("destination", route.getDestination(), destination));
        route.setOriginStation(c.track("originStation", route.getOriginStation(), req.originStation().trim()));
        route.setDestinationStation(c.track("destinationStation", route.getDestinationStation(),
                req.destinationStation().trim()));
        route.setDistanceKm(c.track("distanceKm", route.getDistanceKm(), req.distanceKm()));
        route.setDurationMinutes(c.track("durationMinutes", route.getDurationMinutes(), req.durationMinutes()));
        route.setFromPrice(c.track("fromPrice", route.getFromPrice(), req.fromPrice()));
        route.setPopular(c.track("popular", route.isPopular(), req.isPopular()));
        if (!c.isEmpty()) audit.record(ENTITY, id, AuditAction.UPDATE, c.toString());
        return toResponse(route);
    }

    /** Soft delete: hidden from search; not allowed while trips are still scheduled on it. */
    @Transactional
    public RouteDtos.Response deactivate(Long id) {
        Route route = find(id);
        long upcoming = upcomingTrips(route);
        if (upcoming > 0) {
            throw new ApiException(ErrorCode.IN_USE,
                    "This route has " + upcoming + " upcoming trip(s). Cancel them before removing the route");
        }
        if (route.isActive()) {
            route.setActive(false);
            audit.record(ENTITY, id, AuditAction.DELETE, route.getOrigin() + " → " + route.getDestination());
        }
        return toResponse(route);
    }

    @Transactional
    public RouteDtos.Response restore(Long id) {
        Route route = find(id);
        if (!route.isActive()) {
            route.setActive(true);
            audit.record(ENTITY, id, AuditAction.RESTORE, route.getOrigin() + " → " + route.getDestination());
        }
        return toResponse(route);
    }

    Route find(Long id) {
        return routes.findById(id).orElseThrow(() -> new NotFoundException("Route " + id + " not found"));
    }

    private long upcomingTrips(Route route) {
        return trips.countByRouteAndStatusAndDepartureTimeAfter(route, TripStatus.SCHEDULED, LocalDateTime.now());
    }

    private static ApiException duplicate(String origin, String destination) {
        return new ApiException(ErrorCode.DUPLICATE, "A route " + origin + " → " + destination + " already exists");
    }

    private RouteDtos.Response toResponse(Route r) {
        return RouteDtos.Response.from(r, upcomingTrips(r));
    }
}
