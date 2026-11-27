package com.busbooking.citybus;

import com.busbooking.dto.CityBusDtos;
import com.busbooking.exception.ApiException;
import com.busbooking.exception.ErrorCode;
import com.busbooking.exception.NotFoundException;
import com.busbooking.model.CityBusDirection;
import com.busbooking.model.CityBusDirectionStop;
import com.busbooking.model.CityBusLine;
import com.busbooking.model.CityBusStop;
import com.busbooking.repository.CityBusDirectionStopRepository;
import com.busbooking.repository.CityBusLineRepository;
import com.busbooking.repository.CityBusStopRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read side of the city bus data. The list and its search index are built once and kept in memory
 * (the data only changes on import), so searching by line number, name or stop name is instant.
 */
@Service
public class CityBusService {

    public static final int MAX_RADIUS_M = 2000;
    private static final int MAX_NEARBY = 15;
    private static final Pattern LEADING_NUMBER = Pattern.compile("^(\\d+)(.*)$");

    /** Natural order of line numbers: 1, 2, 10, 60-1, 60-15, D2, TIA. */
    public static final Comparator<String> REF_ORDER = (a, b) -> {
        Matcher ma = LEADING_NUMBER.matcher(a), mb = LEADING_NUMBER.matcher(b);
        boolean na = ma.matches(), nb = mb.matches();
        if (na && nb) {
            int c = Integer.compare(Integer.parseInt(ma.group(1)), Integer.parseInt(mb.group(1)));
            return c != 0 ? c : ma.group(2).compareTo(mb.group(2));
        }
        if (na != nb) return na ? -1 : 1;
        return a.compareTo(b);
    };

    private record Entry(CityBusDtos.Summary summary, String searchText) {}

    private final CityBusLineRepository lines;
    private final CityBusStopRepository stops;
    private final CityBusDirectionStopRepository directionStops;
    private volatile List<Entry> catalog;

    public CityBusService(CityBusLineRepository lines, CityBusStopRepository stops,
                          CityBusDirectionStopRepository directionStops) {
        this.lines = lines;
        this.stops = stops;
        this.directionStops = directionStops;
    }

    @EventListener
    public void onImport(CityBusImportService.DataChanged event) {
        catalog = null;
    }

    /** All lines, or those whose number, name, terminals or any stop matches {@code q} (accents ignored). */
    public List<CityBusDtos.Summary> list(String q) {
        List<Entry> all = catalog();
        String needle = q == null ? "" : normalize(q.trim());
        if (needle.isEmpty()) return all.stream().map(Entry::summary).toList();
        // An exact line number first, then other matches.
        return all.stream().filter(e -> e.searchText().contains(needle))
                .sorted(Comparator.comparing((Entry e) -> !normalize(e.summary().ref()).equals(needle)))
                .map(Entry::summary).toList();
    }

    @Transactional(readOnly = true)
    public CityBusDtos.Detail detail(Long id) {
        CityBusLine line = lines.findWithDirections(id)
                .orElseThrow(() -> new NotFoundException("Bus line " + id + " not found"));
        List<Long> dirIds = line.getDirections().stream().map(CityBusDirection::getId).toList();
        Map<Long, List<CityBusDtos.Stop>> stopsByDir = new HashMap<>();
        if (!dirIds.isEmpty()) {
            for (CityBusDirectionStop ds : directionStops.findByDirectionIds(dirIds)) {
                CityBusStop s = ds.getStop();
                stopsByDir.computeIfAbsent(ds.getDirection().getId(), k -> new ArrayList<>())
                        .add(new CityBusDtos.Stop(s.getId(), s.getName(), s.getLat(), s.getLng()));
            }
        }
        List<CityBusDtos.Direction> dirs = line.getDirections().stream().map(d -> new CityBusDtos.Direction(
                d.getId(), d.getName(), d.getFromName(), d.getToName(), km(d.getLengthMeters()), d.getPolyline(),
                stopsByDir.getOrDefault(d.getId(), List.of()))).toList();
        int maxStops = dirs.stream().mapToInt(d -> d.stops().size()).max().orElse(0);
        return new CityBusDtos.Detail(summary(line, maxStops), dirs, CityBusDtos.ATTRIBUTION);
    }

