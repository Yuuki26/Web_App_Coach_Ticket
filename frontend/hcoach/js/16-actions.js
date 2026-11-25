/* =========================================================================
 * 16. ACTIONS & EVENT HANDLERS
 * ========================================================================= */
function navigate(screen) {
  state.screen = screen;
  state.openCityDropdown = null;
  state.mobileMenuOpen = false;
  window.scrollTo({ top: 0, behavior: 'smooth' });
  render();
}

function scrollToEl(id) {
  if (state.screen !== 'home') {
    state.screen = 'home';
    render();
    setTimeout(() => {
      const el = document.getElementById(id);
      if (el) el.scrollIntoView({ behavior: 'smooth' });
    }, 60);
  } else {
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  }
}

function openAuthModal() { state.authModalOpen = true; render(); }
function closeAuthModal() { state.authModalOpen = false; render(); }

function submitAuth(e) {
  e.preventDefault();
  const name = (state.authTab === 'signup' && state.authForm.name.trim())
    ? state.authForm.name.trim()
    : (state.authForm.email.split('@')[0] || 'Traveler');
  state.authUser = { name, email: state.authForm.email };
  state.authModalOpen = false;
  showToast(state.authTab === 'signup' ? 'Account created! Welcome to H Coach Vietnam.' : 'Welcome back!', 'success');
  render();
}

function logoutUser() {
  state.authUser = null;
  showToast('Signed out successfully.', 'info');
  render();
}

function toggleCityDropdown(field, e) {
  if (e) e.stopPropagation();
  state.openCityDropdown = state.openCityDropdown === field ? null : field;
  state.citySearchText = '';
  render();
  if (state.openCityDropdown === field) {
    setTimeout(() => {
      const inp = document.getElementById(`city-search-${field}`);
      if (inp) inp.focus();
    }, 20);
  }
}

function filterCityDropdown(field, val) {
  state.citySearchText = val;
  const el = document.getElementById(`city-list-${field}`);
  if (el) el.innerHTML = renderCityOptions(field, val);
}

function selectCity(field, city) {
  state.query[field] = city;
  state.openCityDropdown = null;
  state.citySearchText = '';
  render();
}

function swapCities() {
  const tmp = state.query.from;
  state.query.from = state.query.to;
  state.query.to = tmp;
  render();
}

function changePassengers(delta) {
  state.query.passengers = Math.max(1, Math.min(9, state.query.passengers + delta));
  render();
}

/** Searches real trips: GET /api/v1/trips/search?from=&to=&date= */
async function executeSearch() {
  if (!state.query.from || !state.query.to) {
    showToast('Please select both origin and destination.', 'error');
    return;
  }
  if (!state.query.date) {
    showToast('Please select a departure date.', 'error');
    return;
  }
  const seq = ++state.searchSeq;
  state.trips = [];
  state.tripsLoading = true;
  state.tripsError = null;
  state.tripsNotice = null;
  state.filterBusTypes = [];
  state.filterAmenities = [];
  state.filterMaxPrice = 800000;
  state.expandedTripIds.clear();
  navigate('search');

  const from = toBackendCity(state.query.from);
  const to = toBackendCity(state.query.to);
  try {
    const routes = await ensureRouteList();
    if (!from || !to || !routes.some(r => r.origin === from && r.destination === to)) {
      state.tripsNotice = 'not-served';
    } else {
      const q = `from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&date=${state.query.date}`;
      const trips = await apiGet(`/trips/search?${q}`);
      if (seq !== state.searchSeq) return; // a newer search has started
      state.trips = trips.map(toUiTrip);
    }
  } catch (e) {
    if (seq !== state.searchSeq) return;
    state.tripsError = e.message;
  }
  state.tripsLoading = false;
  if (state.screen === 'search') render();
}

/** Moves the search date by some days (e.g. "Try the next day") and searches again. */
function shiftSearchDate(days) {
  const [y, m, d] = state.query.date.split('-').map(Number);
  const next = new Date(y, m - 1, d + days);
  state.query.date = `${next.getFullYear()}-${String(next.getMonth() + 1).padStart(2, '0')}-${String(next.getDate()).padStart(2, '0')}`;
  executeSearch();
}

function searchPopularRoute(from, to) {
  state.query.from = from;
  state.query.to = to;
  if (!state.query.date) state.query.date = getDefaultDate();
  executeSearch();
}

function copyPromo(code) {
  copyText(code);
  showToast(`Copied promo code ${code}! Apply it at Passenger Details.`, 'success');
}

function toggleBusType(bt) {
  state.filterBusTypes = state.filterBusTypes.includes(bt)
    ? state.filterBusTypes.filter(x => x !== bt)
    : [...state.filterBusTypes, bt];
  render();
}

