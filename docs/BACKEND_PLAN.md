# Backend gap plan – H Coach (Topic H: Coach Ticket Selling)

Compares the current Spring Boot backend in `bus-ticket-booking/` against
`final_project_description_v3.pdf`. Scope: **backend only** — the database schema and the
frontend belong to the other team members, so the items marked **🤝 agree first** need a
decision together before code is written.

Legend: ✅ done · 🟡 partly done · ❌ missing

---

## 1. Where the backend stands today

| Area | Status | What exists |
|---|---|---|
| Trip search, seat map, booking, lookup, cancel | ✅ | `TripService`, `BookingService`, `/api/...` endpoints |
| Seat conflict detection | ✅ | Row lock on the trip while booking (`TripRepository.findByIdForUpdate`) |
| Price calculation (legroom, promo, 8% tax) | ✅ | `PricingService` — server side only |
| Server-side validation | 🟡 | Bean Validation on requests; no cross-field or database-state rules beyond booking |
| Error responses | 🟡 | `ProblemDetail` JSON for 400/404/409; no error codes, no 401/403/500 handling |
| MySQL | 🟡 | Config switched; never run against a real MySQL server (not committed as of last session) |
| Sample data | ✅ | ~9,000 trips, 110 routes, bookings (seeder in Java, not a SQL script) |
| Tests | 🟡 | 5 tests (booking flow, pricing, lookup); no API/security tests |
| Users, login, roles | ❌ | None — anyone can call every endpoint |
| CRUD for managers/admins | ❌ | Everything is read-only except bookings |
| Pagination / filtering / sorting | ❌ | Search returns a plain list |
| Audit trail, soft delete | ❌ | |
| Advanced features (need 3) | ❌ | E-ticket QR on the page is decorative only |
| API docs, Docker, deployment | ❌ | |

---

## 2. Requirement-by-requirement gaps

### 2.1 User management & security (PDF §2.1-1, §2.2 Security — 4 + 5 grading points)

- ❌ `User` entity: email (unique), password hash, full name, phone, role, status
  (ACTIVE / DEACTIVATED / PENDING_VERIFICATION), last login, audit columns.
- ❌ **Roles (need ≥3):** `CUSTOMER`, `DRIVER`, `MANAGER`, `ADMIN` (topic H lists Customer, Driver, Manager).
- ❌ Spring Security with **BCrypt** password hashing.
- ❌ Authentication: **JWT access token + refresh token** (recommended because the frontend is a
  separate app) — or server sessions with CSRF tokens. 🤝 agree with the frontend owner.
- ❌ Registration with **email verification** (token emailed, account active after click).
- ❌ Login / logout (logout = revoke refresh token).
- ❌ Profile: view, edit, change password (requires current password).
- ❌ Admin user management: create user, deactivate/reactivate, change role.
- ❌ **Rate limiting** on login and registration (e.g. 5 failed logins per 15 min per email + IP).
- ❌ Security headers: Content-Security-Policy, X-Content-Type-Options, frame options.
- 🟡 SQL injection: already safe (JPA parameters only) — just document it.
- 🟡 XSS: the page escapes output; add server-side sanitizing of free-text fields (names, notes).

### 2.2 Role-based access control (PDF §2.1-1)

| Role | Can do |
|---|---|
| Guest | Search trips, view seat map, see popular routes |
| Customer | Book, pay, view **own** bookings, cancel own bookings, profile |
| Driver | View **assigned** trips, passenger manifest, check passengers in, update trip status |
| Manager | CRUD routes, coaches, trips, operators, promo codes; assign drivers; reports |
| Admin | Everything a manager can, plus user management and audit log |

- ❌ Enforce with `@PreAuthorize` on service methods **and** ownership checks
  (a customer can only read their own booking; a driver only their own trips).
- 🔁 Change: bookings become linked to a `User` (guest checkout by code + email can stay as an extra).

### 2.3 CRUD for ≥4 entities with 6–8 attributes (PDF §2.1-2 — 5 points)

Endpoints follow `/api/v1/{resource}` with GET (list/one), POST, PUT/PATCH, DELETE.

| Entity | Status | Notes |
|---|---|---|
| Route | 🟡 read only | Add CRUD; add stops/distance; **cannot delete a route with upcoming trips** |
| Coach | ❌ new | plate number, operator, coach type, seat layout, year, status (ACTIVE / MAINTENANCE / RETIRED) |
| Trip | 🟡 read only | Add CRUD; validate arrival > departure; coach and driver not double-booked |
| Operator | 🟡 read only | Add CRUD |
| Promo code | ❌ new (hard-coded today) | code, discount %, valid from/to, usage limit, used count, min fare, active |
| Booking | 🟡 | Add manager view/search; customer "my bookings" |
| Payment | ❌ new | amount, method, status, transaction ref, paid at, refund amount |
| User | ❌ new | see 2.1 |

