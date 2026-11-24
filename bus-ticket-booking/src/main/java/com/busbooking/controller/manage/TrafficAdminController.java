package com.busbooking.controller.manage;

import com.busbooking.traffic.TrafficService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lets managers see whether live traffic is on and how much of today's Google allowance is used. */
@RestController
@RequestMapping("/api/v1/manage/traffic")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class TrafficAdminController {

    private final TrafficService service;

    public TrafficAdminController(TrafficService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public TrafficService.Status status() {
        return service.status();
    }
}
