package com.busbooking.dto;

import com.busbooking.model.Route;

import java.math.BigDecimal;

public record PopularRouteResponse(Long id, String origin, String destination, int durationMinutes, BigDecimal fromPrice) {
    public static PopularRouteResponse from(Route r) {
        return new PopularRouteResponse(r.getId(), r.getOrigin(), r.getDestination(), r.getDurationMinutes(), r.getFromPrice());
    }
}