- ❌ **Soft delete** for Route, Coach, Trip, Operator, User (`active` flag / `deleted_at`).
- ❌ **Cascading:** cancelling a trip cancels its bookings and marks payments for refund;
  deactivating a user keeps their bookings (history) but blocks login.
- ❌ **Audit columns** on every table via Spring Data JPA auditing
  (`created_at`, `updated_at`, `created_by`, `updated_by`) + an `audit_log` table for important
  actions (who, what, when, old → new value).

### 2.4 Search, filtering, pagination (PDF §2.1-3 — 4 points)

- ❌ Paginated responses (`page`, `size` ∈ {10, 20, 50, 100}, `totalElements`, `totalPages`) so the
  frontend can show "Showing 21–40 of 156".
- ❌ Multi-column sorting (`sort=price,asc&sort=departureTime,asc`).
- ❌ **≥5 filters per entity**, built with JPA Specifications, e.g.:
  - Trips: origin, destination, date range, price range, coach type, operator, min seats left, status
  - Bookings: code, email/customer, status, trip date range, route, payment status
  - Users: name/email text, role, status, registered date range
  - Coaches: plate, operator, type, status, seat count
- ❌ Multi-field text search (`q=` matches several columns).
- Optional: saved searches per user.

### 2.5 Data model / relationships (PDF §2.1-4, Database section) 🤝 agree first

The PDF needs **6–8 tables (excluding junctions), 2 junction tables, 50+ columns, 3NF**.
Today there are 4 tables (`routes`, `operators`, `trips`, `bookings`) + `booking_seats`.

Proposed model to align with the DB owner:

```
users ─┬─< bookings >── trips >── routes
       │      │           │  └──< trip_drivers >── users(DRIVER)     (junction 1)
       │      └─< payments│
       │      └─< booked_seats                       (one row per seat; unique trip+seat)
       └─< refresh_tokens  └── coaches >── operators
                                 └──< coach_amenities >── amenities  (junction 2)
promo_codes ─< bookings
audit_log
```

- Tables (non-junction): users, routes, operators, coaches, trips, bookings, booked_seats,
  payments, promo_codes, amenities, audit_log → 11 ✔
- Junctions: `trip_drivers`, `coach_amenities` ✔
- `booked_seats` with a **unique (trip_id, seat_label) constraint** gives database-level
  double-booking protection on top of the row lock.
- 🤝 **Who owns the schema:** today Hibernate creates tables (`ddl-auto=update`). The PDF wants
  `database/schema.sql` + `seed-data.sql`. Recommended: DB owner writes `schema.sql`, backend switches
  to `ddl-auto=validate` (or Flyway migrations) so the entity classes must match the real schema.

### 2.6 Business logic (PDF §2.1-5 — 5 points)

Already: seat availability, conflict detection, price/tax/promo, departed-trip check.

Missing:
- ❌ **Booking workflow:** `PENDING` (seats held, 10–15 min) → `CONFIRMED` (paid) → `COMPLETED` /
  `CANCELLED` / `EXPIRED`. Held seats count as taken.
- ❌ **Automatic expiry:** scheduled job releases unpaid holds.
- ❌ **Cancellation policy:** free until 24 h before departure, 50% refund until 2 h, none after
  (matches the "free cancellation up to 24 hours" text on the page).
- ❌ **Dynamic pricing** (named in topic H): price rises with occupancy and as departure nears;
  shown in quote and locked at booking time.
- ❌ **Trip status workflow:** SCHEDULED → BOARDING → DEPARTED → ARRIVED (or CANCELLED); only
  allowed transitions; driver/manager only.
- ❌ **Resource conflicts:** a coach or driver cannot be assigned to overlapping trips; coach in
  MAINTENANCE cannot be scheduled.
- ❌ **Promo rules:** validity dates, usage limit, one use per customer, minimum fare.
- ❌ Max seats per booking (e.g. 6); database-state rules such as "cannot delete an operator with
  active coaches".
- ❌ Scheduled tasks: expire holds, mark trips/bookings completed, generate future trips,
  send reminders.

### 2.7 Advanced features — pick at least 3 (PDF §2.1 "Advanced Features" — 8 points)

Recommended three (all fit the topic and are mostly backend work):

1. **Email system** — verification email, booking confirmation, departure reminder 24 h before,
   cancellation/refund notice (Spring Mail + templates; Mailtrap/Gmail SMTP for the demo).
2. **E-ticket generation** — real QR code (ZXing) inside a downloadable PDF ticket (OpenPDF);
   driver scans/enters the code to check a passenger in.
