package com.busbooking.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A discount code riders can enter at checkout, e.g. FIRSTRIDE for 10% off. */
@Entity
@Table(name = "promo_codes")
public class PromoCode extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Always upper-case. */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String description;

    /** 1–100. */
    @Column(nullable = false)
    private int discountPercent;

    /** First and last day the code works; null = no limit on that side. */
    private LocalDate validFrom;
    private LocalDate validTo;

    /** How many bookings may use it in total; null = unlimited. */
    private Integer usageLimit;

    @Column(nullable = false)
    private int usedCount;

    /** Minimum fare (VND, before discount) for the code to apply; null = any fare. */
    @Column(precision = 12, scale = 0)
    private BigDecimal minFare;

    /** False = switched off (soft delete). */
    @Column(nullable = false)
    private boolean active = true;

    protected PromoCode() {}

    public PromoCode(String code, String description, int discountPercent, LocalDate validFrom, LocalDate validTo,
                     Integer usageLimit, BigDecimal minFare) {
        this.code = code;
        this.description = description;
        this.discountPercent = discountPercent;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.usageLimit = usageLimit;
        this.minFare = minFare;
    }

    public void markUsed() { usedCount++; }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public int getUsedCount() { return usedCount; }
    public BigDecimal getMinFare() { return minFare; }
    public void setMinFare(BigDecimal minFare) { this.minFare = minFare; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
