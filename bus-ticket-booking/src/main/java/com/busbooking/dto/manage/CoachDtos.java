package com.busbooking.dto.manage;

import com.busbooking.dto.AuditInfo;
import com.busbooking.model.Coach;
import com.busbooking.model.CoachStatus;
import com.busbooking.model.CoachType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class CoachDtos {

    private CoachDtos() {}

    /**
     * Body for create and update. {@code status} may be ACTIVE or MAINTENANCE; retiring a coach is DELETE.
     * Manufacture year can't be in the future (checked in the service).
     */
    public record Request(
            @NotBlank @Pattern(regexp = "^\\d{2}[A-Z]{1,2}-\\d{3}\\.\\d{2}$",
                    message = "must look like 51B-123.45") String plateNumber,
            @NotNull Long operatorId,
            @NotNull CoachType type,
            @NotNull @Min(3) @Max(12) Integer seatRows,
            @NotNull @Min(1990) Integer manufactureYear,
            CoachStatus status
    ) {}

    /** {@code q} matches the plate. Seats filter on capacity (rows × 4). */
    public record Filter(String q, Long operatorId, CoachType type, CoachStatus status,
                         Integer minSeats, Integer maxSeats, Integer yearFrom, Integer yearTo) {}

    public record Response(Long id, String plateNumber, Long operatorId, String operatorName, CoachType type,
                           int seatRows, int capacity, int manufactureYear, CoachStatus status,
                           long upcomingTrips, AuditInfo audit) {
        public static Response from(Coach c, long upcomingTrips) {
            return new Response(c.getId(), c.getPlateNumber(), c.getOperator().getId(), c.getOperator().getName(),
                    c.getType(), c.getSeatRows(), c.capacity(), c.getManufactureYear(), c.getStatus(),
                    upcomingTrips, AuditInfo.of(c));
        }
    }
}
