package com.busbooking.config;

import com.busbooking.model.Booking;
import com.busbooking.model.Coach;
import com.busbooking.model.CoachStatus;
import com.busbooking.model.CoachType;
import com.busbooking.model.Operator;
import com.busbooking.model.PaymentMethod;
import com.busbooking.model.PromoCode;
import com.busbooking.model.Route;
import com.busbooking.model.Trip;
import com.busbooking.repository.BookingRepository;
import com.busbooking.repository.CoachRepository;
import com.busbooking.repository.OperatorRepository;
import com.busbooking.repository.PromoCodeRepository;
import com.busbooking.repository.RouteRepository;
import com.busbooking.repository.TripRepository;
import com.busbooking.service.PricingService;
import com.busbooking.service.SeatLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sample data centred on Ho Chi Minh City: intercity coach routes between HCMC's bus stations
 * (Miền Đông mới, Miền Tây, An Sương) and 12 destinations, in both directions. Operators are
 * fictional. Prices are in VND. Runs on every start and only adds what is missing:
 * <ul>
 *   <li>first start (empty database): operators, coaches, routes, promo codes, one sample booking;</li>
 *   <li>every start: trips for any of the coming days that have none yet.</li>
 * </ul>
 * Riders' bookings are never touched.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    public static final String HCMC = "TP. Hồ Chí Minh";
    private static final String MIEN_DONG = "Bến xe Miền Đông mới";
    private static final String MIEN_TAY = "Bến xe Miền Tây";
    private static final String AN_SUONG = "Bến xe An Sương";

    /** A destination served from HCMC: station used in HCMC, station at the other end, km, minutes, cheapest fare. */
    private record Line(String city, String hcmcStation, String cityStation, int km, int minutes, int fromPrice,
                        boolean popular) {}

    private static final List<Line> LINES = List.of(
            new Line("Đà Lạt", MIEN_DONG, "Bến xe Liên tỉnh Đà Lạt", 308, 420, 280_000, true),
            new Line("Vũng Tàu", MIEN_DONG, "Bến xe Vũng Tàu", 125, 150, 160_000, true),
            new Line("Cần Thơ", MIEN_TAY, "Bến xe Trung tâm Cần Thơ", 170, 210, 165_000, true),
            new Line("Nha Trang", MIEN_DONG, "Bến xe phía Nam Nha Trang", 430, 510, 290_000, true),
            new Line("Phan Thiết", MIEN_DONG, "Bến xe Phan Thiết", 200, 240, 190_000, true),
            new Line("Châu Đốc", MIEN_TAY, "Bến xe Châu Đốc", 250, 360, 220_000, true),
            new Line("Rạch Giá", MIEN_TAY, "Bến xe Rạch Giá", 250, 330, 230_000, false),
            new Line("Buôn Ma Thuột", MIEN_DONG, "Bến xe phía Bắc Buôn Ma Thuột", 350, 480, 260_000, false),
            new Line("Tây Ninh", AN_SUONG, "Bến xe Tây Ninh", 100, 150, 90_000, false),
            new Line("Bến Tre", MIEN_TAY, "Bến xe Bến Tre", 85, 120, 95_000, false),
            new Line("Cà Mau", MIEN_TAY, "Bến xe Cà Mau", 300, 480, 250_000, false),
            new Line("Quy Nhơn", MIEN_DONG, "Bến xe Quy Nhơn", 650, 720, 400_000, false));

    /**
     * Approximate map positions (lat, lng) of the stations, used to ask Google Maps for live traffic.
     * The HCMC stations are placed at the stations; the other ends are close to the city's bus station.
     * Managers can correct them through PUT /api/v1/manage/routes/{id}.
     */
    private static final Map<String, double[]> STATION_POSITIONS = Map.ofEntries(
            Map.entry(MIEN_DONG, new double[]{10.8790, 106.8155}),
            Map.entry(MIEN_TAY, new double[]{10.7404, 106.6188}),
            Map.entry(AN_SUONG, new double[]{10.8436, 106.6128}),
            Map.entry("Bến xe Liên tỉnh Đà Lạt", new double[]{11.9273, 108.4446}),
            Map.entry("Bến xe Vũng Tàu", new double[]{10.3520, 107.0857}),
            Map.entry("Bến xe Trung tâm Cần Thơ", new double[]{9.9955, 105.7433}),
            Map.entry("Bến xe phía Nam Nha Trang", new double[]{12.2210, 109.1500}),
            Map.entry("Bến xe Phan Thiết", new double[]{10.9400, 108.1000}),
            Map.entry("Bến xe Châu Đốc", new double[]{10.6905, 105.1140}),
            Map.entry("Bến xe Rạch Giá", new double[]{10.0050, 105.0900}),
            Map.entry("Bến xe phía Bắc Buôn Ma Thuột", new double[]{12.7000, 108.0560}),
            Map.entry("Bến xe Tây Ninh", new double[]{11.3100, 106.1000}),
            Map.entry("Bến xe Bến Tre", new double[]{10.2600, 106.3650}),
            Map.entry("Bến xe Cà Mau", new double[]{9.1700, 105.1700}),
            Map.entry("Bến xe Quy Nhơn", new double[]{13.7690, 109.2230}));

    /** Six departures a day; each adds a different amount (VND) to the route's cheapest fare. */
    private static final LocalTime[] DEPARTURES = {
            LocalTime.of(6, 0), LocalTime.of(8, 30), LocalTime.of(11, 0),
            LocalTime.of(14, 15), LocalTime.of(18, 0), LocalTime.of(21, 30)};
    private static final int[] PRICE_ADD = {20_000, 60_000, 30_000, 80_000, 0, 40_000};

    /** Rows per coach type: standard 9 rows (36 seats), premium 6 (24), sleeper 10 (40 berths). */
    private static final CoachType[] TYPES = {CoachType.STANDARD, CoachType.PREMIUM, CoachType.SLEEPER};
    private static final int[] ROWS = {9, 6, 10};
    private static final int COACHES_PER_OPERATOR = 6;

    private final RouteRepository routes;
    private final OperatorRepository operators;
    private final CoachRepository coaches;
    private final TripRepository trips;
    private final BookingRepository bookings;
    private final PromoCodeRepository promoCodes;
    private final PricingService pricing;
    private final int days;

    public DataSeeder(RouteRepository routes, OperatorRepository operators, CoachRepository coaches,
                      TripRepository trips, BookingRepository bookings, PromoCodeRepository promoCodes,
                      PricingService pricing, @Value("${app.seed.days:14}") int days) {
        this.routes = routes;
        this.operators = operators;
        this.coaches = coaches;
        this.trips = trips;
        this.bookings = bookings;
        this.promoCodes = promoCodes;
        this.pricing = pricing;
        this.days = days;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (routes.count() == 0) {
            seedBaseData();
        }

        addMissingPositions();

        List<Route> routeList = routes.findAll(Sort.by("id")).stream().filter(Route::isActive).toList();
        List<Coach> fleet = coaches.findAll(Sort.by("id")).stream()
                .filter(c -> c.getStatus() == CoachStatus.ACTIVE).toList();
        if (routeList.isEmpty() || fleet.isEmpty()) return;

        LocalDate today = LocalDate.now();
        int added = 0;
        for (int day = 0; day < days; day++) {
            LocalDate date = today.plusDays(day);
            if (trips.existsByDepartureTimeBetween(date.atStartOfDay(), date.atTime(LocalTime.MAX))) continue;
            seedTrips(date, routeList, fleet);
            added++;
        }
        if (added > 0) log.info("Added sample trips for {} day(s)", added);
    }

    private void seedBaseData() {
        log.info("Empty database: adding HCMC sample operators, coaches, routes and trips");

        List<Operator> ops = operators.saveAll(List.of(
                new Operator("Sao Việt Express", "#0F4C81", 4.6, "1900 6001", "hotro@saovietexpress.example"),
                new Operator("Mekong Xanh", "#22A06B", 4.4, "1900 6002", "cskh@mekongxanh.example"),
                new Operator("Cao Nguyên Limousine", "#7C3AED", 4.8, "1900 6003", "booking@caonguyen.example"),
                new Operator("Biển Xanh Coach", "#E8590C", 4.2, "1900 6004", "lienhe@bienxanh.example")));

        List<Coach> fleet = new ArrayList<>();
        int n = 0;
        for (Operator op : ops) {
            for (int i = 0; i < COACHES_PER_OPERATOR; i++, n++) {
                int t = i % TYPES.length;
                // HCMC passenger-coach plates look like 51B-123.45
                String plate = "51B-%03d.%02d".formatted(100 + n * 37 % 900, 10 + n * 13 % 90);
                fleet.add(new Coach(plate, op, TYPES[t], ROWS[t], 2019 + n % 7));
            }
        }
        fleet = coaches.saveAll(fleet);
        fleet.get(fleet.size() - 1).setStatus(CoachStatus.MAINTENANCE); // one coach in the workshop

        List<Route> routeList = new ArrayList<>();
        for (Line l : LINES) {
            BigDecimal price = BigDecimal.valueOf(l.fromPrice());
            routeList.add(new Route(HCMC, l.city(), l.hcmcStation(), l.cityStation(), l.km(), l.minutes(), price, l.popular()));
            routeList.add(new Route(l.city(), HCMC, l.cityStation(), l.hcmcStation(), l.km(), l.minutes(), price, false));
        }
        routeList = routes.saveAll(routeList);

        LocalDate today = LocalDate.now();
        promoCodes.saveAll(List.of(
                new PromoCode("FIRSTRIDE", "10% off your first ride", 10, null, null, null, null),
                new PromoCode("WEEKEND", "15% off fares of 300.000 ₫ or more", 15,
                        today.minusDays(7), today.plusDays(60), 500, new BigDecimal("300000")),
                new PromoCode("TET2027", "20% off for Tết 2027 travel", 20,
                        LocalDate.of(2027, 1, 20), LocalDate.of(2027, 2, 20), 1000, null),
                new PromoCode("HELLOSAIGON", "Launch offer (ended)", 25,
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), 200, null)));

        // A completed trip, so "Find my booking" has something to find: HC-48213 / sam@example.com
        Route canTho = routeList.stream()
                .filter(r -> r.getOrigin().equals(HCMC) && r.getDestination().equals("Cần Thơ"))
                .findFirst().orElseThrow();
        LocalDateTime dep = LocalDateTime.of(2025, 1, 12, 8, 30);
        Trip past = trips.save(new Trip(canTho, fleet.get(1), dep, dep.plusMinutes(canTho.getDurationMinutes()),
                canTho.getFromPrice()));
        saveBooking("HC-48213", past, "Sam Nguyen", "sam@example.com", List.of("3A"));
    }

    /** Fills in station positions for routes created before positions existed (only where none are set). */
    private void addMissingPositions() {
        int updated = 0;
        for (Route route : routes.findAll()) {
            double[] from = STATION_POSITIONS.get(route.getOriginStation());
            double[] to = STATION_POSITIONS.get(route.getDestinationStation());
            boolean changed = false;
            if (route.getOriginLat() == null && from != null) {
                route.setOriginPosition(from[0], from[1]);
                changed = true;
            }
            if (route.getDestinationLat() == null && to != null) {
                route.setDestinationPosition(to[0], to[1]);
                changed = true;
            }
            if (changed) updated++;
        }
        if (updated > 0) log.info("Added station map positions to {} route(s)", updated);
    }

    /** Each pair of routes (out and back) is run by one operator; departures rotate through its coaches. */
    private void seedTrips(LocalDate date, List<Route> routeList, List<Coach> fleet) {
        List<Long> operatorIds = fleet.stream().map(c -> c.getOperator().getId()).distinct().toList();
        for (int r = 0; r < routeList.size(); r++) {
            Route route = routeList.get(r);
            Long operatorId = operatorIds.get((r / 2) % operatorIds.size());
            List<Coach> own = fleet.stream().filter(c -> c.getOperator().getId().equals(operatorId)).toList();
            for (int i = 0; i < DEPARTURES.length; i++) {
                Coach coach = own.get((i + r) % own.size());
                LocalDateTime dep = LocalDateTime.of(date, DEPARTURES[i]);
                Trip trip = trips.save(new Trip(route, coach, dep, dep.plusMinutes(route.getDurationMinutes()),
                        route.getFromPrice().add(BigDecimal.valueOf(PRICE_ADD[i]))));

                // Popular routes get some seats already sold, so the seat map shows booked seats.
                if (route.isPopular()) seedStationSale(trip, i);
            }
        }
    }

    private void seedStationSale(Trip trip, int pattern) {
        List<String> seats = SeatLayout.allSeats(trip.getCoach().getSeatRows()).stream()
                .filter(s -> (pattern * 7 + SeatLayout.rowOf(s) * 3 + s.charAt(s.length() - 1)) % 5 == 0)
                .toList();
        if (seats.isEmpty()) return;

        String code;
        do {
            code = "HC-" + ThreadLocalRandom.current().nextInt(10000, 100000);
        } while (bookings.existsByBookingCode(code));
        saveBooking(code, trip, "Bán tại quầy", "quay@hcoach.example", seats);
    }

    private void saveBooking(String code, Trip trip, String name, String email, List<String> seats) {
        var price = pricing.quote(trip, seats, null);
        bookings.save(new Booking(code, trip, name, email, null, seats,
                price.fare(), price.discount(), price.tax(), price.total(), null, PaymentMethod.CARD));
    }
}
