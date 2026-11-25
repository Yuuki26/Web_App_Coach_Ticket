package com.busbooking.citybus;

import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.model.CityBusDirection;
import com.busbooking.model.CityBusDirectionStop;
import com.busbooking.model.CityBusLine;
import com.busbooking.model.CityBusStop;
import com.busbooking.repository.CityBusDirectionRepository;
import com.busbooking.repository.CityBusDirectionStopRepository;
import com.busbooking.repository.CityBusLineRepository;
import com.busbooking.repository.CityBusStopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads HCMC city bus lines from OpenStreetMap into the database: from the bundled snapshot on first
 * start ({@link CityBusSeeder}), or live from the Overpass API when an admin asks for a refresh.
 * Every import replaces the lines, directions and stop lists; stops are updated in place.
 */
@Service
public class CityBusImportService {

    private static final Logger log = LoggerFactory.getLogger(CityBusImportService.class);

    /** Bus routes in and around HCMC with their masters, stop nodes and road geometry. */
    public static final String OVERPASS_QUERY = """
            [out:json][timeout:180];
            rel["type"="route"]["route"="bus"](10.35,106.35,11.17,107.05)->.r;
            .r out body;
            rel(br.r)["type"="route_master"];
            out body;
            node(r.r);
            out body;
            way(r.r);
            out geom;""";

    private static final Pattern NUMBER = Pattern.compile("(\\d[\\d.,]*)");

    public record ImportResult(int lines, int directions, int stops, String source) {}

    /** Published after an import so cached views of the data are rebuilt. */
    public record DataChanged() {}

    private final CityBusLineRepository lines;
    private final CityBusDirectionRepository directions;
    private final CityBusDirectionStopRepository directionStops;
    private final CityBusStopRepository stops;
    private final JsonMapper json;
    private final ApplicationEventPublisher events;
    private final Set<String> networks;
    private final List<String> overpassUrls;
    private final TransactionTemplate tx;

