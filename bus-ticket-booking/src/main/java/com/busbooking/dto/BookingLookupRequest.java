package com.busbooking.dto;

import jakarta.validation.constraints.NotBlank;

/** A booking is only shown or cancelled when both the code and the email match. */
public record BookingLookupRequest(@NotBlank String bookingCode, @NotBlank String email) {
}
