package com.busbooking.controller.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.OperatorDtos;
import com.busbooking.service.manage.OperatorAdminService;
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

/** Coach companies. Managers and admins. DELETE is a soft delete; POST /{id}/restore undoes it. */
@RestController
@RequestMapping("/api/v1/manage/operators")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class OperatorAdminController {

    private final OperatorAdminService service;

    public OperatorAdminController(OperatorAdminService service) {
        this.service = service;
    }

    /** Filters: q, active, minRating, maxRating, createdFrom, createdTo. Sort: name, rating, createdAt, updatedAt. */
    @GetMapping
    public PageResponse<OperatorDtos.Response> list(@ParameterObject OperatorDtos.Filter filter,
                                                    @ParameterObject PageParams paging) {
        return service.list(filter, paging);
    }

    @GetMapping("/{id}")
    public OperatorDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OperatorDtos.Response create(@Valid @RequestBody OperatorDtos.Request req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public OperatorDtos.Response update(@PathVariable Long id, @Valid @RequestBody OperatorDtos.Request req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public OperatorDtos.Response delete(@PathVariable Long id) {
        return service.deactivate(id);
    }

    @PostMapping("/{id}/restore")
    public OperatorDtos.Response restore(@PathVariable Long id) {
        return service.restore(id);
    }
}
