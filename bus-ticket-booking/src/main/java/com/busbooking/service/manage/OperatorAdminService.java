package com.busbooking.service.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.OperatorDtos;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.AuditAction;
import com.busbooking.model.CoachStatus;
import com.busbooking.model.Operator;
import com.busbooking.repository.CoachRepository;
import com.busbooking.repository.OperatorRepository;
import com.busbooking.repository.Specs;
import com.busbooking.service.AuditService;
import com.busbooking.service.Changes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Coach companies, managed by managers and admins. */
@Service
public class OperatorAdminService {

    static final String ENTITY = "Operator";
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "name", "name", "rating", "rating", "createdAt", "createdAt", "updatedAt", "updatedAt", "id", "id");

    private final OperatorRepository operators;
    private final CoachRepository coaches;
    private final AuditService audit;

    public OperatorAdminService(OperatorRepository operators, CoachRepository coaches, AuditService audit) {
        this.operators = operators;
        this.coaches = coaches;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<OperatorDtos.Response> list(OperatorDtos.Filter f, PageParams paging) {
        Specification<Operator> spec = Specs.<Operator>containsAny(f.q(), "name", "email", "hotline")
                .and(Specs.equalTo("active", f.active()))
                .and(Specs.atLeast("rating", f.minRating()))
                .and(Specs.atMost("rating", f.maxRating()))
                .and(Specs.onOrAfterDay("createdAt", f.createdFrom()))
                .and(Specs.onOrBeforeDay("createdAt", f.createdTo()));
        Page<Operator> page = operators.findAll(spec, paging.toPageable(SORT_FIELDS, Sort.by("name")));
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public OperatorDtos.Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public OperatorDtos.Response create(OperatorDtos.Request req) {
        String name = req.name().trim();
        if (operators.existsByNameIgnoreCase(name)) {
            throw new ApiException(ErrorCode.DUPLICATE, "An operator named " + name + " already exists");
        }
        Operator op = operators.save(new Operator(name, req.color().toUpperCase(), req.rating(),
                blankToNull(req.hotline()), blankToNull(req.email())));
        audit.record(ENTITY, op.getId(), AuditAction.CREATE, op.getName());
        return toResponse(op);
    }

    @Transactional
    public OperatorDtos.Response update(Long id, OperatorDtos.Request req) {
        Operator op = find(id);
        String name = req.name().trim();
        if (!name.equalsIgnoreCase(op.getName()) && operators.existsByNameIgnoreCase(name)) {
            throw new ApiException(ErrorCode.DUPLICATE, "An operator named " + name + " already exists");
        }
        Changes c = new Changes();
        op.setName(c.track("name", op.getName(), name));
        op.setColor(c.track("color", op.getColor(), req.color().toUpperCase()));
        op.setRating(c.track("rating", op.getRating(), req.rating()));
        op.setHotline(c.track("hotline", op.getHotline(), blankToNull(req.hotline())));
        op.setEmail(c.track("email", op.getEmail(), blankToNull(req.email())));
        if (!c.isEmpty()) audit.record(ENTITY, id, AuditAction.UPDATE, c.toString());
        return toResponse(op);
    }

    /** Soft delete. Not allowed while the operator still has coaches in service. */
    @Transactional
    public OperatorDtos.Response deactivate(Long id) {
        Operator op = find(id);
        long inService = coaches.countByOperatorAndStatusNot(op, CoachStatus.RETIRED);
        if (inService > 0) {
            throw new ApiException(ErrorCode.IN_USE,
                    op.getName() + " still has " + inService + " coach(es) in service. Retire them first");
        }
        if (op.isActive()) {
            op.setActive(false);
            audit.record(ENTITY, id, AuditAction.DELETE, op.getName());
        }
        return toResponse(op);
    }

    @Transactional
    public OperatorDtos.Response restore(Long id) {
        Operator op = find(id);
        if (!op.isActive()) {
            op.setActive(true);
            audit.record(ENTITY, id, AuditAction.RESTORE, op.getName());
        }
        return toResponse(op);
    }

    Operator find(Long id) {
        return operators.findById(id).orElseThrow(() -> new NotFoundException("Operator " + id + " not found"));
    }

    private OperatorDtos.Response toResponse(Operator op) {
        return OperatorDtos.Response.from(op, coaches.countByOperatorAndStatusNot(op, CoachStatus.RETIRED));
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
