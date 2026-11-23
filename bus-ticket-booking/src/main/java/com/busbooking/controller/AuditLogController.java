package com.busbooking.controller;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.model.AuditAction;
import com.busbooking.model.AuditLog;
import com.busbooking.repository.AuditLogRepository;
import com.busbooking.repository.Specs;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/** The audit trail of staff changes, newest first. Admins only. */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "createdAt", "createdAt", "entityType", "entityType", "actor", "actor", "action", "action", "id", "id");

    private final AuditLogRepository repository;

    public AuditLogController(AuditLogRepository repository) {
        this.repository = repository;
    }

    public record Filter(String entityType, Long entityId, String actor, AuditAction action,
                         LocalDate from, LocalDate to) {}

    public record Entry(Long id, LocalDateTime createdAt, String actor, AuditAction action, String entityType,
                        Long entityId, String details) {
        static Entry from(AuditLog a) {
            return new Entry(a.getId(), a.getCreatedAt(), a.getActor(), a.getAction(), a.getEntityType(),
                    a.getEntityId(), a.getDetails());
        }
    }

    /** Filters: entityType, entityId, actor, action, from, to. Sort: createdAt (default, newest first), entityType, actor, action. */
    @GetMapping
    @Transactional(readOnly = true)
    public PageResponse<Entry> list(@ParameterObject Filter f, @ParameterObject PageParams paging) {
        Specification<AuditLog> spec = Specs.<AuditLog>equalsIgnoreCase("entityType", f.entityType())
                .and(Specs.equalTo("entityId", f.entityId()))
                .and(Specs.equalsIgnoreCase("actor", f.actor()))
                .and(Specs.equalTo("action", f.action()))
                .and(Specs.onOrAfterDay("createdAt", f.from()))
                .and(Specs.onOrBeforeDay("createdAt", f.to()));
        Sort newestFirst = Sort.by("createdAt").descending().and(Sort.by("id").descending());
        return PageResponse.of(repository.findAll(spec, paging.toPageable(SORT_FIELDS, newestFirst)), Entry::from);
    }
}
