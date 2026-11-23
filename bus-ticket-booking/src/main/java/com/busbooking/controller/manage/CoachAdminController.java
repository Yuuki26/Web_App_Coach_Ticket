package com.busbooking.controller.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.CoachDtos;
import com.busbooking.service.manage.CoachAdminService;
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

/** The fleet. Managers and admins. DELETE retires a coach; POST /{id}/restore puts it back in service. */
@RestController
@RequestMapping("/api/v1/manage/coaches")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class CoachAdminController {

    private final CoachAdminService service;

    public CoachAdminController(CoachAdminService service) {
        this.service = service;
    }

    /**
     * Filters: q (plate), operatorId, type, status, minSeats, maxSeats, yearFrom, yearTo.
     * Sort: plateNumber, operator, type, seatRows, manufactureYear, status, createdAt.
     */
    @GetMapping
    public PageResponse<CoachDtos.Response> list(@ParameterObject CoachDtos.Filter filter,
                                                 @ParameterObject PageParams paging) {
        return service.list(filter, paging);
    }

    @GetMapping("/{id}")
    public CoachDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CoachDtos.Response create(@Valid @RequestBody CoachDtos.Request req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public CoachDtos.Response update(@PathVariable Long id, @Valid @RequestBody CoachDtos.Request req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public CoachDtos.Response delete(@PathVariable Long id) {
        return service.retire(id);
    }

    @PostMapping("/{id}/restore")
    public CoachDtos.Response restore(@PathVariable Long id) {
        return service.restore(id);
    }
}
