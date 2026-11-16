package com.busbooking.dto;

import com.busbooking.model.Booking;
import com.busbooking.model.BookingStatus;
import com.busbooking.model.Trip;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        String bookingCode,
        String status,
        String passengerName,
        String email,
        List<String> seats,
        BigDecimal fare,
        BigDecimal discount,
        BigDecimal tax,
        BigDecimal total,
        String origin,
        String destination,
        String operatorName,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime
) {
    public static BookingResponse from(Booking b) {
        Trip t = b.getTrip();
        return new BookingResponse(
                b.getBookingCode(),
                displayStatus(b),
                b.getPassengerName(),
                b.getEmail(),
                List.copyOf(b.getSeats()),
                b.getFare(),
                b.getDiscount(),
                b.getTax(),
                b.getTotal(),
                t.getRoute().getOrigin(),
                t.getRoute().getDestination(),
                t.getOperator().getName(),
                t.getDepartureTime(),
                t.getArrivalTime()
        );
    }

    /** Upcoming / Completed / Cancelled, as shown on the My Trips tabs. */
    private static String displayStatus(Booking b) {
        if (b.getStatus() == BookingStatus.CANCELLED) return "Cancelled";
        return b.getTrip().getDepartureTime().isBefore(LocalDateTime.now()) ? "Completed" : "Upcoming";
    }
}
