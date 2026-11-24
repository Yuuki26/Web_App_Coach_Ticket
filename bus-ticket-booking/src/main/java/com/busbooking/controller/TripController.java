package com.busbooking.controller;

import com.busbooking.dto.SeatMapResponse;
import com.busbooking.dto.TripResponse;
import com.busbooking.dto.TripTrafficResponse;
import com.busbooking.service.TripService;
import com.busbooking.traffic.TrafficService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
public class TripController {

    private final TripService tripService;
    private final TrafficService trafficService;

    public TripController(TripService tripService, TrafficService trafficService) {
        this.tripService = tripService;
        this.trafficService = trafficService;
    }

    /** GET /api/v1/trips/search?from=New%20York&to=Boston&date=2026-11-20 */
    @GetMapping("/search")
    public List<TripResponse> search(@RequestParam String from,
                                     @RequestParam String to,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return tripService.search(from, to, date);
    }

    @GetMapping("/{id}")
    public TripResponse byId(@PathVariable Long id) {
        return tripService.findById(id);
    }

    @GetMapping("/{id}/seats")
    public SeatMapResponse seats(@PathVariable Long id) {
        return tripService.seatMap(id);
    }

    /** Live traffic on the trip's route, plus an arrival estimate for trips leaving within a few hours. */
    @GetMapping("/{id}/traffic")
    public TripTrafficResponse traffic(@PathVariable Long id) {
        return trafficService.forTrip(id);
    }
}
