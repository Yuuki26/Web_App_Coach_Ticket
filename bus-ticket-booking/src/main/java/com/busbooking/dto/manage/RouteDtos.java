package com.busbooking.dto.manage;

import com.busbooking.dto.AuditInfo;
import com.busbooking.model.Route;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class RouteDtos {

    private RouteDtos() {}

    public record Request(
            @NotBlank @Size(max = 80) String origin,
            @NotBlank @Size(max = 80) String destination,
            @NotBlank @Size(max = 120) String originStation,
            @NotBlank @Size(max = 120) String destinationStation,
            @NotNull @Min(1) @Max(3000) Integer distanceKm,
            @NotNull @Min(10) @Max(2880) Integer durationMinutes,
            @NotNull @DecimalMin("10000") @DecimalMax("10000000") BigDecimal fromPrice,
            /** Optional; false when left out. */
            Boolean popular,
            /** Optional map positions of the two stations, used for live traffic. Give both lat and lng or neither. */
            @DecimalMin("-90") @DecimalMax("90") Double originLat,
            @DecimalMin("-180") @DecimalMax("180") Double originLng,
            @DecimalMin("-90") @DecimalMax("90") Double destinationLat,
            @DecimalMin("-180") @DecimalMax("180") Double destinationLng
    ) {
        public boolean isPopular() {
            return Boolean.TRUE.equals(popular);
        }

        @AssertTrue(message = "give both originLat and originLng, or neither")
        public boolean isOriginPositionComplete() {
            return (originLat == null) == (originLng == null);
        }

        @AssertTrue(message = "give both destinationLat and destinationLng, or neither")
        public boolean isDestinationPositionComplete() {
            return (destinationLat == null) == (destinationLng == null);
        }

        /** Cross-field rule: a route must go somewhere else. */
        @AssertTrue(message = "origin and destination must be different cities")
        public boolean isDifferentCities() {
            return origin == null || destination == null || !origin.trim().equalsIgnoreCase(destination.trim());
        }
    }

    /** {@code q} matches either city or either station name. Prices in VND. */
    public record Filter(String q, String origin, String destination, Boolean active, Boolean popular,
                         BigDecimal minPrice, BigDecimal maxPrice, Integer maxDurationMinutes) {}

    public record Response(Long id, String origin, String destination, String originStation,
                           String destinationStation, int distanceKm, int durationMinutes, BigDecimal fromPrice,
                           boolean popular, Double originLat, Double originLng, Double destinationLat,
                           Double destinationLng, boolean active, long upcomingTrips, AuditInfo audit) {
        public static Response from(Route r, long upcomingTrips) {
            return new Response(r.getId(), r.getOrigin(), r.getDestination(), r.getOriginStation(),
                    r.getDestinationStation(), r.getDistanceKm(), r.getDurationMinutes(), r.getFromPrice(),
                    r.isPopular(), r.getOriginLat(), r.getOriginLng(), r.getDestinationLat(),
                    r.getDestinationLng(), r.isActive(), upcomingTrips, AuditInfo.of(r));
        }
    }
}
