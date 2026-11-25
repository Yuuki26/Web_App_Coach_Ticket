package com.busbooking.controller;

import com.busbooking.citybus.CityBusService;
import com.busbooking.dto.CityBusDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Ho Chi Minh City bus lines (OpenStreetMap data). Public. */
@RestController
@RequestMapping("/api/v1/city-buses")
public class CityBusController {

    private final CityBusService service;

    public CityBusController(CityBusService service) {
        this.service = service;
    }

    /** All lines in number order; {@code q} searches number, name, terminals and stop names (accents optional). */
    @GetMapping
    public List<CityBusDtos.Summary> list(@RequestParam(required = false) String q) {
        return service.list(q);
    }

    /** One line with each direction's path (encoded polyline) and stops in order. */
    @GetMapping("/{id}")
    public CityBusDtos.Detail detail(@PathVariable Long id) {
        return service.detail(id);
    }

    /** Bus stops near a point (e.g. a coach station), nearest first, with the lines at each. */
    @GetMapping("/stops/near")
    public List<CityBusDtos.NearbyStop> near(@RequestParam double lat, @RequestParam double lng,
                                             @RequestParam(defaultValue = "500") int radius) {
        return service.near(lat, lng, radius);
    }
}
