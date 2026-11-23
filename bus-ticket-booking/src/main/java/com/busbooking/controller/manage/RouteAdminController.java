package com.busbooking.controller.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.RouteDtos;
import com.busbooking.service.manage.RouteAdminService;
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

/** Routes. Managers and admins. DELETE is a soft delete; POST /{id}/restore undoes it. */
@RestController
@RequestMapping("/api/v1/manage/routes")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class RouteAdminController {

    private final RouteAdminService service;

    public RouteAdminController(RouteAdminService service) {
        this.service = service;
    }

    /**
     * Filters: q, origin, destination, active, popular, minPrice, maxPrice, maxDurationMinutes.
     * Sort: origin, destination, distanceKm, durationMinutes, fromPrice, createdAt.
     */
    @GetMapping
    public PageResponse<RouteDtos.Response> list(@ParameterObject RouteDtos.Filter filter,
                                                 @ParameterObject PageParams paging) {
        return service.list(filter, paging);
    }

    @GetMapping("/{id}")
    public RouteDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteDtos.Response create(@Valid @RequestBody RouteDtos.Request req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public RouteDtos.Response update(@PathVariable Long id, @Valid @RequestBody RouteDtos.Request req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public RouteDtos.Response delete(@PathVariable Long id) {
        return service.deactivate(id);
    }

    @PostMapping("/{id}/restore")
    public RouteDtos.Response restore(@PathVariable Long id) {
        return service.restore(id);
    }
}
