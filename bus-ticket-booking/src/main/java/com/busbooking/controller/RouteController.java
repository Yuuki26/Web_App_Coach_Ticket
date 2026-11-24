package com.busbooking.controller;

import com.busbooking.dto.PopularRouteResponse;
import com.busbooking.dto.RouteResponse;
import com.busbooking.dto.TrafficResponse;
import com.busbooking.service.TripService;
import com.busbooking.traffic.TrafficService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RouteController {

    private final TripService tripService;
    private final TrafficService trafficService;

    public RouteController(TripService tripService, TrafficService trafficService) {
        this.tripService = tripService;
        this.trafficService = trafficService;
    }

    /** City names for the From / To dropdowns. */
    @GetMapping("/cities")
    public List<String> cities() {
        return tripService.cities();
    }

    /** Every route we run, with stations and map positions, for the Routes page. */
    @GetMapping("/routes")
    public List<RouteResponse> routes() {
        return tripService.routes();
    }

    /** The "Popular routes" cards on the home page. */
    @GetMapping("/routes/popular")
    public List<PopularRouteResponse> popular() {
        return tripService.popularRoutes();
    }

    /** Live traffic between the route's two stations (Google Maps), refreshed at most every few minutes. */
    @GetMapping("/routes/{id}/traffic")
    public TrafficResponse traffic(@PathVariable Long id) {
        return trafficService.forRoute(id);
    }
}
