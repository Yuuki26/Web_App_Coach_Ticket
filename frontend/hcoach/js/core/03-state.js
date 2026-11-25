/* =========================================================================
 * 3. APPLICATION STATE
 * ========================================================================= */
const state = {
  screen: 'home', // home | search | seats | passenger | payment | confirmation | trips | routes | citybus
  mobileMenuOpen: false,
  authModalOpen: false,
  authTab: 'login',
  authUser: null,
  authForm: { name: '', email: '', password: '' },

  query: {
    from: 'Ho Chi Minh City',
    to: 'Lam Dong (Da Lat)',
    date: getDefaultDate(),
    returnDate: '',
    passengers: 1
  },
  openCityDropdown: null, // 'from' | 'to' | null
  citySearchText: '',

  // Search results (GET /api/v1/trips/search, see executeSearch)
  trips: [],
  tripsLoading: false,
  tripsError: null,     // message when the search call failed
  tripsNotice: null,    // 'not-served' when H Coach has no route between the two cities
  searchSeq: 0,         // ignores answers to searches that were replaced by a newer one
  sortBy: 'cheapest', // cheapest | earliest | rated
  filterBusTypes: [],
  filterMaxPrice: 800000,
  filterAmenities: [],
  mobileFilterOpen: false,
  expandedTripIds: new Set(),

  // Seat selection
  selectedTrip: null,
  seatMap: null,        // GET /api/v1/trips/{id}/seats for real trips
  seatMapLoading: false,
  seatMapError: null,
  activeDeck: 'lower',
  selectedSeats: [],

  // Passenger details
  passenger: {
    name: '',
    email: '',
    phone: '+84 ',
    promo: ''
  },
  appliedPromo: null,
  quote: null,          // POST /api/v1/bookings/quote for the chosen seats + promo

  // Payment
  paymentMethod: 'card', // card | ewallet | vietqr | digital
  cardForm: { name: '', number: '', expiry: '', cvv: '' },
  agreedTerms: false,
  isProcessingPayment: false,

  // Confirmation
  lastBooking: null,

  // Routes screen (real data from the backend, see screens/15b-routes.js)
  routes: {
    list: null,          // GET /api/v1/routes
    loading: false,
    error: null,
    filter: 'all',       // all | from | to (Ho Chi Minh City)
    q: '',
    selectedId: null,
    traffic: {},         // routeId -> { data, fetchedAt }
    trafficLoading: {},  // routeId -> true while loading
    trafficError: {},    // routeId -> message
    stationBuses: {}     // HCMC station name -> { lines } of city buses stopping nearby
  },

  // City buses screen (screens/15c-city-buses.js)
  cityBus: {
    list: null,          // GET /api/v1/city-buses
    results: null,       // search results, null when not searching
    loading: false,
    error: null,
    q: '',
    selectedId: null,
    dir: 0,              // which direction of the selected line is shown
    detail: {},          // lineId -> GET /api/v1/city-buses/{id}
    detailLoading: {},
    detailError: {}
  },

  // My Trips
  tripsTab: 'upcoming', // upcoming | completed | cancelled
  qrModalTripId: null,
  cancelModalTripId: null,
  // Real bookings, reloaded from the backend (see openMyTrips in 16-actions.js)
  myTrips: [],
  myTripsLoading: false,
  findForm: { code: '', email: '' }
};
