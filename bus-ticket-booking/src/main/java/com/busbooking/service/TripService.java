package com.busbooking.service;

import com.busbooking.dto.PopularRouteResponse;
import com.busbooking.dto.SeatMapResponse;
import com.busbooking.dto.TripResponse;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.BookingStatus;
import com.busbooking.model.Trip;
import com.busbooking.repository.BookingRepository;
import com.busbooking.repository.RouteRepository;
import com.busbooking.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final BookingRepository bookingRepository;

    public TripService(TripRepository tripRepository, RouteRepository routeRepository,
                       BookingRepository bookingRepository) {
        this.tripRepository = tripRepository;
        this.routeRepository = routeRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<String> cities() {
        return routeRepository.findCities();
    }

    public List<PopularRouteResponse> popularRoutes() {
        return routeRepository.findByPopularTrueAndActiveTrueOrderById().stream().map(PopularRouteResponse::from).toList();
    }

    /** Trips on the given day that have not departed yet. */
    public List<TripResponse> search(String origin, String destination, LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = date.atStartOfDay().isBefore(now) ? now : date.atStartOfDay();
        return tripRepository.search(origin.trim(), destination.trim(), from, date.plusDays(1).atStartOfDay())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TripResponse findById(Long id) {
        return toResponse(getTrip(id));
    }

    public SeatMapResponse seatMap(Long tripId) {
        Trip trip = getTrip(tripId);
        List<String> booked = bookedSeats(tripId).stream().sorted(SeatLayout.ORDER).toList();
        return new SeatMapResponse(tripId, trip.getCoach().getSeatRows(), SeatLayout.COLUMNS,
                SeatLayout.LEGROOM_ROWS, SeatLayout.LEGROOM_SURCHARGE, booked);
    }

    Trip getTrip(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Trip " + id + " not found"));
    }

    Trip getTripForUpdate(Long id) {
        return tripRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Trip " + id + " not found"));
    }

    List<String> bookedSeats(Long tripId) {
        return bookingRepository.findSeats(tripId, BookingStatus.CONFIRMED);
    }

    private TripResponse toResponse(Trip trip) {
        return TripResponse.from(trip, trip.getCoach().capacity() - bookedSeats(trip.getId()).size());
    }
}
