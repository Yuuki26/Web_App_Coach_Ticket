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

function applyPromo() {
  const code = (state.passenger.promo || '').trim().toUpperCase();
  if (PROMO_CODES[code]) {
    state.appliedPromo = code;
    showToast(`Promo ${code} applied — ${PROMO_CODES[code].label}!`, 'success');
    render();
  } else {
    showToast('Invalid promo code. Try FIRSTRIDE, WEEKEND15, or DALAT50K.', 'error');
  }
}

function removePromo() {
  state.appliedPromo = null;
  state.passenger.promo = '';
  showToast('Promo code removed.', 'info');
  render();
}

function processPayment() {
  if (!state.agreedTerms) {
    showToast('Please agree to the Terms of Service.', 'error');
    return;
  }
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

function confirmCancelTrip() {
  const id = state.cancelModalTripId;
  state.myTrips = state.myTrips.map(t => t.id === id ? { ...t, status: 'cancelled' } : t);
  state.cancelModalTripId = null;
  showToast(`Booking ${id} cancelled. Refund in 3–5 business days.`, 'success');
  render();
}
