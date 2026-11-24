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
    default: content = renderHomeScreen();
  }

  root.innerHTML = `
    ${renderHeader()}
    <main>${content}</main>
    ${renderAuthModal()}
  `;
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