function toggleAmenity(k) {
  state.filterAmenities = state.filterAmenities.includes(k)
    ? state.filterAmenities.filter(x => x !== k)
    : [...state.filterAmenities, k];
  render();
}

function resetFilters() {
  state.filterBusTypes = [];
  state.filterAmenities = [];
  state.filterMaxPrice = 800000;
  render();
}

function toggleTripExpand(id) {
  if (state.expandedTripIds.has(id)) state.expandedTripIds.delete(id);
  else state.expandedTripIds.add(id);
  render();
}

function selectTrip(id) {
  const t = state.trips.find(x => x.id === id);
  if (!t) return;
  state.selectedTrip = t;
  state.activeDeck = 'lower';
  state.selectedSeats = [];
  navigate('seats');
  if (t.apiId) loadSeatMap(t.apiId);
}

/** Real seat plan and sold seats: GET /api/v1/trips/{id}/seats */
async function loadSeatMap(tripId) {
  state.seatMap = null;
  state.seatMapError = null;
  state.seatMapLoading = true;
  render();
  try {
    const map = await apiGet(`/trips/${tripId}/seats`);
    if (!state.selectedTrip || state.selectedTrip.apiId !== tripId) return;
    state.seatMap = map;
  } catch (e) {
    state.seatMapError = e.message;
  }
  state.seatMapLoading = false;
  if (state.screen === 'seats') render();
}

function toggleSeat(code) {
  const pax = state.query.passengers;
  if (state.selectedSeats.includes(code)) {
    state.selectedSeats = state.selectedSeats.filter(x => x !== code);
  } else if (state.selectedSeats.length < pax) {
    state.selectedSeats = [...state.selectedSeats, code];
  } else {
    showToast(`You can select up to ${pax} seat${pax > 1 ? 's' : ''}.`, 'info');
  }
  render();
}

/** Seat screen → passenger details, fetching the real price first. */
function goToPassenger() {
  navigate('passenger');
  refreshQuote();
}

/** Identifies what a quote was for, so an old quote is never shown for new seats. */
function quoteKey(promo) {
  const t = state.selectedTrip;
  return t ? `${t.apiId}|${[...state.selectedSeats].sort().join(',')}|${promo || ''}` : '';
}

/** The backend's price for the chosen seats and promo: POST /api/v1/bookings/quote */
async function fetchQuote(promo) {
  const t = state.selectedTrip;
  return apiPost('/bookings/quote', { tripId: t.apiId, seats: state.selectedSeats, promoCode: promo || null });
}

async function refreshQuote() {
  const t = state.selectedTrip;
  if (!t || !t.apiId || !state.selectedSeats.length) return;
  const key = quoteKey(state.appliedPromo);
  try {
    const q = await fetchQuote(state.appliedPromo);
    if (key !== quoteKey(state.appliedPromo)) return; // seats or code changed meanwhile
    state.quote = { ...q, key };
  } catch (e) {
    state.quote = null; // the screens fall back to their own estimate
  }
  if (state.screen === 'passenger' || state.screen === 'payment') render();
}

async function applyPromo() {
  const code = (state.passenger.promo || '').trim().toUpperCase();
  if (!code) return;
  const t = state.selectedTrip;
  if (!t || !t.apiId) {
    // Made-up trips (not from the backend) keep the original local codes.
    if (PROMO_CODES[code]) {
      state.appliedPromo = code;
      showToast(`Promo ${code} applied — ${PROMO_CODES[code].label}!`, 'success');
      render();
    } else {
      showToast('Invalid promo code.', 'error');
    }
    return;
  }
  try {
    const q = await fetchQuote(code);
    if (q.promoApplied) {
      state.appliedPromo = code;
      state.quote = { ...q, key: quoteKey(code) };
      showToast(`Promo ${code} applied: ${q.promoMessage}`, 'success');
      render();
    } else {
      showToast(q.promoMessage || 'This code can\'t be used.', 'error');
    }
  } catch (e) {
    showToast(e.message, 'error');
  }
}

function removePromo() {
  state.appliedPromo = null;
  state.passenger.promo = '';
  showToast('Promo code removed.', 'info');
  render();
  refreshQuote();
}

/**
 * Books the seats: POST /api/v1/bookings. No card details are sent; the
 * backend confirms the booking without charging (no real payment yet).
 */
