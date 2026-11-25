/* =========================================================================
 * 3b. BACKEND API (Spring Boot, bus-ticket-booking)
 * -------------------------------------------------------------------------
 * The backend runs on http://localhost:8081. Open this frontend through a
 * local web server (e.g. VS Code "Live Server" on port 5500) rather than
 * double-clicking index.html: the backend only accepts calls from the
 * addresses listed in app.cors.allowed-origins.
 * To use another backend address, set window.HCOACH_API_BASE before this file.
 * ========================================================================= */
const API_BASE = window.HCOACH_API_BASE
  || (location.port === '8081' ? '/api/v1' : 'http://localhost:8081/api/v1');

/**
 * Calls the backend and returns the JSON answer.
 * Throws an Error whose message can be shown to users. err.code is the
 * backend's error code (e.g. SEAT_TAKEN) and err.errors the per-field
 * problems of a VALIDATION_FAILED answer ([{field, message}]).
 */
async function apiRequest(path, body) {
  let res;
  try {
    res = await fetch(API_BASE + path, body === undefined
      ? { headers: { Accept: 'application/json' } }
      : { method: 'POST', headers: { Accept: 'application/json', 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
  } catch (e) {
    throw new Error('Cannot reach the H Coach server. Make sure the backend is running on localhost:8081.');
  }
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const fieldProblem = data && data.errors && data.errors[0];
    const message = fieldProblem ? `${fieldProblem.field}: ${fieldProblem.message}` : data && data.detail;
    const err = new Error(message || `Request failed (${res.status})`);
    err.code = data && data.code;
    err.status = res.status;
    err.errors = (data && data.errors) || [];
    throw err;
  }
  return data;
}

/** GET, e.g. apiGet('/routes') */
const apiGet = path => apiRequest(path);
/** POST with a JSON body, e.g. apiPost('/bookings/quote', {...}) */
const apiPost = (path, body) => apiRequest(path, body);

/* ---------- City names ----------
 * The backend uses Vietnamese city names ("Đà Lạt"); this site's search box
 * uses English province names ("Lam Dong (Da Lat)"). */
const CITY_TO_PROVINCE = {
  'TP. Hồ Chí Minh': 'Ho Chi Minh City',
  'Đà Lạt': 'Lam Dong (Da Lat)',
  'Vũng Tàu': 'Ba Ria - Vung Tau',
  'Cần Thơ': 'Can Tho',
  'Nha Trang': 'Khanh Hoa (Nha Trang)',
  'Phan Thiết': 'Binh Thuan (Mui Ne)',
  'Châu Đốc': 'An Giang',
  'Rạch Giá': 'Kien Giang (Phu Quoc)',
  'Buôn Ma Thuột': 'Dak Lak (Buon Ma Thuot)',
  'Tây Ninh': 'Tay Ninh',
  'Bến Tre': 'Ben Tre',
  'Cà Mau': 'Ca Mau',
  'Quy Nhơn': 'Binh Dinh (Quy Nhon)'
};
const PROVINCE_TO_CITY = Object.fromEntries(Object.entries(CITY_TO_PROVINCE).map(([c, p]) => [p, c]));

/** The backend's name for a city picked in the search box, or null if H Coach doesn't serve it. */
function toBackendCity(name) {
  if (PROVINCE_TO_CITY[name]) return PROVINCE_TO_CITY[name];
  return CITY_TO_PROVINCE[name] ? name : null;
}

/** Every route (GET /api/v1/routes), loaded once and shared with the Routes screen. */
async function ensureRouteList() {
  if (!state.routes.list) state.routes.list = await apiGet('/routes');
  return state.routes.list;
}

/* ---------- Trips ----------
 * Turns a trip from GET /api/v1/trips/search into the shape the search and
 * seat screens draw. Amenities aren't stored in the backend yet, so they
 * follow the coach type. */
const BUS_TYPE_BY_COACH = { STANDARD: 'Seater', PREMIUM: 'VIP Limousine', SLEEPER: 'Sleeper' };
const AMENITIES_BY_COACH = {
  STANDARD: ['wifi', 'power'],
  PREMIUM: ['wifi', 'power', 'restroom', 'legroom', 'coffee'],
  SLEEPER: ['wifi', 'power', 'restroom']
};

function toUiTrip(t) {
  const hhmm = iso => iso.slice(11, 16);
  const nextDay = t.arrivalTime.slice(0, 10) !== t.departureTime.slice(0, 10);
  const initials = t.operatorName.split(/\s+/).filter(Boolean).slice(0, 2).map(w => w[0]).join('').toUpperCase();
  return {
    id: String(t.id),          // the screens compare ids as strings
    apiId: t.id,               // marks a real trip: seats come from GET /trips/{id}/seats
    routeId: t.routeId,
    operator: t.operatorName,
    initials,
    color: t.operatorColor,
    rating: t.rating,
    coachPlate: t.coachPlate,
    departureAt: t.departureTime,
    departureTime: hhmm(t.departureTime),
    arrivalTime: hhmm(t.arrivalTime) + (nextDay ? ' +1' : ''),
    duration: `${Math.floor(t.durationMinutes / 60)}h ${String(t.durationMinutes % 60).padStart(2, '0')}m`,
    busType: BUS_TYPE_BY_COACH[t.coachType] || t.coachType,
    pickupStation: t.originStation,
    dropStation: t.destinationStation,
    seatsLeft: t.seatsLeft,
    totalSeats: t.totalSeats,
    amenities: AMENITIES_BY_COACH[t.coachType] || ['wifi'],
    price: Number(t.price),
    hasUpperDeck: false        // the backend models every coach as one deck
  };
}

/* ---------- Bookings ---------- */

/** Payment options on the payment screen → the backend's PaymentMethod. */
const PAYMENT_METHOD_API = { card: 'CARD', ewallet: 'EWALLET', vietqr: 'VIETQR', digital: 'DIGITAL_WALLET' };

/** Turns a booking from the backend into the shape the confirmation and My Trips screens draw. */
function toUiBooking(b) {
  const hhmm = iso => iso.slice(11, 16);
  const minutes = Math.round((new Date(b.arrivalTime) - new Date(b.departureTime)) / 60000);
  return {
    id: b.bookingCode,
    real: true,
    status: b.status.toLowerCase(),          // upcoming | completed | cancelled
    operator: b.operatorName,
    from: b.origin,
    to: b.destination,
    pickupStation: b.originStation,
    dropStation: b.destinationStation,
    date: formatDateEN(b.departureTime.slice(0, 10)),
    departureAt: b.departureTime,
    departure: hhmm(b.departureTime),
    arrival: hhmm(b.arrivalTime),
    duration: `${Math.floor(minutes / 60)}h ${String(minutes % 60).padStart(2, '0')}m`,
    seats: b.seats,
    price: Number(b.total),
    fare: Number(b.fare),
    discount: Number(b.discount),
    tax: Number(b.tax),
    busType: BUS_TYPE_BY_COACH[b.coachType] || b.coachType,
    passengerName: b.passengerName,
    email: b.email
  };
}

/**
 * Booking codes + emails booked or looked up in this browser, so My Trips can
 * reload them without an account. The server still checks code and email
 * together on every request.
 */
const bookingRefs = {
  KEY: 'hcoach.bookings',
  all() {
    try { return JSON.parse(localStorage.getItem(this.KEY)) || []; } catch (e) { return []; }
  },
  add(code, email) {
    const list = this.all().filter(r => r.code !== code);
    list.unshift({ code, email });
    try { localStorage.setItem(this.KEY, JSON.stringify(list.slice(0, 30))); } catch (e) { /* private mode */ }
  },
  remove(code) {
    try { localStorage.setItem(this.KEY, JSON.stringify(this.all().filter(r => r.code !== code))); } catch (e) { /* ignore */ }
  }
};

/**
 * Fare for the chosen seats. Real trips add the backend's legroom surcharge
 * (rows 1–2, from the seat map); the made-up trips never had one.
 */
function seatsSubtotal(trip, seats) {
  if (!trip) return 0;
  let total = trip.price * seats.length;
  const map = state.seatMap;
  if (trip.apiId && map && map.tripId === trip.apiId) {
    const legroom = seats.filter(s => parseInt(s, 10) <= map.legroomRows).length;
    total += legroom * Number(map.legroomSurcharge);
  }
  return total;
}
