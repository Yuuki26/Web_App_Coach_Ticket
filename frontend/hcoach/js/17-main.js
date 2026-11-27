/* =========================================================================
 * 17. MAIN RENDER & INITIALIZATION
 * ========================================================================= */
function render() {
  const root = document.getElementById('app');
  if (!root) return;

  let content = '';
  switch (state.screen) {
    case 'home': content = renderHomeScreen(); break;
    case 'search': content = renderSearchScreen(); break;
    case 'seats': content = renderSeatScreen(); break;
    case 'passenger': content = renderPassengerScreen(); break;
    case 'payment': content = renderPaymentScreen(); break;
    case 'confirmation': content = renderConfirmationScreen(); break;
    case 'trips': content = renderMyTripsScreen(); break;
    case 'routes': content = renderRoutesScreen(); break;
    case 'citybus': content = renderCityBusScreen(); break;
    default: content = renderHomeScreen();
  }

  root.innerHTML = `
    ${renderHeader()}
    <main>${content}</main>
    ${renderAuthModal()}
  `;

  // The Routes map is drawn by Leaflet after its container is on the page.
  if (state.screen === 'routes') mountRouteMap();
  else if (routeMap) { routeMap.remove(); routeMap = null; }
  if (state.screen === 'citybus') mountCityBusMap();
  else if (cityBusMap) { cityBusMap.remove(); cityBusMap = null; }
}

document.addEventListener('click', (e) => {
  if (state.openCityDropdown && !e.target.closest('.city-picker')) {
    state.openCityDropdown = null;
    render();
  }
});

document.addEventListener('DOMContentLoaded', () => {
  render();
  loadDestinationPhotos();
  startDealCountdown();
});