    /** Stops within {@code radiusM} metres of a point, nearest first, with the lines that serve them. */
    @Transactional(readOnly = true)
    public List<CityBusDtos.NearbyStop> near(double lat, double lng, int radiusM) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "lat/lng out of range");
        }
        int r = Math.max(50, Math.min(MAX_RADIUS_M, radiusM));
        double dLat = r / 111_320.0;
        double dLng = r / (111_320.0 * Math.cos(Math.toRadians(lat)));
        record Found(CityBusStop stop, int distance) {}
        List<Found> found = stops.findByLatBetweenAndLngBetween(lat - dLat, lat + dLat, lng - dLng, lng + dLng).stream()
                .map(s -> new Found(s, (int) Math.round(Geo.meters(lat, lng, s.getLat(), s.getLng()))))
                .filter(f -> f.distance() <= r)
                .sorted(Comparator.comparingInt(Found::distance))
                .toList();
        if (found.isEmpty()) return List.of();

        Map<Long, List<CityBusDtos.LineRef>> linesAt = new HashMap<>();
        for (Object[] row : directionStops.findLinesAtStops(found.stream().map(f -> f.stop().getId()).toList())) {
            linesAt.computeIfAbsent((Long) row[0], k -> new ArrayList<>())
                    .add(new CityBusDtos.LineRef((Long) row[1], (String) row[2], (String) row[3]));
        }
        return found.stream()
                .filter(f -> linesAt.containsKey(f.stop().getId())) // stops no line uses any more
                .limit(MAX_NEARBY)
                .map(f -> new CityBusDtos.NearbyStop(f.stop().getId(), f.stop().getName(), f.stop().getLat(),
                        f.stop().getLng(), f.distance(), linesAt.get(f.stop().getId()).stream()
                        .sorted(Comparator.comparing(CityBusDtos.LineRef::ref, REF_ORDER)).toList()))
                .toList();
    }

    private List<Entry> catalog() {
        List<Entry> c = catalog;
        if (c == null) {
            c = buildCatalog();
            catalog = c;
        }
        return c;
    }

    /** Two queries: lines with their directions (fetched together), and every stop name per direction. */
    private List<Entry> buildCatalog() {
        Map<Long, Set<String>> stopNamesByDir = new HashMap<>();
        Map<Long, Integer> stopCountByDir = new HashMap<>();
        for (Object[] row : directionStops.findDirectionStopNames()) {
            Long dirId = (Long) row[0];
            stopNamesByDir.computeIfAbsent(dirId, k -> new HashSet<>()).add(normalize((String) row[1]));
            stopCountByDir.merge(dirId, 1, Integer::sum);
        }
        List<Entry> out = new ArrayList<>();
        for (CityBusLine line : lines.findAllWithDirections()) {
            StringBuilder text = new StringBuilder(normalize(line.getRef())).append(' ').append(normalize(line.getName()));
            int maxStops = 0;
            for (CityBusDirection d : line.getDirections()) {
                maxStops = Math.max(maxStops, stopCountByDir.getOrDefault(d.getId(), 0));
                text.append(' ').append(normalize(d.getFromName())).append(' ').append(normalize(d.getToName()));
                for (String s : stopNamesByDir.getOrDefault(d.getId(), Set.of())) text.append(" | ").append(s);
            }
            out.add(new Entry(summary(line, maxStops), text.toString()));
        }
        out.sort(Comparator.comparing(e -> e.summary().ref(), REF_ORDER));
        return List.copyOf(out);
    }

    private static CityBusDtos.Summary summary(CityBusLine l, int stopCount) {
        CityBusDirection first = l.getDirections().isEmpty() ? null : l.getDirections().get(0);
        return new CityBusDtos.Summary(l.getId(), l.getRef(), l.getName(),
                first == null ? null : first.getFromName(), first == null ? null : first.getToName(),
                l.getOperator(), l.getOpeningHours(), l.getHeadway(), l.getFare(), l.getFareVnd(), l.getColour(),
                l.getDirections().size(), stopCount, first == null ? 0 : km(first.getLengthMeters()));
    }

    private static double km(int meters) {
        return Math.round(meters / 100.0) / 10.0;
    }

    /** Lower case without Vietnamese accents: "Bến Thành" → "ben thanh". */
    static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return n.replace('đ', 'd');
    }
}
