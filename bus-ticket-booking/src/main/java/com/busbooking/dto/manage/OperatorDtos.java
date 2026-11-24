package com.busbooking.dto.manage;

import com.busbooking.dto.AuditInfo;
import com.busbooking.model.Operator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class OperatorDtos {

    private OperatorDtos() {}

    /** Body for create (POST) and full update (PUT). */
    public record Request(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a colour like #0F4C81") String color,
            @NotNull @DecimalMin("1.0") @DecimalMax("5.0") Double rating,
            @Pattern(regexp = "^[+0-9 ()\\-]{4,30}$", message = "must be a phone number") String hotline,
            @Email @Size(max = 150) String email
    ) {}

    /**
     * Query filters: {@code q} matches name, email or hotline.
     */
    public record Filter(String q, Boolean active, Double minRating, Double maxRating,
                         LocalDate createdFrom, LocalDate createdTo) {}

    public record Response(Long id, String name, String color, double rating, String hotline, String email,
                           boolean active, long activeCoaches, AuditInfo audit) {
        public static Response from(Operator o, long activeCoaches) {
            return new Response(o.getId(), o.getName(), o.getColor(), o.getRating(), o.getHotline(), o.getEmail(),
                    o.isActive(), activeCoaches, AuditInfo.of(o));
        }
    }
}
