package com.busbooking.service.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.TripDtos;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.AuditAction;
import com.busbooking.model.Booking;
import com.busbooking.model.BookingStatus;
import com.busbooking.model.Coach;
import com.busbooking.model.CoachStatus;
import com.busbooking.model.Route;
import com.busbooking.model.Trip;
import com.busbooking.model.TripStatus;
import com.busbooking.repository.BookingRepository;
import com.busbooking.repository.Specs;
import com.busbooking.repository.TripRepository;
import com.busbooking.service.AuditService;
import com.busbooking.service.Changes;
import com.busbooking.service.SeatLayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduling trips. Rules:
 * <ul>
 *   <li>route active; coach in service and its operator active; departure in the future</li>
 *   <li>a coach can't be on two scheduled trips at overlapping times</li>
 *   <li>once seats are sold the route can't change, and a new coach must have every sold seat</li>
 *   <li>cancelling a trip cancels all its confirmed bookings (cascade)</li>
 * </ul>
 */
@Service
public class TripAdminService {

    static final String ENTITY = "Trip";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "departureTime", "departureTime", "arrivalTime", "arrivalTime", "price", "price",
            "status", "status", "origin", "route.origin", "destination", "route.destination",
            "operator", "coach.operator.name", "createdAt", "createdAt", "id", "id");

    private final TripRepository trips;
    private final BookingRepository bookings;
    private final RouteAdminService routes;
    private final CoachAdminService coaches;
    private final AuditService audit;

    public TripAdminService(TripRepository trips, BookingRepository bookings, RouteAdminService routes,
                            CoachAdminService coaches, AuditService audit) {
        this.trips = trips;
        this.bookings = bookings;
        this.routes = routes;
        this.coaches = coaches;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<TripDtos.Response> list(TripDtos.Filter f, PageParams paging) {
        Specification<Trip> spec = Specs.<Trip>equalTo("route.id", f.routeId())
                .and(Specs.equalsIgnoreCase("route.origin", f.origin()))
                .and(Specs.equalsIgnoreCase("route.destination", f.destination()))
                .and(Specs.equalTo("coach.operator.id", f.operatorId()))
                .and(Specs.equalTo("coach.id", f.coachId()))
                .and(Specs.equalTo("coach.type", f.coachType()))
                .and(Specs.equalTo("status", f.status()))
                .and(Specs.onOrAfterDay("departureTime", f.departureFrom()))
                .and(Specs.onOrBeforeDay("departureTime", f.departureTo()))
                .and(Specs.atLeast("price", f.minPrice()))
                .and(Specs.atMost("price", f.maxPrice()));
        Page<Trip> page = trips.findAll(spec, paging.toPageable(SORT_FIELDS, Sort.by("departureTime")));

        // One query for all seat counts on the page instead of one per trip.
        Map<Long, Integer> booked = new HashMap<>();
        List<Long> ids = page.getContent().stream().map(Trip::getId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : bookings.countSeatsByTrip(ids, BookingStatus.CONFIRMED)) {
                booked.put((Long) row[0], ((Number) row[1]).intValue());
            }
        }
        return PageResponse.of(page, t -> TripDtos.Response.from(t, booked.getOrDefault(t.getId(), 0)));
    }

    @Transactional(readOnly = true)
    public TripDtos.Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public TripDtos.Response create(TripDtos.Request req) {
        Route route = activeRoute(req.routeId());
        Coach coach = coachInService(req.coachId());
        if (!req.departureTime().isAfter(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Departure must be in the future");
        }
        checkCoachFree(coach, req.departureTime(), req.arrivalTime(), -1L);
        Trip trip = trips.save(new Trip(route, coach, req.departureTime(), req.arrivalTime(), req.price()));
        audit.record(ENTITY, trip.getId(), AuditAction.CREATE, route.getOrigin() + " → " + route.getDestination()
                + " " + trip.getDepartureTime().format(WHEN) + " on " + coach.getPlateNumber());
        return toResponse(trip);
    }

    @Transactional
    public TripDtos.Response update(Long id, TripDtos.Request req) {
        Trip trip = trips.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        if (trip.getStatus() != TripStatus.SCHEDULED) {
            throw new ApiException(ErrorCode.TRIP_CANCELLED, "A cancelled trip can't be edited");
        }
        if (!trip.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.TRIP_DEPARTED, "This trip has already departed");
        }
        if (!req.departureTime().isAfter(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Departure must be in the future");
        }

        List<String> sold = bookings.findSeats(id, BookingStatus.CONFIRMED);
        boolean routeChanged = !req.routeId().equals(trip.getRoute().getId());
        boolean coachChanged = !req.coachId().equals(trip.getCoach().getId());
        if (routeChanged && !sold.isEmpty()) {
            throw new ApiException(ErrorCode.IN_USE,
                    sold.size() + " seat(s) are sold on this trip, so its route can't change. Cancel it instead");
        }
        Route route = routeChanged ? activeRoute(req.routeId()) : trip.getRoute();
        Coach coach = coachChanged ? coachInService(req.coachId()) : trip.getCoach();
        if (coachChanged) {
            List<String> missing = sold.stream()
                    .filter(s -> !SeatLayout.isValid(s, coach.getSeatRows()))
                    .sorted(SeatLayout.ORDER).toList();
            if (!missing.isEmpty()) {
                throw new ApiException(ErrorCode.IN_USE, "Coach " + coach.getPlateNumber()
                        + " has no seat " + String.join(", ", missing) + ", which is already sold");
            }
        }
        checkCoachFree(coach, req.departureTime(), req.arrivalTime(), id);

        Changes c = new Changes();
        if (routeChanged) {
            c.track("route", trip.getRoute().getOrigin() + " → " + trip.getRoute().getDestination(),
                    route.getOrigin() + " → " + route.getDestination());
            trip.setRoute(route);
        }
        if (coachChanged) {
            c.track("coach", trip.getCoach().getPlateNumber(), coach.getPlateNumber());
            trip.setCoach(coach);
        }
        trip.setDepartureTime(c.track("departureTime", trip.getDepartureTime(), req.departureTime()));
        trip.setArrivalTime(c.track("arrivalTime", trip.getArrivalTime(), req.arrivalTime()));
        trip.setPrice(c.track("price", trip.getPrice(), req.price()));
        if (!c.isEmpty()) audit.record(ENTITY, id, AuditAction.UPDATE, c.toString());
        return toResponse(trip);
    }

    /** Cancels the trip and every confirmed booking on it. */
    @Transactional
    public TripDtos.CancelResult cancel(Long id, String reason) {
        Trip trip = trips.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        if (trip.getStatus() == TripStatus.CANCELLED) {
            throw new ApiException(ErrorCode.TRIP_CANCELLED, "This trip is already cancelled");
        }
        if (!trip.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new ApiException(ErrorCode.TRIP_DEPARTED, "This trip has already departed and can't be cancelled");
        }
        List<Booking> affected = bookings.findByTripAndStatus(trip, BookingStatus.CONFIRMED);
        affected.forEach(b -> b.setStatus(BookingStatus.CANCELLED));
        trip.setStatus(TripStatus.CANCELLED);
        audit.record(ENTITY, id, AuditAction.CANCEL, affected.size() + " booking(s) cancelled"
                + (reason == null || reason.isBlank() ? "" : ". Reason: " + reason.trim()));
        return new TripDtos.CancelResult(toResponse(trip), affected.size());
    }

    private void checkCoachFree(Coach coach, LocalDateTime departure, LocalDateTime arrival, Long excludeId) {
        List<Trip> clash = trips.findOverlapping(coach, departure, arrival, excludeId);
        if (!clash.isEmpty()) {
            Trip other = clash.get(0);
            throw new ApiException(ErrorCode.SCHEDULE_CONFLICT, "Coach " + coach.getPlateNumber()
                    + " is already on trip " + other.getId() + " (" + other.getRoute().getOrigin() + " → "
                    + other.getRoute().getDestination() + ", " + other.getDepartureTime().format(WHEN) + "–"
                    + other.getArrivalTime().format(WHEN) + ")");
        }
    }

    private Route activeRoute(Long id) {
        Route route = routes.find(id);
        if (!route.isActive()) throw new ApiException(ErrorCode.CONFLICT, "This route has been removed");
        return route;
    }

    private Coach coachInService(Long id) {
        Coach coach = coaches.find(id);
        if (coach.getStatus() != CoachStatus.ACTIVE) {
            throw new ApiException(ErrorCode.CONFLICT,
                    "Coach " + coach.getPlateNumber() + " is " + coach.getStatus().name().toLowerCase() + ", not in service");
        }
        if (!coach.getOperator().isActive()) {
            throw new ApiException(ErrorCode.CONFLICT, coach.getOperator().getName() + " has been removed");
        }
        return coach;
    }

    private Trip find(Long id) {
        return trips.findById(id).orElseThrow(() -> notFound(id));
    }

    private static NotFoundException notFound(Long id) {
        return new NotFoundException("Trip " + id + " not found");
    }

    private TripDtos.Response toResponse(Trip t) {
        return TripDtos.Response.from(t, bookings.findSeats(t.getId(), BookingStatus.CONFIRMED).size());
    }
}
