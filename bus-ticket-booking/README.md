# H Coach – coach ticket booking

Spring Boot 4.1 · Java 17+ · Spring Web MVC · Spring Data JPA · MySQL · Bean Validation.
The web page (plain HTML, CSS and JavaScript) is served by the same app.

## Run

The app stores its data in **MySQL**. You need MySQL Server running on `localhost:3306`.

1. Tell the app your MySQL login (once per terminal window; the user defaults to `root`):

   ```bash
   set DB_PASSWORD=your-mysql-password
   ```

2. Start it:

   ```bash
   mvnw.cmd spring-boot:run
   ```

3. Open http://localhost:8081. (Port 8081 because another Tomcat already uses 8080 on this machine;
   change `server.port` in `application.properties` if needed.)

The `hcoach` database and its tables are created automatically on first start. `config/DataSeeder.java`
then adds sample data: 11 cities, 4 operators, every city pair as a route, and 6 trips a day for the next
14 days (`app.seed.days`). On later starts it only adds trips for days that have none, and it never
touches riders' bookings. A sample completed booking exists for "Find my booking":
`HC-48213` / `sam@example.com`.

Settings come from environment variables so no password is stored in the code:
`DB_USER` (default `root`), `DB_PASSWORD` (default empty), `DB_URL` (default local `hcoach` database).

### Run without MySQL

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
```

This uses a database held in memory: nothing to install, but all data is lost when the app stops.
The automated tests use this mode too. Its console is at http://localhost:8081/h2-console
(JDBC URL `jdbc:h2:mem:busdb`, user `sa`, empty password).

## Structure

```
src/main/java/com/busbooking
├── model/        Route, Operator, Trip, Booking (+ enums)
├── repository/   Spring Data repositories
├── dto/          Request/response records exchanged with the page
├── service/      TripService, BookingService, PricingService, SeatLayout
├── controller/   REST endpoints under /api/v1
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
| GET | `/api/v1/cities` | Cities for the From / To dropdowns |
| GET | `/api/v1/routes/popular` | Popular route cards (cheapest fare, duration) |
| GET | `/api/v1/trips/search?from=New York&to=Boston&date=2026-11-20` | Trips that day that haven't departed |
| GET | `/api/v1/trips/{id}` | One trip |
| GET | `/api/v1/trips/{id}/seats` | Seat plan and booked seats |
| POST | `/api/v1/bookings/quote` | Price for `{tripId, seats, promoCode}` |
| POST | `/api/v1/bookings` | Book: `{tripId, passengerName, email, phone, seats, promoCode, paymentMethod}` |
| POST | `/api/v1/bookings/lookup` | Find a booking by `{bookingCode, email}` |
| POST | `/api/v1/bookings/cancel` | Cancel a booking by `{bookingCode, email}` |

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

- Add Spring Security for user accounts and an admin area to manage routes, trips and promo codes.
