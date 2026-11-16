package com.busbooking.dto;

import com.busbooking.model.CoachType;
import com.busbooking.model.Trip;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripResponse(
        Long id,
        String origin,
        String destination,
        String operatorName,
        String operatorColor,
        double rating,
        CoachType coachType,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        int durationMinutes,
        BigDecimal price,
        int seatsLeft
) {
    public static TripResponse from(Trip trip, int seatsLeft) {
        return new TripResponse(
                trip.getId(),
                trip.getRoute().getOrigin(),
                trip.getRoute().getDestination(),
                trip.getOperator().getName(),
                trip.getOperator().getColor(),
                trip.getOperator().getRating(),
                trip.getCoachType(),
                trip.getDepartureTime(),
                trip.getArrivalTime(),
                trip.getRoute().getDurationMinutes(),
                trip.getPrice(),
                seatsLeft
        );
    }
}
