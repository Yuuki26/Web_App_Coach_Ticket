package com.busbooking.controller;

import com.busbooking.dto.PopularRouteResponse;
import com.busbooking.service.TripService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RouteController {

    private final TripService tripService;

    public RouteController(TripService tripService) {
        this.tripService = tripService;
    }

    /** City names for the From / To dropdowns. */
    @GetMapping("/cities")
    public List<String> cities() {
        return tripService.cities();
    }

    /** The "Popular routes" cards on the home page. */
    @GetMapping("/routes/popular")
    public List<PopularRouteResponse> popular() {
        return tripService.popularRoutes();
    }
}
