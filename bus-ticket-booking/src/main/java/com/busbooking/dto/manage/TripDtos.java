package com.busbooking.dto.manage;

import com.busbooking.dto.AuditInfo;
import com.busbooking.model.CoachType;
import com.busbooking.model.Trip;
import com.busbooking.model.TripStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class TripDtos {

    private TripDtos() {}

    public record Request(
            @NotNull Long routeId,
            @NotNull Long coachId,
            @NotNull LocalDateTime departureTime,
            @NotNull LocalDateTime arrivalTime,
            @NotNull @DecimalMin("10000") @DecimalMax("10000000") BigDecimal price
    ) {
        /** Cross-field rule: arrival is 10 minutes to 48 hours after departure. */
        @AssertTrue(message = "arrival must be between 10 minutes and 48 hours after departure")
        public boolean isArrivalAfterDeparture() {
            if (departureTime == null || arrivalTime == null) return true;
            Duration d = Duration.between(departureTime, arrivalTime);
            return d.toMinutes() >= 10 && d.toHours() <= 48;
        }
    }

    /** Departure dates are whole days. Prices in VND. */
    public record Filter(Long routeId, String origin, String destination, Long operatorId, Long coachId,
                         CoachType coachType, TripStatus status, LocalDate departureFrom, LocalDate departureTo,
                         BigDecimal minPrice, BigDecimal maxPrice) {}

    public record Response(Long id, Long routeId, String origin, String destination, Long coachId,
                           String coachPlate, Long operatorId, String operatorName, CoachType coachType,
                           LocalDateTime departureTime, LocalDateTime arrivalTime, BigDecimal price,
                           TripStatus status, int capacity, int seatsBooked, int seatsLeft, AuditInfo audit) {
        public static Response from(Trip t, int seatsBooked) {
            var coach = t.getCoach();
            return new Response(t.getId(), t.getRoute().getId(), t.getRoute().getOrigin(),
                    t.getRoute().getDestination(), coach.getId(), coach.getPlateNumber(),
                    coach.getOperator().getId(), coach.getOperator().getName(), coach.getType(),
                    t.getDepartureTime(), t.getArrivalTime(), t.getPrice(), t.getStatus(), coach.capacity(),
                    seatsBooked, coach.capacity() - seatsBooked, AuditInfo.of(t));
        }
    }

    /** Result of cancelling a trip: the trip and how many bookings were cancelled with it. */
    public record CancelResult(Response trip, int cancelledBookings) {}
}
