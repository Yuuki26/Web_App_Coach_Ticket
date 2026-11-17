package com.busbooking.service;

import com.busbooking.dto.BookingLookupRequest;
import com.busbooking.dto.BookingRequest;
import com.busbooking.dto.BookingResponse;
import com.busbooking.dto.QuoteRequest;
import com.busbooking.dto.QuoteResponse;
import com.busbooking.exception.BookingException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.Booking;
import com.busbooking.model.BookingStatus;
import com.busbooking.model.PaymentMethod;
import com.busbooking.model.Trip;
import com.busbooking.repository.BookingRepository;
import com.busbooking.repository.UserRepository;
import com.busbooking.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TripService tripService;
    private final PricingService pricingService;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, TripService tripService,
                          PricingService pricingService, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.tripService = tripService;
        this.pricingService = pricingService;
        this.userRepository = userRepository;
    }

    /** Price breakdown for the chosen seats, without booking anything. */
    @Transactional(readOnly = true)
    public QuoteResponse quote(QuoteRequest req) {
        Trip trip = tripService.getTrip(req.tripId());
        return pricingService.quote(trip, validSeats(req.seats()), req.promoCode());
    }

    /**
     * Books the seats. The trip row is locked for the length of the transaction, so if two
     * requests want the same seat, the second one waits and then sees the seat as taken.
     * Payment itself is not processed (demo): this is where a payment provider call would go.
     */
    @Transactional
    public BookingResponse create(BookingRequest req) {
        Trip trip = tripService.getTripForUpdate(req.tripId());

        if (trip.getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BookingException(ErrorCode.TRIP_DEPARTED, "This trip has already departed");
        }

        List<String> seats = validSeats(req.seats());
        List<String> taken = tripService.bookedSeats(trip.getId()).stream()
                .filter(seats::contains)
                .sorted(SeatLayout.ORDER)
                .toList();
        if (!taken.isEmpty()) {
            throw new BookingException(ErrorCode.SEAT_TAKEN, "Seat " + String.join(", ", taken)
                    + (taken.size() > 1 ? " were" : " was") + " just booked by someone else. Please pick again");
        }

        QuoteResponse price = pricingService.quote(trip, seats, req.promoCode());
        Booking booking = new Booking(
                newBookingCode(), trip, req.passengerName().trim(), req.email().trim(), req.phone(), seats,
                price.fare(), price.discount(), price.tax(), price.total(), price.promoCode(),
                req.paymentMethod() == null ? PaymentMethod.CARD : req.paymentMethod());
        // Logged-in customers get the booking on their account; guests keep using code + email.
        CurrentUser.id().map(userRepository::getReferenceById).ifPresent(booking::setCustomer);

        return BookingResponse.from(bookingRepository.save(booking));
    }

    /** Bookings made while logged in to this account, newest first. */
    @Transactional(readOnly = true)
    public List<BookingResponse> mine(Long userId) {
        return bookingRepository.findByCustomerIdOrderByCreatedAtDesc(userId).stream()
                .map(BookingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse lookup(BookingLookupRequest req) {
        return BookingResponse.from(find(req));
    }

    @Transactional
    public BookingResponse cancel(BookingLookupRequest req) {
        Booking booking = find(req);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("This booking is already cancelled");
        }
        if (booking.getTrip().getDepartureTime().isBefore(LocalDateTime.now())) {
            throw new BookingException(ErrorCode.TRIP_DEPARTED, "This trip has already departed and can't be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(booking);
    }

    /** Same error whether the code or the email is wrong, so codes can't be guessed one part at a time. */
    private Booking find(BookingLookupRequest req) {
        String code = req.bookingCode().trim().toUpperCase(Locale.ROOT);
        return bookingRepository.findByBookingCode(code)
                .filter(b -> b.getEmail().equalsIgnoreCase(req.email().trim()))
                .orElseThrow(() -> new NotFoundException("No booking found. Check the code and email"));
    }

    private List<String> validSeats(List<String> requested) {
        List<String> seats = requested.stream().map(s -> s.trim().toUpperCase(Locale.ROOT)).toList();
        for (String seat : seats) {
            if (!SeatLayout.isValid(seat)) throw new BookingException("Seat " + seat + " does not exist");
        }
        if (new HashSet<>(seats).size() != seats.size()) {
            throw new BookingException("The same seat was selected twice");
        }
        return seats.stream().sorted(SeatLayout.ORDER).toList();
    }

    private String newBookingCode() {
        String code;
        do {
            code = "HC-" + ThreadLocalRandom.current().nextInt(10000, 100000);
        } while (bookingRepository.existsByBookingCode(code));
        return code;
    }
}
