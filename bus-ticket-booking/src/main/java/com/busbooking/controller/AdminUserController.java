package com.busbooking.controller;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.UserAdminDtos;
import com.busbooking.service.manage.UserAdminService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** User management for admins. Also protected by the /api/v1/admin/** rule in SecurityConfig. */
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService service;

    public AdminUserController(UserAdminService service) {
        this.service = service;
    }

    /** Filters: q, role, status, createdFrom, createdTo. Sort: email, fullName, role, status, createdAt, lastLoginAt. */
    @GetMapping
    public PageResponse<UserAdminDtos.Response> list(@ParameterObject UserAdminDtos.Filter filter,
                                                     @ParameterObject PageParams paging) {
        return service.list(filter, paging);
    }

    @GetMapping("/{id}")
    public UserAdminDtos.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    /** Creates an active, verified account with any role (e.g. a new driver or manager). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAdminDtos.Response create(@Valid @RequestBody UserAdminDtos.Create req) {
        return service.create(req);
    }

    @PatchMapping("/{id}/role")
    public UserAdminDtos.Response changeRole(@PathVariable Long id, @Valid @RequestBody UserAdminDtos.ChangeRole req) {
        return service.changeRole(id, req.role());
    }

    @PostMapping("/{id}/deactivate")
    public UserAdminDtos.Response deactivate(@PathVariable Long id) {
        return service.deactivate(id);
    }

    @PostMapping("/{id}/reactivate")
    public UserAdminDtos.Response reactivate(@PathVariable Long id) {
        return service.reactivate(id);
    }
}
