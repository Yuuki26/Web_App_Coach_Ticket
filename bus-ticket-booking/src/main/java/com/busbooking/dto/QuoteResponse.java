package com.busbooking.dto;

import java.math.BigDecimal;

/** Price breakdown. promoMessage is null when no code was entered. */
public record QuoteResponse(
        BigDecimal fare,
        BigDecimal discount,
        BigDecimal tax,
        BigDecimal total,
        boolean promoApplied,
        String promoCode,
        String promoMessage
) {
}
