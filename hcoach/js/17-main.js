/* =========================================================================
 * 17. MAIN RENDER & MULTI-PAGE INITIALIZATION
 * ========================================================================= */
function render() {
  const root = document.getElementById('app');
  if (!root) return;

  let content = '';
  switch (state.screen) {
    case 'home':
      content = typeof renderHomeScreen === 'function' ? renderHomeScreen() : '';
      break;
    case 'search':
      content = typeof renderSearchScreen === 'function' ? renderSearchScreen() : '';
      break;
    case 'seats':
      content = typeof renderSeatScreen === 'function' ? renderSeatScreen() : '';
      break;
    case 'passenger':
      content = typeof renderPassengerScreen === 'function' ? renderPassengerScreen() : '';
      break;
    case 'payment':
      content = typeof renderPaymentScreen === 'function' ? renderPaymentScreen() : '';
      break;
    case 'confirmation':
      content = typeof renderConfirmationScreen === 'function' ? renderConfirmationScreen() : '';
      break;
    case 'trips':
      content = typeof renderMyTripsScreen === 'function' ? renderMyTripsScreen() : '';
      break;
    default:
      content = typeof renderHomeScreen === 'function' ? renderHomeScreen() : '';
  }

  root.innerHTML = `
    ${renderHeader()}
    <main>${content}</main>
    ${renderAuthModal()}
  `;

  saveState();
}

function initCurrentPage() {
  state.screen = detectCurrentPage();

  if (state.screen === 'search') {
    const params = new URLSearchParams(window.location.search);
    let routeChanged = false;
    const fromParam = params.get('from');
    const toParam = params.get('to');
    const dateParam = params.get('date');
    const paxParam = params.get('passengers');

    if (fromParam && fromParam !== state.query.from) {
      state.query.from = fromParam;
      routeChanged = true;
    }
    if (toParam && toParam !== state.query.to) {
      state.query.to = toParam;
      routeChanged = true;
    }
    if (dateParam) state.query.date = dateParam;
    if (paxParam) state.query.passengers = Math.max(1, Math.min(9, Number(paxParam) || 1));

    if (!state.trips.length || routeChanged) {
      state.trips = generateTrips(state.query.from, state.query.to);
    }
    saveState();
  }

  if (state.screen === 'seats' && !state.selectedTrip) {
    queueToast('Please choose a trip first.', 'info');
    window.location.replace('search.html');
    return false;
  }

  if (state.screen === 'passenger') {
    if (!state.selectedTrip) {
      queueToast('Please choose a trip first.', 'info');
      window.location.replace('search.html');
      return false;
    }
    if (!state.selectedSeats.length) {
      queueToast('Please select your seats first.', 'info');
      window.location.replace('seats.html');
      return false;
    }
  }

  if (state.screen === 'payment') {
    if (!state.selectedTrip) {
      queueToast('Please choose a trip first.', 'info');
      window.location.replace('search.html');
      return false;
    }
    if (!state.selectedSeats.length) {
      queueToast('Please select your seats first.', 'info');
      window.location.replace('seats.html');
      return false;
    }
  }

  if (state.screen === 'confirmation' && !state.lastBooking) {
    if (state.myTrips && state.myTrips.length) {
      state.lastBooking = state.myTrips[0];
    } else {
      window.location.replace('trips.html');
      return false;
    }
  }

  return true;
}

document.addEventListener('click', (e) => {
  if (state.openCityDropdown && !e.target.closest('.city-picker')) {
    state.openCityDropdown = null;
    render();
  }
});

document.addEventListener('DOMContentLoaded', () => {
  if (!initCurrentPage()) return;
  render();
  flushPendingToast();

  if (state.screen === 'home') {
    loadDestinationPhotos();
    startDealCountdown();
    if (window.location.hash) {
      const targetId = window.location.hash.slice(1);
      setTimeout(() => {
        const el = document.getElementById(targetId);
        if (el) el.scrollIntoView({ behavior: 'smooth' });
      }, 80);
    }
  }
});
