package com.busbooking.service.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.BookingAdminDtos;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.Booking;
import com.busbooking.repository.BookingRepository;
import com.busbooking.repository.Specs;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;

/** Read-only booking search for staff. */
@Service
public class BookingAdminService {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "createdAt", "createdAt", "departureTime", "trip.departureTime", "total", "total",
            "passengerName", "passengerName", "status", "status", "bookingCode", "bookingCode", "id", "id");

    private final BookingRepository bookings;

    public BookingAdminService(BookingRepository bookings) {
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingAdminDtos.Response> list(BookingAdminDtos.Filter f, PageParams paging) {
        Specification<Booking> spec = Specs.<Booking>containsAny(f.q(), "bookingCode", "passengerName", "email")
                .and(Specs.equalTo("status", f.status()))
                .and(Specs.equalTo("trip.id", f.tripId()))
                .and(Specs.equalTo("trip.route.id", f.routeId()))
                .and(Specs.equalTo("trip.coach.operator.id", f.operatorId()))
                .and(Specs.onOrAfterDay("trip.departureTime", f.departureFrom()))
                .and(Specs.onOrBeforeDay("trip.departureTime", f.departureTo()))
                .and(Specs.onOrAfterDay("createdAt", f.createdFrom()))
                .and(Specs.onOrBeforeDay("createdAt", f.createdTo()));
        return PageResponse.of(bookings.findAll(spec, paging.toPageable(SORT_FIELDS, Sort.by("createdAt").descending())),
                BookingAdminDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public BookingAdminDtos.Response get(String code) {
        return bookings.findByBookingCode(code.trim().toUpperCase(Locale.ROOT))
                .map(BookingAdminDtos.Response::from)
                .orElseThrow(() -> new NotFoundException("Booking " + code + " not found"));
    }
}
