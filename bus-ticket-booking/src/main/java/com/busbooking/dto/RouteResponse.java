package com.busbooking.dto;

import com.busbooking.model.Route;

import java.math.BigDecimal;

/**
 * A route as shown on the public Routes page. Station positions (WGS84) are for placing the stations on
 * a map and may be null.
 */
public record RouteResponse(Long id, String origin, String destination, String originStation,
                            String destinationStation, int distanceKm, int durationMinutes, BigDecimal fromPrice,
                            boolean popular, Double originLat, Double originLng, Double destinationLat,
                            Double destinationLng) {
    public static RouteResponse from(Route r) {
        return new RouteResponse(r.getId(), r.getOrigin(), r.getDestination(), r.getOriginStation(),
                r.getDestinationStation(), r.getDistanceKm(), r.getDurationMinutes(), r.getFromPrice(),
                r.isPopular(), r.getOriginLat(), r.getOriginLng(), r.getDestinationLat(), r.getDestinationLng());
    }
}
