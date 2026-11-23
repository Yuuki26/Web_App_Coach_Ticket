package com.busbooking.service;

import com.busbooking.model.AuditAction;
import com.busbooking.model.AuditLog;
import com.busbooking.repository.AuditLogRepository;
import com.busbooking.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the audit trail. Runs inside the caller's transaction, so a change and its audit line
 * are saved together or not at all.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, AuditAction action, String details) {
        String actor = CurrentUser.email().orElse(CurrentUser.SYSTEM);
        repository.save(new AuditLog(entityType, entityId, action, actor, details));
        log.info("{} {} {} by {}{}", action, entityType, entityId, actor, details == null ? "" : ": " + details);
    }
}