async function processPayment() {
  if (!state.agreedTerms) {
    showToast('Please agree to the Terms of Service.', 'error');
    return;
  }
  const t = state.selectedTrip;
  if (!t) return navigate('home');
  if (!t.apiId) return processDemoPayment();

  state.isProcessingPayment = true;
  render();
  const phone = state.passenger.phone.trim();
  try {
    const b = await apiPost('/bookings', {
      tripId: t.apiId,
      passengerName: state.passenger.name.trim(),
      email: state.passenger.email.trim(),
      phone: phone.replace(/[\s+]/g, '').length > 2 ? phone : null,
      seats: state.selectedSeats,
      promoCode: state.appliedPromo,
      paymentMethod: PAYMENT_METHOD_API[state.paymentMethod] || 'CARD'
    });
    const booking = toUiBooking(b);
    bookingRefs.add(b.bookingCode, b.email);
    state.lastBooking = booking;
    state.myTrips = [booking, ...state.myTrips.filter(x => x.id !== booking.id)];
    state.isProcessingPayment = false;
    state.agreedTerms = false;
    state.tripsTab = 'upcoming';
    state.appliedPromo = null;
    state.passenger.promo = '';
    state.quote = null;
    showToast(`Booking confirmed! Your code is ${b.bookingCode}`, 'success');
    navigate('confirmation');
  } catch (e) {
    state.isProcessingPayment = false;
    if (e.code === 'SEAT_TAKEN') {
      // Someone else just booked one of these seats: show the fresh seat plan.
      showToast(e.message, 'error');
      state.selectedSeats = [];
      state.quote = null;
      navigate('seats');
      loadSeatMap(t.apiId);
    } else if (e.code === 'TRIP_DEPARTED' || e.code === 'TRIP_CANCELLED') {
      showToast(e.message, 'error');
      executeSearch();
    } else {
      showToast(e.message, 'error');
      render();
    }
  }
}

/** The original made-up payment, still used for trips that didn't come from the backend. */
function processDemoPayment() {
  state.isProcessingPayment = true;
  render();

  setTimeout(() => {
    const { total } = calcTotals();
    const id = 'HC-VN-' + Math.floor(10000 + Math.random() * 90000);
    const t = state.selectedTrip;
    const booking = {
      id,
      status: 'upcoming',
      operator: t.operator,
      from: state.query.from,
      to: state.query.to,
      pickupStation: t.pickupStation,
      dropStation: t.dropStation,
      date: formatDateEN(state.query.date),
      departure: t.departureTime,
      arrival: t.arrivalTime,
      duration: t.duration,
      seats: [...state.selectedSeats],
      price: total,
      busType: t.busType,
      passengerName: state.passenger.name.trim() || 'H Coach Traveler',
      email: state.passenger.email.trim() || 'traveler@hcoach.vn'
    };

    state.lastBooking = booking;
    state.myTrips = [booking, ...state.myTrips];
    state.isProcessingPayment = false;
    state.agreedTerms = false;
    state.tripsTab = 'upcoming';

    showToast(`Payment successful! Booking ID ${id}`, 'success');
    navigate('confirmation');
  }, 1200);
}

/** Cancels a booking: POST /api/v1/bookings/cancel with its code and email. */
async function confirmCancelTrip() {
  const id = state.cancelModalTripId;
  const trip = state.myTrips.find(t => t.id === id);
  state.cancelModalTripId = null;
  if (!trip) return render();
  if (!trip.real) {
    state.myTrips = state.myTrips.map(t => t.id === id ? { ...t, status: 'cancelled' } : t);
    showToast(`Booking ${id} cancelled.`, 'success');
    return render();
  }
  try {
    const b = await apiPost('/bookings/cancel', { bookingCode: trip.id, email: trip.email });
    state.myTrips = state.myTrips.map(t => t.id === id ? toUiBooking(b) : t);
    showToast(`Booking ${id} cancelled. Your seats are released.`, 'success');
  } catch (e) {
    showToast(e.message, 'error');
  }
  render();
}

/** My Trips: reloads every booking saved in this browser (POST /api/v1/bookings/lookup). */
async function openMyTrips() {
  navigate('trips');
  const refs = bookingRefs.all();
  if (!refs.length) return;
  state.myTripsLoading = true;
  render();
  const results = await Promise.all(refs.map(r =>
    apiPost('/bookings/lookup', { bookingCode: r.code, email: r.email })
      .then(toUiBooking)
      .catch(e => {
        if (e.status === 404) bookingRefs.remove(r.code); // no longer exists
        return null;
      })));
  state.myTrips = results.filter(Boolean);
  state.myTripsLoading = false;
  if (state.screen === 'trips') render();
}

/** "Find a booking" on My Trips: by booking code + email. */
async function findBooking(e) {
  e.preventDefault();
  const code = state.findForm.code.trim().toUpperCase();
  const email = state.findForm.email.trim();
  if (!code || !email) return showToast('Enter your booking code and email.', 'error');
  try {
    const booking = toUiBooking(await apiPost('/bookings/lookup', { bookingCode: code, email }));
    bookingRefs.add(booking.id, booking.email);
    state.myTrips = [booking, ...state.myTrips.filter(t => t.id !== booking.id)];
    state.tripsTab = booking.status;
    state.findForm = { code: '', email: '' };
    showToast(`Found booking ${booking.id}`, 'success');
    render();
  } catch (err) {
    showToast(err.message, 'error');
  }
}
