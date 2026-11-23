package com.busbooking.dto;

import com.busbooking.model.CoachType;
import com.busbooking.model.Trip;
import com.busbooking.model.TripStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TripResponse(
        Long id,
        String origin,
        String destination,
        String originStation,
        String destinationStation,
        String operatorName,
        String operatorColor,
        double rating,
        CoachType coachType,
        String coachPlate,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        int durationMinutes,
        BigDecimal price,
        int totalSeats,
        int seatsLeft,
        TripStatus status
) {
    public static TripResponse from(Trip trip, int seatsLeft) {
        var route = trip.getRoute();
        var coach = trip.getCoach();
        return new TripResponse(
                trip.getId(),
                route.getOrigin(),
                route.getDestination(),
                route.getOriginStation(),
                route.getDestinationStation(),
                coach.getOperator().getName(),
                coach.getOperator().getColor(),
                coach.getOperator().getRating(),
                coach.getType(),
                coach.getPlateNumber(),
                trip.getDepartureTime(),
                trip.getArrivalTime(),
                (int) java.time.Duration.between(trip.getDepartureTime(), trip.getArrivalTime()).toMinutes(),
                trip.getPrice(),
                coach.capacity(),
                seatsLeft,
                trip.getStatus()
        );
    }
}
