package com.busbooking.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One line per important change made by staff: who did what to which record, and when.
 * Rows are only ever inserted, never changed. Not linked by foreign key on purpose, so the
 * history survives even if the record it describes is removed.
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(columnList = "entity_type, entity_id"),
        @Index(columnList = "created_at")})
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    /** Email of the user who did it, or "system". */
    @Column(nullable = false, length = 150)
    private String actor;

    /** Short human-readable description, e.g. "status ACTIVE -> MAINTENANCE". */
    @Column(length = 1000)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AuditLog() {}

    public AuditLog(String entityType, Long entityId, AuditAction action, String actor, String details) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actor = actor;
        this.details = details == null || details.length() <= 1000 ? details : details.substring(0, 997) + "...";
    }

    public Long getId() { return id; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public AuditAction getAction() { return action; }
    public String getActor() { return actor; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
