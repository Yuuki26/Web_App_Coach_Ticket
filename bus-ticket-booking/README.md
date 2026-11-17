# H Coach – coach ticket booking

Spring Boot 4.1 · Java 17+ · Spring Web MVC · Spring Data JPA · Spring Security (JWT) · MySQL · Bean Validation · springdoc OpenAPI.
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

Settings come from environment variables so no secret is stored in the code (see `.env.example`):

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | local `hcoach` database | JDBC URL |
| `DB_USER` | `root` | MySQL user |
| `DB_PASSWORD` | empty | MySQL password |
| `JWT_SECRET` | random per start | Signs login tokens. 32+ characters. **Set it** anywhere but local dev, or everyone is logged out on restart |
| `APP_PUBLIC_URL` | `http://localhost:8081` | Start of the email verification link |

API documentation: http://localhost:8081/swagger-ui.html (click **Authorize** and paste an access token).

### Test accounts

Created on startup (`app.seed.demo-users=true`), already verified:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@test.com` | `Admin123!` |
| Manager | `manager@test.com` | `Manager123!` |
| Driver | `driver@test.com` | `Driver123!` |
| Customer | `customer@test.com` | `Customer123!` |

### Run without MySQL

```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
```

This uses a database held in memory: nothing to install, but all data is lost when the app stops.
The automated tests use this mode too (`mvnw.cmd test`). Its console is at http://localhost:8081/h2-console
(JDBC URL `jdbc:h2:mem:busdb`, user `sa`, empty password).

## Structure

```
src/main/java/com/busbooking
├── model/        Route, Operator, Trip, Booking, User, UserToken (+ enums)
├── repository/   Spring Data repositories
├── dto/          Request/response records exchanged with the page
├── service/      TripService, BookingService, PricingService, SeatLayout, AuthService, UserService
├── security/     SecurityConfig (who may call what), JWT config, TokenService
├── controller/   REST endpoints under /api/v1
├── exception/    Custom exceptions + JSON error handler
└── config/       CORS, OpenAPI, sample data and demo account seeders
src/main/resources/static
├── index.html    Page shell
├── styles.css    Styles from the H Coach design
└── app.js        Screens and API calls
```

## API

Full, always-current reference: `/swagger-ui.html`. 🔓 = no login needed, 🔒 = needs
`Authorization: Bearer <accessToken>`.

### Accounts

| Method | Path | Purpose |
|---|---|---|
| POST 🔓 | `/api/v1/auth/register` | `{email, password, fullName, phone}` → customer account, verification link sent |
| POST 🔓 | `/api/v1/auth/verify-email` | `{token}` from the link → account active |
| POST 🔓 | `/api/v1/auth/resend-verification` | `{email}` → new link (same answer whether or not the email exists) |
| POST 🔓 | `/api/v1/auth/login` | `{email, password}` → `{accessToken, expiresIn, refreshToken, user}` |
| POST 🔓 | `/api/v1/auth/refresh` | `{refreshToken}` → new token pair; each refresh token works once |
| POST 🔓 | `/api/v1/auth/logout` | `{refreshToken}` → ends that session |
| GET 🔒 | `/api/v1/users/me` | Own profile |
| PATCH 🔒 | `/api/v1/users/me` | `{fullName?, phone?}` |
| PUT 🔒 | `/api/v1/users/me/password` | `{currentPassword, newPassword}` → logs out every device |
| GET 🔒 admin | `/api/v1/admin/users` | All users |

Passwords: 8–72 characters with upper- and lower-case letters, a number and a symbol.
Until real email is added, the verification link is written to the application log (`[dev mail]`).

### Trips and bookings

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/cities` | Cities for the From / To dropdowns |
| GET | `/api/v1/routes/popular` | Popular route cards (cheapest fare, duration) |
| GET | `/api/v1/trips/search?from=New York&to=Boston&date=2026-11-20` | Trips that day that haven't departed |
| GET | `/api/v1/trips/{id}` | One trip |
| GET | `/api/v1/trips/{id}/seats` | Seat plan and booked seats |
| POST | `/api/v1/bookings/quote` | Price for `{tripId, seats, promoCode}` |
| POST | `/api/v1/bookings` | Book: `{tripId, passengerName, email, phone, seats, promoCode, paymentMethod}`. With a token, saved on the account |
| GET 🔒 | `/api/v1/bookings/mine` | Bookings made while logged in |
| POST | `/api/v1/bookings/lookup` | Find a booking by `{bookingCode, email}` |
| POST | `/api/v1/bookings/cancel` | Cancel a booking by `{bookingCode, email}` |

### Errors

Every error is RFC 7807 JSON with a machine-readable `code`; validation errors also list each field:

```json
{"status":400,"title":"Bad Request","detail":"Some fields are not valid","code":"VALIDATION_FAILED",
 "errors":[{"field":"password","message":"must be 8-72 characters and include ..."}]}
```

Codes: `VALIDATION_FAILED`, `BAD_REQUEST`, `INVALID_TOKEN`, `WRONG_PASSWORD` (400) · `UNAUTHORIZED`,
`INVALID_CREDENTIALS`, `REFRESH_TOKEN_INVALID` (401) · `FORBIDDEN`, `EMAIL_NOT_VERIFIED`, `ACCOUNT_DISABLED` (403) ·
`NOT_FOUND` (404) · `CONFLICT`, `EMAIL_TAKEN`, `SEAT_TAKEN`, `TRIP_DEPARTED`, `BOOKING_RULE_VIOLATED` (409) ·
`INTERNAL_ERROR` (500).

## Rules the backend enforces

- **Login** uses a 15-minute JWT access token (HS256) and a 7-day refresh token. Refresh tokens are
  single-use; replaying an old one revokes all of that user's sessions. Only SHA-256 hashes of refresh and
  verification tokens are stored. Passwords are hashed with BCrypt (cost 12).
- **Roles**: Customer, Driver, Manager, Admin. New sign-ups are customers; `/api/v1/admin/**` is admin-only.
- **Security headers**: Content-Security-Policy, X-Frame-Options, X-Content-Type-Options, Referrer-Policy.
  CSRF tokens are not needed because the token travels in a header, not a cookie.

- **Price** is always calculated on the server: seat price, +$5 for rows 1–2, promo discount, then 8% tax.
  Promo codes live in `PricingService` (`FIRSTRIDE` = 10% off).
- **Seats** are 9 rows of A–D (`SeatLayout`). A seat can't be booked twice; the trip row is locked while booking.
- **Bookings** can only be viewed or cancelled with both the booking code and the matching email.
- **Status** shown as Upcoming / Completed / Cancelled; Completed means the departure time has passed.

## Not built yet (placeholders in the design)

- Login / sign up screens on the bundled page (the API exists; the page still uses booking codes for My Trips).
- Real payment. The card fields on the page are never read or sent; a booking is confirmed without charging.
- Confirmation emails, the WEEKEND and FAMILY deals, and the "24 hours before departure" cancellation limit.

## Next steps

See `../docs/BACKEND_PLAN.md`. Next: admin area to manage users, routes, coaches, trips and promo codes,
with paging, filters, soft delete and an audit trail.
