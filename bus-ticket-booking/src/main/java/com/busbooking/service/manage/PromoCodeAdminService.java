package com.busbooking.service.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.PromoCodeDtos;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.AuditAction;
import com.busbooking.model.PromoCode;
import com.busbooking.repository.PromoCodeRepository;
import com.busbooking.repository.Specs;
import com.busbooking.service.AuditService;
import com.busbooking.service.Changes;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

/** Discount codes. A code that has been used can't be renamed, and its limit can't drop below its uses. */
@Service
public class PromoCodeAdminService {

    static final String ENTITY = "PromoCode";
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "code", "code", "discountPercent", "discountPercent", "validFrom", "validFrom", "validTo", "validTo",
            "usedCount", "usedCount", "createdAt", "createdAt", "id", "id");

    private final PromoCodeRepository promoCodes;
    private final AuditService audit;

    public PromoCodeAdminService(PromoCodeRepository promoCodes, AuditService audit) {
        this.promoCodes = promoCodes;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<PromoCodeDtos.Response> list(PromoCodeDtos.Filter f, PageParams paging) {
        Specification<PromoCode> spec = Specs.<PromoCode>containsAny(f.q(), "code", "description")
                .and(Specs.equalTo("active", f.active()))
                .and(validOn(f.validOn()))
                .and(Specs.atLeast("discountPercent", f.minDiscount()))
                .and(Specs.atMost("discountPercent", f.maxDiscount()));
        return PageResponse.of(promoCodes.findAll(spec, paging.toPageable(SORT_FIELDS, Sort.by("code"))),
                PromoCodeDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public PromoCodeDtos.Response get(Long id) {
        return PromoCodeDtos.Response.from(find(id));
    }

    @Transactional
    public PromoCodeDtos.Response create(PromoCodeDtos.Request req) {
        String code = req.code().toUpperCase(Locale.ROOT);
        if (promoCodes.existsByCode(code)) throw duplicate(code);
        PromoCode p = promoCodes.save(new PromoCode(code, req.description().trim(), req.discountPercent(),
                req.validFrom(), req.validTo(), req.usageLimit(), req.minFare()));
        audit.record(ENTITY, p.getId(), AuditAction.CREATE, code + " " + p.getDiscountPercent() + "%");
        return PromoCodeDtos.Response.from(p);
    }

    @Transactional
    public PromoCodeDtos.Response update(Long id, PromoCodeDtos.Request req) {
        PromoCode p = find(id);
        String code = req.code().toUpperCase(Locale.ROOT);
        if (!code.equals(p.getCode())) {
            if (p.getUsedCount() > 0) {
                throw new ApiException(ErrorCode.IN_USE, "This code has been used " + p.getUsedCount()
                        + " time(s), so it can't be renamed. Create a new code instead");
            }
            if (promoCodes.existsByCode(code)) throw duplicate(code);
        }
        if (req.usageLimit() != null && req.usageLimit() < p.getUsedCount()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED,
                    "Usage limit can't be lower than the " + p.getUsedCount() + " time(s) it has already been used");
        }
        Changes c = new Changes();
        p.setCode(c.track("code", p.getCode(), code));
        p.setDescription(c.track("description", p.getDescription(), req.description().trim()));
        p.setDiscountPercent(c.track("discountPercent", p.getDiscountPercent(), req.discountPercent()));
        p.setValidFrom(c.track("validFrom", p.getValidFrom(), req.validFrom()));
        p.setValidTo(c.track("validTo", p.getValidTo(), req.validTo()));
        p.setUsageLimit(c.track("usageLimit", p.getUsageLimit(), req.usageLimit()));
        p.setMinFare(c.track("minFare", p.getMinFare(), req.minFare()));
        if (!c.isEmpty()) audit.record(ENTITY, id, AuditAction.UPDATE, c.toString());
        return PromoCodeDtos.Response.from(p);
    }

    /** Soft delete: the code stops working but past bookings keep their link to it. */
    @Transactional
    public PromoCodeDtos.Response deactivate(Long id) {
        PromoCode p = find(id);
        if (p.isActive()) {
            p.setActive(false);
            audit.record(ENTITY, id, AuditAction.DELETE, p.getCode());
        }
        return PromoCodeDtos.Response.from(p);
    }

    @Transactional
    public PromoCodeDtos.Response restore(Long id) {
        PromoCode p = find(id);
        if (!p.isActive()) {
            p.setActive(true);
            audit.record(ENTITY, id, AuditAction.RESTORE, p.getCode());
        }
        return PromoCodeDtos.Response.from(p);
    }

    private PromoCode find(Long id) {
        return promoCodes.findById(id).orElseThrow(() -> new NotFoundException("Promo code " + id + " not found"));
    }

    /** Codes whose validity period includes the given day (open ends count as unlimited). */
    private static Specification<PromoCode> validOn(LocalDate day) {
        if (day == null) return Specs.all();
        return (root, query, cb) -> cb.and(
                cb.or(cb.isNull(root.get("validFrom")), cb.lessThanOrEqualTo(root.get("validFrom"), day)),
                cb.or(cb.isNull(root.get("validTo")), cb.greaterThanOrEqualTo(root.get("validTo"), day)));
    }

    private static ApiException duplicate(String code) {
        return new ApiException(ErrorCode.DUPLICATE, "Promo code " + code + " already exists");
    }
}
