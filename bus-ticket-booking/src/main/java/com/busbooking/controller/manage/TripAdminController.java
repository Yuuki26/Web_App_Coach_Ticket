package com.busbooking.controller.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.TripDtos;
import com.busbooking.service.manage.TripAdminService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Trip scheduling. Managers and admins. DELETE cancels the trip and all its bookings. */
@RestController
@RequestMapping("/api/v1/manage/trips")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class TripAdminController {

    private final TripAdminService service;

    public TripAdminController(TripAdminService service) {
        this.service = service;
    }

    /**
     * Filters: routeId, origin, destination, operatorId, coachId, coachType, status, departureFrom,
     * departureTo, minPrice, maxPrice. Sort: departureTime, arrivalTime, price, status, origin,
     * destination, operator, createdAt.
     */
    @GetMapping
    public PageResponse<TripDtos.Response> list(@ParameterObject TripDtos.Filter filter,
                                                @ParameterObject PageParams paging) {
        return service.list(filter, paging);
    }

    @GetMapping("/{id}")
    public TripDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripDtos.Response create(@Valid @RequestBody TripDtos.Request req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public TripDtos.Response update(@PathVariable Long id, @Valid @RequestBody TripDtos.Request req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public TripDtos.CancelResult cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return service.cancel(id, reason);
    }
}
