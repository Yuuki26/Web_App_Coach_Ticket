package com.busbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuoteRequest(
        @NotNull Long tripId,
        @NotEmpty List<@NotBlank String> seats,
        String promoCode
) {
}
