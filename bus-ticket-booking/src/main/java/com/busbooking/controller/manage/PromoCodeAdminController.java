package com.busbooking.controller.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.PromoCodeDtos;
import com.busbooking.service.manage.PromoCodeAdminService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Discount codes. Managers and admins. DELETE switches a code off; POST /{id}/restore turns it back on. */
@RestController
@RequestMapping("/api/v1/manage/promo-codes")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class PromoCodeAdminController {

    private final PromoCodeAdminService service;

    public PromoCodeAdminController(PromoCodeAdminService service) {
        this.service = service;
    }

    /** Filters: q, active, validOn, minDiscount, maxDiscount. Sort: code, discountPercent, validFrom, validTo, usedCount, createdAt. */
    @GetMapping
    public PageResponse<PromoCodeDtos.Response> list(@ParameterObject PromoCodeDtos.Filter filter,
                                                     @ParameterObject PageParams paging) {
        return service.list(filter, paging);
    }

    @GetMapping("/{id}")
    public PromoCodeDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromoCodeDtos.Response create(@Valid @RequestBody PromoCodeDtos.Request req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public PromoCodeDtos.Response update(@PathVariable Long id, @Valid @RequestBody PromoCodeDtos.Request req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public PromoCodeDtos.Response delete(@PathVariable Long id) {
        return service.deactivate(id);
    }

    @PostMapping("/{id}/restore")
    public PromoCodeDtos.Response restore(@PathVariable Long id) {
        return service.restore(id);
    }
}
