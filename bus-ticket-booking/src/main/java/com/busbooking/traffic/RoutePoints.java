package com.busbooking.traffic;

import com.busbooking.model.Route;

/** What a traffic provider needs to know about a route, copied out of the entity. */
public record RoutePoints(Long routeId, String origin, String destination, String originStation,
                          String destinationStation, Double originLat, Double originLng,
                          Double destinationLat, Double destinationLng, int distanceKm, int scheduledMinutes) {

    public static RoutePoints from(Route r) {
        return new RoutePoints(r.getId(), r.getOrigin(), r.getDestination(), r.getOriginStation(),
                r.getDestinationStation(), r.getOriginLat(), r.getOriginLng(), r.getDestinationLat(),
                r.getDestinationLng(), r.getDistanceKm(), r.getDurationMinutes());
    }

    public boolean hasOriginPosition() {
        return originLat != null && originLng != null;
    }

    public boolean hasDestinationPosition() {
        return destinationLat != null && destinationLng != null;
    }
}
