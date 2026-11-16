package com.busbooking.dto;

import java.math.BigDecimal;
import java.util.List;

/** Everything the seat-selection screen needs to draw the coach. */
public record SeatMapResponse(
        Long tripId,
        int rows,
        List<String> columns,
        int legroomRows,
        BigDecimal legroomSurcharge,
        List<String> bookedSeats
) {
}
