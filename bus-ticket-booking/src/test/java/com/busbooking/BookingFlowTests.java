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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static com.busbooking.config.DataSeeder.HCMC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("h2") // tests use the in-memory database, not MySQL
class BookingFlowTests {

    @Autowired TripService tripService;
    @Autowired BookingService bookingService;

    private TripResponse anyTrip(String from, String to) {
        List<TripResponse> trips = tripService.search(from, to, LocalDate.now().plusDays(3));
        assertEquals(6, trips.size());
        return trips.get(0);
    }

    private String freeSeat(TripResponse trip, int minRow) {
        var map = tripService.seatMap(trip.id());
        return SeatLayout.allSeats(map.rows()).stream()
                .filter(s -> SeatLayout.rowOf(s) >= minRow && !map.bookedSeats().contains(s))
                .findFirst().orElseThrow();
    }

    private BookingRequest request(Long tripId, String email, String... seats) {
        return new BookingRequest(tripId, "Test Rider", email, null, List.of(seats), null, PaymentMethod.CARD);
    }

    @Test
    void priceIsFarePlusLegroomMinusPromoPlusTax() {
        TripResponse trip = anyTrip(HCMC, "Rạch Giá");

        // One legroom seat (+30.000 ₫) and one normal seat, 10% off, then 8% VAT, whole đồng.
        QuoteResponse quote = bookingService.quote(new QuoteRequest(trip.id(), List.of("1A", "5B"), "firstride"));
        BigDecimal fare = trip.price().multiply(BigDecimal.valueOf(2)).add(new BigDecimal("30000"));

        assertEquals(0, fare.compareTo(quote.fare()));
        assertTrue(quote.promoApplied());
        assertEquals(0, quote.discount().compareTo(fare.multiply(new BigDecimal("0.10")).setScale(0, RoundingMode.HALF_UP)));
        assertEquals(quote.fare().subtract(quote.discount()).add(quote.tax()), quote.total());

        QuoteResponse badCode = bookingService.quote(new QuoteRequest(trip.id(), List.of("5B"), "NOPE"));
        assertFalse(badCode.promoApplied());
        assertEquals(0, badCode.discount().signum());
    }

    @Test
    void promoCodeRulesAreChecked() {
        TripResponse cheap = anyTrip(HCMC, "Tây Ninh"); // ~110.000 ₫ a seat

        // WEEKEND needs a fare of 300.000 ₫ or more.
        QuoteResponse oneSeat = bookingService.quote(new QuoteRequest(cheap.id(), List.of("5A"), "WEEKEND"));
        assertFalse(oneSeat.promoApplied());
        assertTrue(oneSeat.promoMessage().contains("at least"));
        QuoteResponse fourSeats = bookingService.quote(
                new QuoteRequest(cheap.id(), List.of("5A", "5B", "5C", "5D"), "WEEKEND"));
        assertTrue(fourSeats.promoApplied());

        QuoteResponse notYet = bookingService.quote(new QuoteRequest(cheap.id(), List.of("5A"), "TET2027"));
        assertFalse(notYet.promoApplied());
        assertTrue(notYet.promoMessage().startsWith("This code can be used from"));

        QuoteResponse expired = bookingService.quote(new QuoteRequest(cheap.id(), List.of("5A"), "HELLOSAIGON"));
        assertFalse(expired.promoApplied());
        assertTrue(expired.promoMessage().startsWith("This code expired"));
    }

    @Test
    void aSeatCanOnlyBeBookedOnceUntilItIsCancelled() {
        TripResponse trip = anyTrip(HCMC, "Vũng Tàu");
        String seat = freeSeat(trip, 3);

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
        // 165.000 ₫ fare + 8% VAT
        assertEquals(0, new BigDecimal("178200").compareTo(sample.total()));

        assertThrows(NotFoundException.class,
                () -> bookingService.lookup(new BookingLookupRequest("HC-48213", "someone@else.com")));
    }

    @Test
    void unknownSeatsAndTooManySeatsAreRejected() {
        TripResponse trip = anyTrip(HCMC, "Bến Tre");
        assertThrows(BookingException.class, () -> bookingService.create(request(trip.id(), "a@example.com", "13A")));
        assertThrows(BookingException.class, () -> bookingService.create(request(trip.id(), "a@example.com", "3E")));
        assertThrows(BookingException.class, () -> bookingService.create(
                request(trip.id(), "a@example.com", "3A", "3B", "3C", "3D", "4A", "4B", "4C")));
    }
}
