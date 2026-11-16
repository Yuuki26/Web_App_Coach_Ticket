# H Coach – coach ticket booking

Spring Boot 4.1 · Java 17+ · Spring Web MVC · Spring Data JPA · H2 (in-memory) · Bean Validation.
The web page (plain HTML, CSS and JavaScript) is served by the same app.

## Run

```bash
mvnw.cmd spring-boot:run
```

Then open http://localhost:8081. (Port 8081 because another Tomcat already uses 8080 on this machine;
change `server.port` in `application.properties` if needed.)

Database console: http://localhost:8081/h2-console (JDBC URL `jdbc:h2:mem:busdb`, user `sa`, empty password).
Data is in memory and is rebuilt on every start by `config/DataSeeder.java`:
11 cities, 4 operators, every city pair as a route, 6 trips a day for 14 days (`app.seed.days`).
A sample completed booking exists for "Find my booking": `HC-48213` / `sam@example.com`.

## Structure

```
src/main/java/com/busbooking
├── model/        Route, Operator, Trip, Booking (+ enums)
├── repository/   Spring Data repositories
├── dto/          Request/response records exchanged with the page
├── service/      TripService, BookingService, PricingService, SeatLayout
├── controller/   REST endpoints under /api
├── exception/    Custom exceptions + JSON error handler
└── config/       CORS, sample data seeder
src/main/resources/static
├── index.html    Page shell
├── styles.css    Styles from the H Coach design
└── app.js        Screens and API calls
```

## API

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/cities` | Cities for the From / To dropdowns |
| GET | `/api/routes/popular` | Popular route cards (cheapest fare, duration) |
| GET | `/api/trips/search?from=New York&to=Boston&date=2026-11-20` | Trips that day that haven't departed |
| GET | `/api/trips/{id}` | One trip |
| GET | `/api/trips/{id}/seats` | Seat plan and booked seats |
| POST | `/api/bookings/quote` | Price for `{tripId, seats, promoCode}` |
| POST | `/api/bookings` | Book: `{tripId, passengerName, email, phone, seats, promoCode, paymentMethod}` |
| POST | `/api/bookings/lookup` | Find a booking by `{bookingCode, email}` |
| POST | `/api/bookings/cancel` | Cancel a booking by `{bookingCode, email}` |

Errors are JSON with a `detail` message: `400` invalid input, `404` not found, `409` seat taken / trip departed.

## Rules the backend enforces

- **Price** is always calculated on the server: seat price, +$5 for rows 1–2, promo discount, then 8% tax.
  Promo codes live in `PricingService` (`FIRSTRIDE` = 10% off).
- **Seats** are 9 rows of A–D (`SeatLayout`). A seat can't be booked twice; the trip row is locked while booking.
- **Bookings** can only be viewed or cancelled with both the booking code and the matching email.
- **Status** shown as Upcoming / Completed / Cancelled; Completed means the departure time has passed.

## Not built yet (placeholders in the design)

- Login / sign up. My Trips remembers booking codes in the browser instead of an account.
- Real payment. The card fields on the page are never read or sent; a booking is confirmed without charging.
- Confirmation emails, the WEEKEND and FAMILY deals, and the "24 hours before departure" cancellation limit.

## Next steps

- Switch H2 to MySQL/PostgreSQL so bookings survive a restart.
- Add Spring Security for user accounts and an admin area to manage routes, trips and promo codes.
