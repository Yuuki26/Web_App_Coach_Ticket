package com.busbooking.citybus;

import com.busbooking.repository.CityBusLineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;

/**
 * On a start with no city bus data, loads the bundled OpenStreetMap snapshot
 * ({@code src/main/resources/citybus/hcmc-bus-osm.json}). Refresh it later with
 * POST /api/v1/admin/city-buses/import.
 */
@Component
public class CityBusSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CityBusSeeder.class);
    static final String SNAPSHOT = "citybus/hcmc-bus-osm.json";

    private final CityBusLineRepository lines;
    private final CityBusImportService importer;
    private final JsonMapper json;
    private final boolean enabled;

    public CityBusSeeder(CityBusLineRepository lines, CityBusImportService importer, JsonMapper json,
                         @Value("${app.citybus.seed:true}") boolean enabled) {
        this.lines = lines;
        this.importer = importer;
        this.json = json;
        this.enabled = enabled;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!enabled || lines.count() > 0) return;
        ClassPathResource resource = new ClassPathResource(SNAPSHOT);
        if (!resource.exists()) {
            log.warn("No city bus snapshot at {}; import with POST /api/v1/admin/city-buses/import", SNAPSHOT);
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            CityBusSnapshot snap = json.readValue(in, CityBusSnapshot.class);
            CityBusImportService.ImportResult r = importer.apply(snap);
            log.info("Loaded {} city bus lines ({} directions, {} stops) from {} ({})",
                    r.lines(), r.directions(), r.stops(), SNAPSHOT, snap.fetchedAt());
        }
    }
}
