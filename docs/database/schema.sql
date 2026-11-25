-- H Coach: coach ticket booking schema (MySQL 8+)
-- Run: mysql -u root -p < schema.sql

DROP DATABASE IF EXISTS hcoach;
CREATE DATABASE hcoach CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE hcoach;

-- 1. Accounts: passengers and admins. Guests do not need a row here.
CREATE TABLE users (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL,
    phone         VARCHAR(30),
    password_hash VARCHAR(255)  NOT NULL,
    role          ENUM('PASSENGER','ADMIN') NOT NULL DEFAULT 'PASSENGER',
    active        TINYINT(1)    NOT NULL DEFAULT 1,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- 2. Cities served (feeds the From / To dropdowns).
CREATE TABLE cities (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_cities_name (name)
) ENGINE=InnoDB;

-- 3. A directed city pair, e.g. New York -> Boston.
CREATE TABLE routes (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    origin_city_id      BIGINT        NOT NULL,
    destination_city_id BIGINT        NOT NULL,
    duration_minutes    INT           NOT NULL,
    distance_km         INT,
    popular             TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_routes_pair (origin_city_id, destination_city_id),
    CONSTRAINT fk_routes_origin      FOREIGN KEY (origin_city_id)      REFERENCES cities (id),
    CONSTRAINT fk_routes_destination FOREIGN KEY (destination_city_id) REFERENCES cities (id),
    CONSTRAINT ck_routes_distinct    CHECK (origin_city_id <> destination_city_id)
) ENGINE=InnoDB;

-- 4. Coach companies.
CREATE TABLE operators (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    brand_color   CHAR(7),
    rating        DECIMAL(2,1),
    contact_email VARCHAR(150),
    contact_phone VARCHAR(30),
    active        TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_operators_name (name)
) ENGINE=InnoDB;

-- 5. Seat plan per kind of coach: seat_rows x seats_per_row, first legroom_rows cost extra.
CREATE TABLE coach_types (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    name             VARCHAR(50)   NOT NULL,           -- Standard, Premium, Sleeper
    seat_rows        INT           NOT NULL,
    seats_per_row    INT           NOT NULL,           -- columns A, B, C, D ...
    legroom_rows     INT           NOT NULL DEFAULT 0,
    legroom_surcharge DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    UNIQUE KEY uq_coach_types_name (name)
) ENGINE=InnoDB;

-- 6. A physical coach (vehicle) owned by an operator.
CREATE TABLE coaches (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    operator_id   BIGINT      NOT NULL,
    coach_type_id BIGINT      NOT NULL,
    plate_number  VARCHAR(20) NOT NULL,
    active        TINYINT(1)  NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_coaches_plate (plate_number),
    CONSTRAINT fk_coaches_operator FOREIGN KEY (operator_id)   REFERENCES operators (id),
    CONSTRAINT fk_coaches_type     FOREIGN KEY (coach_type_id) REFERENCES coach_types (id)
) ENGINE=InnoDB;

-- 7. A scheduled departure of one coach on one route.
CREATE TABLE trips (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    route_id       BIGINT        NOT NULL,
    coach_id       BIGINT        NOT NULL,
    departure_time DATETIME      NOT NULL,
    arrival_time   DATETIME      NOT NULL,
    base_price     DECIMAL(10,2) NOT NULL,             -- one seat, before surcharge, promo and tax
    status         ENUM('SCHEDULED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'SCHEDULED',
    PRIMARY KEY (id),
    KEY idx_trips_route_departure (route_id, departure_time),
    KEY idx_trips_coach_departure (coach_id, departure_time),
    CONSTRAINT fk_trips_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT fk_trips_coach FOREIGN KEY (coach_id) REFERENCES coaches (id),
    CONSTRAINT ck_trips_times CHECK (arrival_time > departure_time)
) ENGINE=InnoDB;

-- 8. Discount codes managed by admins.
CREATE TABLE promo_codes (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    code           VARCHAR(30)   NOT NULL,
    description    VARCHAR(255),
    discount_type  ENUM('PERCENT','FIXED') NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    valid_from     DATETIME,
    valid_to       DATETIME,
    usage_limit    INT,                                -- NULL = unlimited
    used_count     INT           NOT NULL DEFAULT 0,
    active         TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_promo_code (code)
) ENGINE=InnoDB;

-- 9. One purchase. user_id is NULL for guest bookings; contact_* are always filled
--    so "find my booking" (code + email) works for guests and members alike.
CREATE TABLE bookings (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    booking_code  VARCHAR(20)   NOT NULL,              -- e.g. HC-48213
    user_id       BIGINT,
    trip_id       BIGINT        NOT NULL,
    promo_code_id BIGINT,
    contact_name  VARCHAR(100)  NOT NULL,
    contact_email VARCHAR(150)  NOT NULL,
    contact_phone VARCHAR(30),
    subtotal      DECIMAL(10,2) NOT NULL,              -- sum of seat prices
    discount      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax           DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total         DECIMAL(10,2) NOT NULL,
    status        ENUM('PENDING','CONFIRMED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_bookings_code (booking_code),
    KEY idx_bookings_email (contact_email),
    KEY idx_bookings_user (user_id),
    KEY idx_bookings_trip (trip_id),
    CONSTRAINT fk_bookings_user  FOREIGN KEY (user_id)       REFERENCES users (id),
    CONSTRAINT fk_bookings_trip  FOREIGN KEY (trip_id)       REFERENCES trips (id),
    CONSTRAINT fk_bookings_promo FOREIGN KEY (promo_code_id) REFERENCES promo_codes (id)
) ENGINE=InnoDB;

-- 10. One row per seat sold. trip_id is repeated here so the database itself can stop a
--     seat being sold twice. `held` is 1 while the seat is taken and NULL once the booking
--     is cancelled; MySQL allows many NULLs in a unique key, so a freed seat can be resold.
CREATE TABLE booking_seats (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    booking_id     BIGINT        NOT NULL,
    trip_id        BIGINT        NOT NULL,
    seat_label     VARCHAR(5)    NOT NULL,             -- e.g. 3A
    passenger_name VARCHAR(100),
    price          DECIMAL(10,2) NOT NULL,             -- base price + legroom surcharge at sale time
    held           TINYINT(1)    NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_trip_seat_held (trip_id, seat_label, held),
    KEY idx_booking_seats_booking (booking_id),
    CONSTRAINT fk_booking_seats_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_seats_trip    FOREIGN KEY (trip_id)    REFERENCES trips (id)
) ENGINE=InnoDB;

-- 11. Payment attempts for a booking. No card numbers are stored, only the provider's reference.
CREATE TABLE payments (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    booking_id      BIGINT        NOT NULL,
    method          ENUM('CARD','APPLE','GOOGLE','PAYPAL') NOT NULL,
    amount          DECIMAL(10,2) NOT NULL,
    status          ENUM('PENDING','PAID','FAILED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    transaction_ref VARCHAR(100),
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at         DATETIME,
    PRIMARY KEY (id),
    KEY idx_payments_booking (booking_id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id)
) ENGINE=InnoDB;
