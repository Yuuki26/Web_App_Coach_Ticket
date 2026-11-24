package com.busbooking.controller.manage;

import com.busbooking.dto.PageParams;
import com.busbooking.dto.PageResponse;
import com.busbooking.dto.manage.BookingAdminDtos;
import com.busbooking.service.manage.BookingAdminService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** All bookings, for staff. Managers and admins. */
@RestController
@RequestMapping("/api/v1/manage/bookings")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class BookingAdminController {

    private final BookingAdminService service;

    public BookingAdminController(BookingAdminService service) {
        this.service = service;
    }

    /**
     * Filters: q, status, tripId, routeId, operatorId, departureFrom, departureTo, createdFrom, createdTo.
     * Sort: createdAt (default, newest first), departureTime, total, passengerName, status, bookingCode.
     */
    @GetMapping
    public PageResponse<BookingAdminDtos.Response> list(@ParameterObject BookingAdminDtos.Filter filter,
                                                        @ParameterObject PageParams paging) {
        return service.list(filter, paging);
    }

    @GetMapping("/{code}")
    public BookingAdminDtos.Response get(@PathVariable String code) {
        return service.get(code);
    }
}
