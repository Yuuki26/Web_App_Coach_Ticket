/* =========================================================================
 * 16. ACTIONS & EVENT HANDLERS (MULTI-PAGE NAVIGATION)
 * ========================================================================= */
function navigate(screen, hash = '') {
  state.openCityDropdown = null;
  state.mobileMenuOpen = false;
  saveState();
  const target = PAGE_FILES[screen] || 'index.html';
  window.location.href = hash ? `${target}#${hash}` : target;
}

function scrollToEl(id, e) {
  if (state.screen === 'home') {
    if (e) e.preventDefault();
    state.mobileMenuOpen = false;
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  } else {
    if (e) e.preventDefault();
    navigate('home', id);
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
  saveState();
  showToast(state.authTab === 'signup' ? 'Account created! Welcome to H Coach Vietnam.' : 'Welcome back!', 'success');
  render();
}

function logoutUser() {
  state.authUser = null;
  saveState();
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
  saveState();
  render();
}

function swapCities() {
  const tmp = state.query.from;
  state.query.from = state.query.to;
  state.query.to = tmp;
  saveState();
  render();
}

function changePassengers(delta) {
  state.query.passengers = Math.max(1, Math.min(9, state.query.passengers + delta));
  saveState();
  render();
}

function executeSearch(e) {
  if (e) e.preventDefault();
  if (!state.query.from || !state.query.to) {
    showToast('Please select both origin and destination.', 'error');
    return;
  }
  if (!state.query.date) {
    showToast('Please select a departure date.', 'error');
    return;
  }
  state.trips = generateTrips(state.query.from, state.query.to);
  state.filterBusTypes = [];
  state.filterAmenities = [];
  state.filterMaxPrice = 800000;
  state.expandedTripIds.clear();
  state.openCityDropdown = null;
  state.mobileMenuOpen = false;

  saveState();
  queueToast(`Searching ${state.query.from} → ${state.query.to}…`, 'success');

  const params = new URLSearchParams({
    from: state.query.from,
    to: state.query.to,
    date: state.query.date,
    passengers: String(state.query.passengers)
  });
  window.location.href = `search.html?${params.toString()}`;
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
  saveState();
  render();
}

function toggleAmenity(k) {
  state.filterAmenities = state.filterAmenities.includes(k)
    ? state.filterAmenities.filter(x => x !== k)
    : [...state.filterAmenities, k];
  saveState();
  render();
}

function resetFilters() {
  state.filterBusTypes = [];
  state.filterAmenities = [];
  state.filterMaxPrice = 800000;
  saveState();
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
  saveState();
  render();
}

function applyPromo() {
  const code = (state.passenger.promo || '').trim().toUpperCase();
  if (PROMO_CODES[code]) {
    state.appliedPromo = code;
    saveState();
    showToast(`Promo ${code} applied — ${PROMO_CODES[code].label}!`, 'success');
    render();
  } else {
    showToast('Invalid promo code. Try FIRSTRIDE, WEEKEND15, or DALAT50K.', 'error');
  }
}

function removePromo() {
  state.appliedPromo = null;
  state.passenger.promo = '';
  saveState();
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

    saveState();
    queueToast(`Payment successful! Booking ID ${id}`, 'success');
    navigate('confirmation');
  }, 1200);
}

function confirmCancelTrip() {
  const id = state.cancelModalTripId;
  state.myTrips = state.myTrips.map(t => t.id === id ? { ...t, status: 'cancelled' } : t);
  state.cancelModalTripId = null;
  saveState();
  showToast(`Booking ${id} cancelled. Refund in 3–5 business days.`, 'success');
  render();
}
