package com.busbooking.config;

import com.busbooking.dto.QuoteResponse;
import com.busbooking.model.Booking;
import com.busbooking.model.CoachType;
import com.busbooking.model.Operator;
import com.busbooking.model.PaymentMethod;
import com.busbooking.model.Route;
import com.busbooking.model.Trip;
import com.busbooking.repository.BookingRepository;
import com.busbooking.repository.OperatorRepository;
import com.busbooking.repository.RouteRepository;
import com.busbooking.repository.TripRepository;
import com.busbooking.service.PricingService;
import com.busbooking.service.SeatLayout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fills the empty in-memory database on startup with the sample data from the H Coach design:
 * its cities, operators, popular routes, and trips for the coming days. All of it is demo data.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final List<String> CITIES = List.of(
            "New York", "Boston", "Los Angeles", "Las Vegas", "San Francisco", "Chicago",
            "Detroit", "Miami", "Orlando", "Seattle", "Portland");

    /** origin, destination, cheapest fare, duration in minutes. */
    private record Popular(String origin, String destination, int fromPrice, int minutes) {}

    private static final List<Popular> POPULAR = List.of(
            new Popular("New York", "Boston", 24, 270),
            new Popular("Los Angeles", "Las Vegas", 29, 255),
            new Popular("San Francisco", "Los Angeles", 39, 490),
            new Popular("Chicago", "Detroit", 22, 320),
            new Popular("Miami", "Orlando", 27, 230),
            new Popular("Seattle", "Portland", 19, 210));

    /** Six departures a day; each adds a different amount to the route's cheapest fare. */
    private static final LocalTime[] DEPARTURES = {
            LocalTime.of(6, 0), LocalTime.of(8, 30), LocalTime.of(11, 0),
            LocalTime.of(14, 15), LocalTime.of(18, 0), LocalTime.of(21, 30)};
    private static final int[] PRICE_ADD = {3, 15, 7, 19, 0, 11};

    private final RouteRepository routes;
    private final OperatorRepository operators;
    private final TripRepository trips;
    private final BookingRepository bookings;
    private final PricingService pricing;
    private final int days;

    private int seedBookingCounter = 10000;

    public DataSeeder(RouteRepository routes, OperatorRepository operators, TripRepository trips,
                      BookingRepository bookings, PricingService pricing,
                      @Value("${app.seed.days:14}") int days) {
        this.routes = routes;
        this.operators = operators;
        this.trips = trips;
        this.bookings = bookings;
        this.pricing = pricing;
        this.days = days;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (routes.count() > 0) return;

        List<Operator> ops = operators.saveAll(List.of(
                new Operator("BlueLine Express", "#0F4C81", 4.6),
                new Operator("Metro Star", "#7C3AED", 4.3),
                new Operator("GreenWay Lines", "#22A06B", 4.8),
                new Operator("Sunset Coach", "#E8590C", 4.1)));

        // Popular routes first so they keep the order of the design's cards.
        List<Route> routeList = new ArrayList<>();
        for (Popular p : POPULAR) {
            routeList.add(new Route(p.origin(), p.destination(), p.minutes(), BigDecimal.valueOf(p.fromPrice()), true));
        }
        for (String from : CITIES) {
            for (String to : CITIES) {
                if (from.equals(to) || isPopular(from, to)) continue;
                Popular reverse = popularBetween(to, from);
                int price = reverse != null ? reverse.fromPrice() : 16 + (from.length() * 7 + to.length() * 3) % 24;
                int minutes = reverse != null ? reverse.minutes() : 210 + (from.length() + to.length()) % 4 * 60;
                routeList.add(new Route(from, to, minutes, BigDecimal.valueOf(price), false));
            }
        }
        routeList = routes.saveAll(routeList);

        LocalDate today = LocalDate.now();
        for (Route route : routeList) {
            List<Trip> routeTrips = new ArrayList<>();
            for (int day = 0; day < days; day++) {
                for (int i = 0; i < DEPARTURES.length; i++) {
                    LocalDateTime dep = LocalDateTime.of(today.plusDays(day), DEPARTURES[i]);
                    routeTrips.add(new Trip(route, ops.get(i % ops.size()), CoachType.values()[i % 3], dep,
                            dep.plusMinutes(route.getDurationMinutes()),
                            route.getFromPrice().add(BigDecimal.valueOf(PRICE_ADD[i]))));
                }
            }
            routeTrips = trips.saveAll(routeTrips);

            // Popular routes get some seats already sold, so the seat map shows booked seats.
            if (route.isPopular()) {
                for (int n = 0; n < routeTrips.size(); n++) {
                    seedStationSale(routeTrips.get(n), n % DEPARTURES.length);
                }
            }
        }

        seedSampleBooking(routeList, ops.get(1));
    }

    private void seedStationSale(Trip trip, int pattern) {
        List<String> seats = SeatLayout.allSeats().stream()
                .filter(s -> (pattern * 7 + SeatLayout.rowOf(s) * 3 + s.charAt(s.length() - 1)) % 5 == 0)
                .toList();
        if (seats.isEmpty()) return;
        save("HC-" + seedBookingCounter++, trip, "Station sale", "station@hcoach.example", seats);
    }

    /** The completed trip from the design, so "Find my booking" has something to find: HC-48213 / sam@example.com. */
    private void seedSampleBooking(List<Route> routeList, Operator metroStar) {
        Route route = routeList.stream()
                .filter(r -> r.getOrigin().equals("Chicago") && r.getDestination().equals("Detroit"))
                .findFirst().orElseThrow();
        LocalDateTime dep = LocalDateTime.of(2025, 1, 12, 8, 30);
        Trip past = trips.save(new Trip(route, metroStar, CoachType.STANDARD, dep,
                dep.plusMinutes(route.getDurationMinutes()), route.getFromPrice()));
        save("HC-48213", past, "Sam Rivera", "sam@example.com", List.of("3A"));
    }

    private void save(String code, Trip trip, String name, String email, List<String> seats) {
        QuoteResponse price = pricing.quote(trip, seats, null);
        bookings.save(new Booking(code, trip, name, email, null, seats,
                price.fare(), price.discount(), price.tax(), price.total(), null, PaymentMethod.CARD));
    }

    private static boolean isPopular(String from, String to) {
        return popularBetween(from, to) != null;
    }

    private static Popular popularBetween(String from, String to) {
        return POPULAR.stream()
                .filter(p -> p.origin().equals(from) && p.destination().equals(to))
                .findFirst().orElse(null);
    }
}