    public CityBusImportService(CityBusLineRepository lines, CityBusDirectionRepository directions,
                                CityBusDirectionStopRepository directionStops, CityBusStopRepository stops,
                                JsonMapper json, ApplicationEventPublisher events,
                                @Value("${app.citybus.networks}") List<String> networks,
                                @Value("${app.citybus.overpass-urls}") List<String> overpassUrls,
                                PlatformTransactionManager transactionManager) {
        this.lines = lines;
        this.directions = directions;
        this.directionStops = directionStops;
        this.stops = stops;
        this.json = json;
        this.events = events;
        this.networks = Set.copyOf(networks);
        this.overpassUrls = overpassUrls;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** Parses a raw Overpass answer (e.g. a saved file) and imports it. */
    @Transactional
    public ImportResult importOverpass(InputStream raw) {
        JsonNode tree = json.readTree(raw);
        String now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toString();
        return apply(new OsmBusParser(networks).parse(tree, now));
    }

    /**
     * Downloads fresh data from the first Overpass server that answers, then imports it. The download
     * (up to a few minutes) happens outside the database transaction.
     */
    public ImportResult importLive() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
        factory.setReadTimeout(Duration.ofSeconds(200));
        RestClient http = RestClient.builder().requestFactory(factory).build();
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("data", OVERPASS_QUERY);

        for (String url : overpassUrls) {
            try {
                byte[] body = http.post().uri(url).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .header("User-Agent", "hcoach-bus-booking/1.0")
                        .body(form).retrieve().body(byte[].class);
                if (body == null || body.length == 0 || body[0] != '{') {
                    log.warn("Overpass server {} sent no JSON (probably busy)", url);
                    continue;
                }
                ImportResult result = tx.execute(status -> importOverpass(new java.io.ByteArrayInputStream(body)));
                log.info("Imported city buses from {}: {}", url, result);
                return result;
            } catch (RestClientException e) {
                log.warn("Overpass server {} failed: {}", url, e.getMessage());
            }
        }
        throw new ApiException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
                "OpenStreetMap's Overpass servers are busy right now. Try again in a few minutes");
    }

    /** Replaces the city bus data with the snapshot's. */
    @Transactional
    public ImportResult apply(CityBusSnapshot snap) {
        // Direction rows are rebuilt from scratch; deleting them first avoids clashes on their OSM ids.
        directionStops.deleteAllInBatch();
        directions.deleteAllInBatch();

        Map<Long, CityBusStop> stopByOsm = new HashMap<>();
        for (CityBusStop s : stops.findAll()) stopByOsm.put(s.getOsmNodeId(), s);
        List<CityBusStop> newStops = new ArrayList<>();
        for (CityBusSnapshot.Stop s : snap.stops()) {
            CityBusStop existing = stopByOsm.get(s.osmId());
            if (existing != null) {
                existing.update(s.name(), s.lat(), s.lng());
            } else {
                CityBusStop created = new CityBusStop(s.osmId(), s.name(), s.lat(), s.lng());
                newStops.add(created);
                stopByOsm.put(s.osmId(), created);
            }
        }
        stops.saveAll(newStops);

        Map<String, CityBusLine> lineByKey = new LinkedHashMap<>();
        for (CityBusLine l : lines.findAll()) lineByKey.put(l.getNetwork() + "|" + l.getRef(), l);
        Set<String> kept = new HashSet<>();
        int directionCount = 0;
        for (CityBusSnapshot.Line l : snap.lines()) {
            String key = l.network() + "|" + l.ref();
            kept.add(key);
            CityBusLine line = lineByKey.computeIfAbsent(key, k -> new CityBusLine(l.network(), l.ref()));
            CityBusSnapshot.Direction first = l.directions().get(0);
            line.setName(first.name() != null ? first.name()
                    : first.from() != null && first.to() != null ? first.from() + " - " + first.to() : "Tuyến " + l.ref());
            line.setOperator(l.operator());
            line.setOpeningHours(l.openingHours());
            line.setHeadway(l.headway());
            line.setFare(l.fare());
            line.setFareVnd(parseVnd(l.fare()));
            line.setColour(l.colour());
            line.setOsmMasterId(l.osmMasterId());
            line.getDirections().clear();

            int order = 0;
            for (CityBusSnapshot.Direction d : l.directions()) {
                CityBusDirection dir = new CityBusDirection(line, d.osmId(), order++);
                dir.setName(d.name() != null ? d.name() : line.getName());
                dir.setFromName(d.from());
                dir.setToName(d.to());
                dir.setPolyline(d.polyline());
                dir.setLengthMeters(d.lengthMeters());
                int seq = 1;
                for (Long osmId : d.stops()) {
                    CityBusStop stop = stopByOsm.get(osmId);
                    if (stop != null) dir.getStops().add(new CityBusDirectionStop(dir, stop, seq++));
                }
                line.getDirections().add(dir);
                directionCount++;
            }
            lines.save(line);
        }
        List<CityBusLine> gone = lineByKey.entrySet().stream()
                .filter(e -> !kept.contains(e.getKey()) && e.getValue().getId() != null)
                .map(Map.Entry::getValue).toList();
        lines.deleteAll(gone);

        events.publishEvent(new DataChanged());
        return new ImportResult(snap.lines().size(), directionCount, snap.stops().size(), snap.source());
    }

    /** The current data as a snapshot, e.g. to refresh the bundled file. */
    @Transactional(readOnly = true)
    public CityBusSnapshot snapshot() {
        List<CityBusLine> all = lines.findAllWithDirections();
        List<Long> dirIds = all.stream().flatMap(l -> l.getDirections().stream()).map(CityBusDirection::getId).toList();
        Map<Long, List<Long>> stopsByDir = new HashMap<>();
        Map<Long, CityBusSnapshot.Stop> usedStops = new LinkedHashMap<>();
        if (!dirIds.isEmpty()) {
            for (CityBusDirectionStop ds : directionStops.findByDirectionIds(dirIds)) {
                CityBusStop s = ds.getStop();
                stopsByDir.computeIfAbsent(ds.getDirection().getId(), k -> new ArrayList<>()).add(s.getOsmNodeId());
                usedStops.putIfAbsent(s.getOsmNodeId(),
                        new CityBusSnapshot.Stop(s.getOsmNodeId(), s.getName(), s.getLat(), s.getLng()));
            }
        }
        List<CityBusSnapshot.Line> out = all.stream().map(l -> new CityBusSnapshot.Line(
                l.getNetwork(), l.getRef(), l.getOperator(), l.getOpeningHours(), l.getHeadway(), l.getFare(),
                l.getColour(), l.getOsmMasterId(),
                l.getDirections().stream().map(d -> new CityBusSnapshot.Direction(
                        d.getOsmRelationId(), d.getName(), d.getFromName(), d.getToName(), d.getPolyline(),
                        d.getLengthMeters(), stopsByDir.getOrDefault(d.getId(), List.of()))).toList())).toList();
        String now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS).toString();
        return new CityBusSnapshot("© OpenStreetMap contributors, ODbL", now, out, new ArrayList<>(usedStops.values()));
    }

    /** "6000 VND" → 6000; "6.000đ" → 6000; null when no number. */
    static Integer parseVnd(String fare) {
        if (fare == null) return null;
        Matcher m = NUMBER.matcher(fare);
        if (!m.find()) return null;
        try {
            return Integer.valueOf(m.group(1).replaceAll("[.,]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
