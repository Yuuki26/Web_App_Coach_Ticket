package com.busbooking.controller;

import com.busbooking.dto.BookingLookupRequest;
import com.busbooking.dto.BookingRequest;
import com.busbooking.dto.BookingResponse;
import com.busbooking.dto.QuoteRequest;
import com.busbooking.dto.QuoteResponse;
import com.busbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lookup and cancel use POST with the email in the body (not the URL),
 * so email addresses don't end up in browser history or server logs.
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** Price breakdown for the selected seats and promo code. */
    @PostMapping("/quote")
    public QuoteResponse quote(@Valid @RequestBody QuoteRequest request) {
        return bookingService.quote(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody BookingRequest request) {
        return bookingService.create(request);
    }

    /** "Find my booking": needs both the booking code and the email used to book. */
    @PostMapping("/lookup")
    public BookingResponse lookup(@Valid @RequestBody BookingLookupRequest request) {
        return bookingService.lookup(request);
    }

    @PostMapping("/cancel")
    public BookingResponse cancel(@Valid @RequestBody BookingLookupRequest request) {
        return bookingService.cancel(request);
    }
}
