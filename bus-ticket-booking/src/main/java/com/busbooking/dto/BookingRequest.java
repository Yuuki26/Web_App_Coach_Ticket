package com.busbooking.dto;

import com.busbooking.model.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookingRequest(
        @NotNull Long tripId,
        @NotBlank @Size(max = 100) String passengerName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 30) String phone,
        @NotEmpty List<@NotBlank String> seats,
        String promoCode,
        PaymentMethod paymentMethod
) {
}
