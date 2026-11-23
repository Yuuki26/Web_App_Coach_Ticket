package com.busbooking.dto.manage;

import com.busbooking.dto.AuditInfo;
import com.busbooking.model.PromoCode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PromoCodeDtos {

    private PromoCodeDtos() {}

    /** Leave validFrom / validTo / usageLimit / minFare empty for "no limit". */
    public record Request(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{3,30}$", message = "must be 3-30 letters or digits") String code,
            @NotBlank @Size(max = 200) String description,
            @NotNull @Min(1) @Max(100) Integer discountPercent,
            LocalDate validFrom,
            LocalDate validTo,
            @Min(1) Integer usageLimit,
            @DecimalMin("0") BigDecimal minFare
    ) {
        @AssertTrue(message = "validTo must be on or after validFrom")
        public boolean isValidPeriod() {
            return validFrom == null || validTo == null || !validTo.isBefore(validFrom);
        }
    }

    /** {@code q} matches code or description; {@code validOn} keeps codes whose dates include that day. */
    public record Filter(String q, Boolean active, LocalDate validOn, Integer minDiscount, Integer maxDiscount) {}

    public record Response(Long id, String code, String description, int discountPercent, LocalDate validFrom,
                           LocalDate validTo, Integer usageLimit, int usedCount, BigDecimal minFare, boolean active,
                           AuditInfo audit) {
        public static Response from(PromoCode p) {
            return new Response(p.getId(), p.getCode(), p.getDescription(), p.getDiscountPercent(), p.getValidFrom(),
                    p.getValidTo(), p.getUsageLimit(), p.getUsedCount(), p.getMinFare(), p.isActive(), AuditInfo.of(p));
        }
    }
}
