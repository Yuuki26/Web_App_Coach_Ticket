package com.busbooking;

import com.busbooking.dto.BookingLookupRequest;
import com.busbooking.dto.BookingRequest;
import com.busbooking.dto.BookingResponse;
import com.busbooking.dto.QuoteRequest;
import com.busbooking.dto.QuoteResponse;
import com.busbooking.dto.TripResponse;
import com.busbooking.exception.BookingException;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.PaymentMethod;
import com.busbooking.service.BookingService;
import com.busbooking.service.SeatLayout;
import com.busbooking.service.TripService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BookingFlowTests {

    @Autowired TripService tripService;
    @Autowired BookingService bookingService;

    private TripResponse anyTrip(String from, String to) {
        List<TripResponse> trips = tripService.search(from, to, LocalDate.now().plusDays(3));
        assertEquals(6, trips.size());
        return trips.get(0);
    }

    private String freeSeat(Long tripId, int minRow) {
        List<String> booked = tripService.seatMap(tripId).bookedSeats();
        return SeatLayout.allSeats().stream()
                .filter(s -> SeatLayout.rowOf(s) >= minRow && !booked.contains(s))
                .findFirst().orElseThrow();
    }

    private BookingRequest request(Long tripId, String email, String... seats) {
        return new BookingRequest(tripId, "Test Rider", email, null, List.of(seats), null, PaymentMethod.CARD);
    }

    @Test
    void priceIsFarePlusLegroomMinusPromoPlusTax() {
        TripResponse trip = anyTrip("Boston", "Miami");

        // One legroom seat (+$5) and one normal seat, 10% off, then 8% tax.
        QuoteResponse quote = bookingService.quote(new QuoteRequest(trip.id(), List.of("1A", "5B"), "firstride"));
        BigDecimal fare = trip.price().multiply(BigDecimal.valueOf(2)).add(new BigDecimal("5.00"));

        assertEquals(0, fare.compareTo(quote.fare()));
        assertTrue(quote.promoApplied());
        assertEquals(quote.fare().subtract(quote.discount()).add(quote.tax()), quote.total());
        assertEquals(0, quote.discount().compareTo(fare.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP)));

        QuoteResponse badCode = bookingService.quote(new QuoteRequest(trip.id(), List.of("5B"), "NOPE"));
        assertFalse(badCode.promoApplied());
        assertEquals(0, badCode.discount().signum());
    }

    @Test
    void aSeatCanOnlyBeBookedOnceUntilItIsCancelled() {
        TripResponse trip = anyTrip("New York", "Boston");
        String seat = freeSeat(trip.id(), 3);

        BookingResponse first = bookingService.create(request(trip.id(), "first@example.com", seat));
        assertEquals("Upcoming", first.status());
        assertTrue(tripService.seatMap(trip.id()).bookedSeats().contains(seat));

        assertThrows(BookingException.class, () -> bookingService.create(request(trip.id(), "second@example.com", seat)));

        BookingLookupRequest ref = new BookingLookupRequest(first.bookingCode(), "FIRST@example.com");
        assertEquals("Cancelled", bookingService.cancel(ref).status());
        assertFalse(tripService.seatMap(trip.id()).bookedSeats().contains(seat));
        assertThrows(BookingException.class, () -> bookingService.cancel(ref));

        bookingService.create(request(trip.id(), "second@example.com", seat));
    }

    @Test
    void lookupNeedsBothCodeAndEmail() {
        BookingResponse sample = bookingService.lookup(new BookingLookupRequest("hc-48213", "sam@example.com"));
        assertEquals("Completed", sample.status());
        assertEquals(0, new BigDecimal("23.76").compareTo(sample.total()));

        assertThrows(NotFoundException.class,
                () -> bookingService.lookup(new BookingLookupRequest("HC-48213", "someone@else.com")));
    }

    @Test
    void unknownSeatsAreRejected() {
        TripResponse trip = anyTrip("Seattle", "Portland");
        assertThrows(BookingException.class, () -> bookingService.create(request(trip.id(), "a@example.com", "10A")));
        assertThrows(BookingException.class, () -> bookingService.create(request(trip.id(), "a@example.com", "3E")));
    }
}
