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
then adds sample data centred on **Ho Chi Minh City**: intercity routes between the city's bus stations
(Bến xe Miền Đông mới, Miền Tây, An Sương) and 12 destinations (Đà Lạt, Vũng Tàu, Cần Thơ, Nha Trang,
Phan Thiết, Châu Đốc, Rạch Giá, Buôn Ma Thuột, Tây Ninh, Bến Tre, Cà Mau, Quy Nhơn) in both directions,
4 fictional operators with 24 coaches, 4 promo codes, and 6 trips per route per day for the next 14 days
(`app.seed.days`). All prices are in whole **VND**. On later starts it only adds trips for days that have
none, and it never touches riders' bookings. A sample completed booking exists for "Find my booking":
`HC-48213` / `sam@example.com`.

> **Upgrading from an older version?** The tables changed (coaches, promo codes, HCMC data). Drop the old
> database once so it is rebuilt: `DROP DATABASE hcoach;` in MySQL, then start the app. (The later
> station-position columns need no drop: they are added on start and filled in for the sample stations.)

Settings come from environment variables, or from a `.env` file next to `pom.xml` (copy `.env.example`;
git ignores `.env`), so no secret is stored in the code:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | local `hcoach` database | JDBC URL |
| `DB_USER` | `root` | MySQL user |
| `DB_PASSWORD` | empty | MySQL password |
| `JWT_SECRET` | random per start | Signs login tokens. 32+ characters. **Set it** anywhere but local dev, or everyone is logged out on restart |
| `APP_PUBLIC_URL` | `http://localhost:8081` | Start of the email verification link |
| `TOMTOM_API_KEY` | empty | Server-side TomTom key for live traffic (see [Live traffic](#live-traffic-tomtom-or-google-maps)) |
| `GOOGLE_MAPS_API_KEY` | empty | Alternative: Google key with the Routes API (used instead of TomTom when both are set) |
| `TRAFFIC_PROVIDER` | `auto` | `auto` (whichever key is set), `tomtom`, `google`, `simulated` or `off` |

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
├── model/        Operator, Coach, Route, Trip, Booking, PromoCode, User, UserToken, AuditLog
│                 (+ Auditable base class with created/updated at/by)
├── repository/   Spring Data repositories + Specs (search filters)
├── dto/          Request/response records; PageParams/PageResponse for paged lists; manage/ for staff
├── service/      TripService, BookingService, PricingService, SeatLayout, AuthService, UserService, AuditService
│   └── manage/   Operator, Coach, Route, Trip, PromoCode, Booking and User admin services
├── security/     SecurityConfig (who may call what), JWT config, TokenService
├── controller/   REST endpoints under /api/v1 (manage/ = manager & admin endpoints)
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

Passwords: 8–72 characters with upper- and lower-case letters, a number and a symbol.
Until real email is added, the verification link is written to the application log (`[dev mail]`).

### Trips and bookings

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/cities` | Cities for the From / To dropdowns |
| GET | `/api/v1/routes` | Every route: cities, stations, km, timetable duration, cheapest fare, station map positions |
| GET | `/api/v1/routes/popular` | Popular route cards (cheapest fare, duration) |
| GET | `/api/v1/trips/search?from=TP. Hồ Chí Minh&to=Đà Lạt&date=2026-11-20` | Scheduled trips that day that haven't departed |
| GET | `/api/v1/trips/{id}` | One trip |
| GET | `/api/v1/trips/{id}/seats` | Seat plan and booked seats |
| POST | `/api/v1/bookings/quote` | Price for `{tripId, seats, promoCode}` |
| POST | `/api/v1/bookings` | Book: `{tripId, passengerName, email, phone, seats, promoCode, paymentMethod}`. With a token, saved on the account |
| GET 🔒 | `/api/v1/bookings/mine` | Bookings made while logged in |
| POST | `/api/v1/bookings/lookup` | Find a booking by `{bookingCode, email}` |
| POST | `/api/v1/bookings/cancel` | Cancel a booking by `{bookingCode, email}` |
| GET | `/api/v1/routes/{id}/traffic` | Live traffic on a route (see below) |
| GET | `/api/v1/trips/{id}/traffic` | Traffic on the trip's route, plus an arrival estimate if it leaves within 3 hours |

Search results and popular routes include the route's id (`routeId` / `id`) for the traffic calls.

The frontend in `../frontend/hcoach` uses `/api/v1/routes` and the traffic endpoint on its **Routes**
screen. Serve it with VS Code Live Server (port 5500), which the backend's CORS settings allow
(`app.cors.allowed-origins`, or the `CORS_ORIGINS` environment variable).

### Live traffic (TomTom or Google Maps)

`GET /api/v1/routes/{id}/traffic` asks **TomTom's Routing API** (or Google's Routes API, if that key is
set instead) how long the drive between the route's two stations takes right now:

```json
{"routeId":1,"origin":"TP. Hồ Chí Minh","destination":"Đà Lạt","source":"TOMTOM",
 "scheduledMinutes":420,"typicalMinutes":395,"currentMinutes":452,"delayMinutes":57,"level":"MODERATE",
 "distanceKm":305.4,"encodedPolyline":"...","speedIntervals":[],"checkedAt":"2026-11-23T17:05:12",
 "stale":false,"note":null}
```

- `level`: `LIGHT` (under 10 % slower than an empty road), `MODERATE` (10–30 %), `HEAVY` (30 % or more).
- `encodedPolyline` is the road path in Google's encoded-polyline format (whichever provider answered),
  for drawing on a map; Leaflet, MapLibre and Google Maps all have decoders. `speedIntervals` split that
  line into `NORMAL` / `SLOW` / `TRAFFIC_JAM` stretches (always with TomTom; with Google only when
  `app.traffic.speed-intervals=true`).
- Stations are sent by map position (`originLat/Lng`, `destinationLat/Lng` on the route; the sample
  positions are approximate and can be corrected with `PUT /api/v1/manage/routes/{id}`). TomTom needs
  them; Google can fall back to the station name.
- **Cost control.** One answer per route is reused for 5 minutes (`app.traffic.cache-ttl`) however many
  people ask, and at most 160 requests a day are sent (`app.traffic.daily-limit`). TomTom's free plan
  allows about 2,500 a day, so this can be raised; Google bills each request. Past the limit, or if the
  provider fails, the last answer is returned with `"stale": true` and a `note`; with no earlier answer
  the call fails with `503 TRAFFIC_UNAVAILABLE`. The daily count is kept in memory and restarts with the app.
- **Without a key** the feature is off (503), except in the h2 mode, which returns made-up rush-hour
  numbers marked `"source":"SIMULATED"`. `TRAFFIC_PROVIDER=simulated` does the same anywhere.
- Managers can check the setup and today's usage at `GET /api/v1/manage/traffic/status`.

Getting a TomTom key: sign up at developer.tomtom.com (no credit card), copy the key from the
dashboard, and put `TOMTOM_API_KEY=...` in `.env`. Keep it on the server only; a map in the browser needs
its own key.

### Management (🔒 manager or admin)

Every list takes `page` (from 1), `size` (10, 20, 50 or 100) and one or more `sort=field,asc|desc`, and
returns `{content, page, size, totalElements, totalPages, from, to, sort}`; `from`/`to` give
"Showing 21–40 of 156". Each record includes `audit: {createdAt, createdBy, updatedAt, updatedBy}`.
Every change is also written to the audit log.

| Resource | Endpoints | Filters | Rules |
|---|---|---|---|
| Operators | `/api/v1/manage/operators` GET, POST, `/{id}` GET, PUT, DELETE, `/{id}/restore` POST | q, active, minRating, maxRating, createdFrom, createdTo | Unique name; can't remove while it has coaches in service |
| Coaches | `/api/v1/manage/coaches` (same set) | q (plate), operatorId, type, status, minSeats, maxSeats, yearFrom, yearTo | Plate like `51B-123.45`, unique; 3–12 rows; with upcoming trips can't be retired, resized, moved or sent to maintenance |
| Routes | `/api/v1/manage/routes` (same set) | q, origin, destination, active, popular, minPrice, maxPrice, maxDurationMinutes | Different cities; unique pair; with upcoming trips can't be removed or change cities |
| Trips | `/api/v1/manage/trips` GET, POST, `/{id}` GET, PUT, DELETE (`?reason=`) | routeId, origin, destination, operatorId, coachId, coachType, status, departureFrom/To, minPrice, maxPrice | Future departure; arrival 10 min–48 h later; coach not on an overlapping trip; route fixed once seats are sold; a new coach must have every sold seat; DELETE cancels the trip **and all its bookings** |
| Promo codes | `/api/v1/manage/promo-codes` (same set as operators) | q, active, validOn, minDiscount, maxDiscount | validTo ≥ validFrom; used codes can't be renamed; limit ≥ uses |
| Bookings | `/api/v1/manage/bookings` GET, `/{code}` GET | q, status, tripId, routeId, operatorId, departureFrom/To, createdFrom/To | Read-only |

DELETE on operators, coaches, routes and promo codes is a **soft delete** (the record is kept and can be
restored), so past bookings never lose their history.

### Admin (🔒 admin only)

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/admin/users` | Filters: q, role, status, createdFrom, createdTo |
| POST | `/api/v1/admin/users` | Create an active, verified account with any role |
| GET | `/api/v1/admin/users/{id}` | One user |
| PATCH | `/api/v1/admin/users/{id}/role` | `{role}`; logs the user out |
| POST | `/api/v1/admin/users/{id}/deactivate` · `/reactivate` | Soft delete / undo; deactivating logs the user out |
| GET | `/api/v1/admin/audit-logs` | Who changed what. Filters: entityType, entityId, actor, action, from, to |

An admin can't change their own role or deactivate themselves, and the last active admin is protected.

### Errors

Every error is RFC 7807 JSON with a machine-readable `code`; validation errors also list each field:

```json
{"status":400,"title":"Bad Request","detail":"Some fields are not valid","code":"VALIDATION_FAILED",
 "errors":[{"field":"password","message":"must be 8-72 characters and include ..."}]}
```

Codes: `VALIDATION_FAILED`, `BAD_REQUEST`, `INVALID_TOKEN`, `WRONG_PASSWORD` (400) · `UNAUTHORIZED`,
`INVALID_CREDENTIALS`, `REFRESH_TOKEN_INVALID` (401) · `FORBIDDEN`, `EMAIL_NOT_VERIFIED`, `ACCOUNT_DISABLED` (403) ·
`NOT_FOUND` (404) · `CONFLICT`, `EMAIL_TAKEN`, `SEAT_TAKEN`, `TRIP_DEPARTED`, `TRIP_CANCELLED`, `IN_USE`,
`DUPLICATE`, `SCHEDULE_CONFLICT`, `BOOKING_RULE_VIOLATED` (409) ·
`INTERNAL_ERROR` (500) · `TRAFFIC_UNAVAILABLE` (503).

## Rules the backend enforces

- **Login** uses a 15-minute JWT access token (HS256) and a 7-day refresh token. Refresh tokens are
  single-use; replaying an old one revokes all of that user's sessions. Only SHA-256 hashes of refresh and
  verification tokens are stored. Passwords are hashed with BCrypt (cost 12).
- **Roles**: Customer, Driver, Manager, Admin. New sign-ups are customers; `/api/v1/manage/**` needs Manager
  or Admin, `/api/v1/admin/**` is admin-only.
- **Security headers**: Content-Security-Policy, X-Frame-Options, X-Content-Type-Options, Referrer-Policy.
  CSRF tokens are not needed because the token travels in a header, not a cookie.

- **Price** is always calculated on the server in whole VND: seat price, +30.000 ₫ for rows 1–2, promo
  discount, then 8% VAT.
- **Promo codes** (table `promo_codes`) can have start/end dates, a usage limit and a minimum fare. The code's
  row is locked while booking so a limited code can't be over-used. Samples: `FIRSTRIDE` 10%, `WEEKEND` 15%
  on fares ≥ 300.000 ₫, `TET2027` (not valid yet), `HELLOSAIGON` (expired).
- **Seats** are rows of A–D; the number of rows comes from the coach (3–12). A seat can't be booked twice; the
  trip row is locked while booking. At most 6 seats per booking. Cancelled trips can't be booked.
- **Bookings** can only be viewed or cancelled with both the booking code and the matching email.
- **Status** shown as Upcoming / Completed / Cancelled; Completed means the departure time has passed.

## Not built yet (placeholders in the design)

- Login / sign up screens on the bundled page (the API exists; the page still uses booking codes for My Trips).
- Real payment. The card fields on the page are never read or sent; a booking is confirmed without charging.
- Confirmation emails, the FAMILY deal, and the "24 hours before departure" cancellation limit.
- The bundled page still has US-themed marketing text (hero, reviews, deals); prices and stations are real data.

## Next steps

See `../docs/BACKEND_PLAN.md`. Next (phase 3): booking holds that expire, cancellation/refund policy,
dynamic pricing, trip status workflow (boarding → departed → arrived), driver assignment and passenger lists.
