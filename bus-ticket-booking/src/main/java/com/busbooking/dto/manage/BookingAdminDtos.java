package com.busbooking.dto.manage;

import com.busbooking.dto.AuditInfo;
import com.busbooking.dto.BookingResponse;
import com.busbooking.model.Booking;
import com.busbooking.model.BookingStatus;
import com.busbooking.model.PaymentMethod;

import java.time.LocalDate;

public final class BookingAdminDtos {

    private BookingAdminDtos() {}

    /** {@code q} matches booking code, passenger name or email. Dates are whole days. */
    public record Filter(String q, BookingStatus status, Long tripId, Long routeId, Long operatorId,
                         LocalDate departureFrom, LocalDate departureTo, LocalDate createdFrom, LocalDate createdTo) {}

    /** A booking as staff see it: the rider's view plus contact, payment and account details. */
    public record Response(BookingResponse booking, Long tripId, String phone, PaymentMethod paymentMethod,
                           String promoCode, String accountEmail, AuditInfo audit) {
        public static Response from(Booking b) {
            return new Response(BookingResponse.from(b), b.getTrip().getId(), b.getPhone(), b.getPaymentMethod(),
                    b.getPromoCode() == null ? null : b.getPromoCode().getCode(),
                    b.getCustomer() == null ? null : b.getCustomer().getEmail(), AuditInfo.of(b));
        }
    }
}
