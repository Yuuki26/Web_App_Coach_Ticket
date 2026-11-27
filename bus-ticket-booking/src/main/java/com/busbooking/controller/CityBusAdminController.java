package com.busbooking.controller;

import com.busbooking.citybus.CityBusImportService;
import com.busbooking.citybus.CityBusSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/** Refreshing the city bus data from OpenStreetMap. Admins only. */
@RestController
@RequestMapping("/api/v1/admin/city-buses")
@PreAuthorize("hasRole('ADMIN')")
public class CityBusAdminController {

    private final CityBusImportService importer;

    public CityBusAdminController(CityBusImportService importer) {
        this.importer = importer;
    }

    /** Downloads fresh data from OpenStreetMap's Overpass API (can take a minute or two). */
    @PostMapping("/import")
    public CityBusImportService.ImportResult importLive() {
        return importer.importLive();
    }

    /** Imports a saved Overpass answer sent as the request body (the query is in CityBusImportService). */
    @PostMapping(value = "/import-file", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CityBusImportService.ImportResult importFile(HttpServletRequest request) throws IOException {
        return importer.importOverpass(request.getInputStream());
    }

    /** The current data in the bundled-file format (save as src/main/resources/citybus/hcmc-bus-osm.json). */
    @GetMapping("/snapshot")
    public CityBusSnapshot snapshot() {
        return importer.snapshot();
    }
}