3. **Manager dashboard & reports** — endpoints for revenue per day/route, occupancy rate,
   top routes, cancellations; CSV/Excel export (frontend draws the charts).

Good extras if time allows: scheduled automation (already needed for 2.6), Server-Sent Events for
live seat-map updates, VNPay/Stripe **sandbox** payment, SMS reminder.

### 2.8 API standards & error handling (PDF §2.2 Backend)

- ❌ Version prefix: move all endpoints to `/api/v1/...` 🤝 tell the frontend owner.
- ❌ Use PUT/PATCH/DELETE where appropriate (today cancel/lookup are POST — keep lookup as POST
  for privacy, make cancel `PATCH /api/v1/bookings/{code}/cancel` for logged-in users).
- ❌ Status codes 401/403 from Spring Security; 500 handler that never leaks stack traces.
- ❌ **Error codes** in every error body (e.g. `SEAT_TAKEN`, `TRIP_DEPARTED`, `VALIDATION_FAILED`),
  per-field validation errors as a list, and handlers for `DataIntegrityViolationException`,
  `AccessDeniedException`, unexpected exceptions.
- ❌ Dev vs prod error detail via Spring profiles.
- ❌ **OpenAPI/Swagger** (springdoc) → satisfies "API documentation" and helps the frontend owner.

### 2.9 Logging & monitoring

- ❌ Log logins (success/failure), data changes, errors — with timestamp, user, action, result.
- ❌ `logback-spring.xml` with log levels per package and **rolling files** (size/time based).
- ❌ Spring Boot Actuator `/actuator/health` for the deployment platform.

### 2.10 Testing (PDF report §6 — 10–15 scenarios)

- ❌ Controller tests with MockMvc, including security (401 without token, 403 wrong role).
- ❌ Scenarios to add: duplicate email registration, wrong password lockout, expired hold
  releases seats, cancellation refund tiers, overlapping driver assignment rejected, promo limit
  reached, pagination totals, soft-deleted route hidden from search.

### 2.11 Repository, deployment & submission (PDF §2.2 Version Control, §5.1, §5.2)

- ❌ Rename/move `bus-ticket-booking/` → `backend/`, add `database/`, `frontend/`, `docs/` per §5.2.
- ❌ `docker-compose.yml` (MySQL + backend [+ frontend]) and a backend `Dockerfile`.
- ❌ `.env.example` (DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, MAIL_*), no secrets in Git.
- ❌ Deploy to a public URL (Railway / Render / Fly.io all support MySQL or a MySQL add-on).
- ❌ Seed **test accounts** exactly as the PDF lists: `admin@test.com / Admin123!`,
  `manager@test.com / Manager123!`, `customer@test.com / Customer123!` (+ a driver account).
- ❌ `LICENSE`, `docs/API.md`, `docs/DEPLOYMENT.md`, `CONTRIBUTING.md`.
- ⚠️ Process requirements: 50+ commits total, **20+ per person**, `type(scope): message` messages,
  feature branches (`feature/user-auth`) merged by pull request. Work in small commits from now on.

---

## 3. Suggested order of work

Each phase unblocks the next; phase 1 also unblocks both teammates.

| Phase | Work | Why first |
|---|---|---|
| **0. Agree** 🤝 | Final ER model (2.5), who owns the schema, JWT vs session, `/api/v1` contract, error format | Avoids rework for all three people |
| **1. Foundation** | `User` + roles, Spring Security, BCrypt, JWT/refresh, register/login/logout, profile, `/api/v1`, error codes, Swagger, test accounts | Frontend can build login and all pages against real endpoints |
| **2. Management CRUD** | Coach, Route, Trip, Operator, PromoCode, User admin; soft delete; JPA auditing + audit log; pagination/filter/sort | Biggest chunk of grading points (CRUD + search) |
| **3. Business logic** | Booking hold → payment → confirm, auto-expiry job, cancellation policy, dynamic pricing, trip status workflow, driver assignment + manifest, conflict checks | "Complex business logic" points |
| **4. Advanced features** | Email (verify/confirm/remind), PDF e-ticket with QR, manager reports + CSV/Excel export | The 3 required advanced features |
| **5. Hardening & ship** | Rate limiting, security headers, logging/rotation, Actuator, 10–15 tests, Docker, deployment, docs | Stability and deployment are graded at the demo |

## 4. Questions for the team

1. Is the frontend a separate app (React etc.) or the current static page inside Spring Boot?
   This decides JWT vs session and whether CORS matters.
2. Will the DB owner write `schema.sql` by hand (backend validates against it), or generate it
   from the entities?
3. Which week of the 5 are we in? Determines how much of phase 4 is realistic.
