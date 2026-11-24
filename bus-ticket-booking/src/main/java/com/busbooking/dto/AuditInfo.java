package com.busbooking.dto;

import com.busbooking.model.Auditable;

import java.time.LocalDateTime;

/** Who created and last changed a record, and when. Included in every management response. */
public record AuditInfo(LocalDateTime createdAt, String createdBy, LocalDateTime updatedAt, String updatedBy) {
    public static AuditInfo of(Auditable a) {
        return new AuditInfo(a.getCreatedAt(), a.getCreatedBy(), a.getUpdatedAt(), a.getUpdatedBy());
    }
}
