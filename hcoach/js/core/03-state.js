/* =========================================================================
 * 3. APPLICATION STATE & MULTI-PAGE PERSISTENCE (localStorage)
 * ========================================================================= */
const PAGE_FILES = {
  home: 'index.html',
  search: 'search.html',
  seats: 'seats.html',
  passenger: 'passenger.html',
  payment: 'payment.html',
  confirmation: 'confirmation.html',
  trips: 'trips.html'
};

const STORAGE_KEY = 'hcoach_app_state_v1';
const PENDING_TOAST_KEY = 'hcoach_pending_toast_v1';

function detectCurrentPage() {
  if (document.body && document.body.dataset && document.body.dataset.page) {
    return document.body.dataset.page;
  }
  const file = (window.location.pathname.split('/').pop() || 'index.html').toLowerCase();
  const entry = Object.entries(PAGE_FILES).find(([, f]) => f.toLowerCase() === file);
  return entry ? entry[0] : 'home';
}

const DEFAULT_MY_TRIPS = [
  {
    id: 'HC-VN-82934',
    status: 'upcoming',
    operator: 'Phuong Trang Express',
    from: 'Ho Chi Minh City',
    to: 'Lam Dong (Da Lat)',
    pickupStation: 'New Mien Dong Bus Station',
    dropStation: 'Da Lat Intercity Bus Station',
    date: formatDateEN(getDefaultDate()),
    departure: '06:30',
    arrival: '13:00',
    duration: '6h 30m',
    seats: ['12A', '12B'],
    price: 560000,
    busType: 'Sleeper',
    passengerName: 'Minh Anh Nguyen',
    email: 'minhanh@hcoach.vn'
  },
  {
    id: 'HC-VN-71823',
    status: 'completed',
    operator: 'Hoang Long Coach',
    from: 'Hanoi',
    to: 'Hai Phong',
    pickupStation: 'My Dinh Bus Station',
    dropStation: 'Vinh Niem Bus Station',
    date: 'Mon, Oct 12, 2026',
    departure: '08:00',
    arrival: '09:45',
    duration: '1h 45m',
    seats: ['4C'],
    price: 120000,
    busType: 'Seater',
    passengerName: 'Minh Anh Nguyen',
    email: 'minhanh@hcoach.vn'
  },
  {
    id: 'HC-VN-60912',
    status: 'cancelled',
    operator: 'Thanh Buoi Lines',
    from: 'Da Nang',
    to: 'Thua Thien Hue',
    pickupStation: 'Da Nang Central Bus Station',
    dropStation: 'South Hue Bus Station',
    date: 'Wed, Sep 23, 2026',
    departure: '14:15',
    arrival: '16:30',
    duration: '2h 15m',
    seats: ['5A'],
    price: 110000,
    busType: 'Sleeper',
    passengerName: 'Minh Anh Nguyen',
    email: 'minhanh@hcoach.vn'
  }
];

const state = {
  screen: detectCurrentPage(), // home | search | seats | passenger | payment | confirmation | trips
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

  // Search results
  trips: [],
  sortBy: 'cheapest', // cheapest | earliest | rated
  filterBusTypes: [],
  filterMaxPrice: 800000,
  filterAmenities: [],
  mobileFilterOpen: false,
  expandedTripIds: new Set(),

  // Seat selection
  selectedTrip: null,
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

  // Payment
  paymentMethod: 'card', // card | ewallet | vietqr | digital
  cardForm: { name: '', number: '', expiry: '', cvv: '' },
  agreedTerms: false,
  isProcessingPayment: false,

  // Confirmation
  lastBooking: null,

  // My Trips
  tripsTab: 'upcoming', // upcoming | completed | cancelled
  qrModalTripId: null,
  cancelModalTripId: null,
  myTrips: DEFAULT_MY_TRIPS.map(t => ({ ...t, seats: [...t.seats] }))
};

function saveState() {
  try {
    const payload = {
      authUser: state.authUser,
      query: state.query,
      trips: state.trips,
      sortBy: state.sortBy,
      filterBusTypes: state.filterBusTypes,
      filterMaxPrice: state.filterMaxPrice,
      filterAmenities: state.filterAmenities,
      selectedTrip: state.selectedTrip,
      activeDeck: state.activeDeck,
      selectedSeats: state.selectedSeats,
      passenger: state.passenger,
      appliedPromo: state.appliedPromo,
      paymentMethod: state.paymentMethod,
      lastBooking: state.lastBooking,
      tripsTab: state.tripsTab,
      myTrips: state.myTrips
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
  } catch (e) {
    // Ignore storage quota / privacy mode errors
  }
}

function loadState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return;
    const saved = JSON.parse(raw);
    if (!saved || typeof saved !== 'object') return;

    if (saved.authUser !== undefined) state.authUser = saved.authUser;
    if (saved.query && typeof saved.query === 'object') {
      state.query = { ...state.query, ...saved.query };
      if (!state.query.date) state.query.date = getDefaultDate();
      state.query.passengers = Math.max(1, Math.min(9, Number(state.query.passengers) || 1));
    }
    if (Array.isArray(saved.trips)) state.trips = saved.trips;
    if (saved.sortBy) state.sortBy = saved.sortBy;
    if (Array.isArray(saved.filterBusTypes)) state.filterBusTypes = saved.filterBusTypes;
    if (typeof saved.filterMaxPrice === 'number') state.filterMaxPrice = saved.filterMaxPrice;
    if (Array.isArray(saved.filterAmenities)) state.filterAmenities = saved.filterAmenities;
    if (saved.selectedTrip !== undefined) state.selectedTrip = saved.selectedTrip;
    if (saved.activeDeck) state.activeDeck = saved.activeDeck;
    if (Array.isArray(saved.selectedSeats)) state.selectedSeats = saved.selectedSeats;
    if (saved.passenger && typeof saved.passenger === 'object') {
      state.passenger = { ...state.passenger, ...saved.passenger };
    }
    if (saved.appliedPromo !== undefined) state.appliedPromo = saved.appliedPromo;
    if (saved.paymentMethod) state.paymentMethod = saved.paymentMethod;
    if (saved.lastBooking !== undefined) state.lastBooking = saved.lastBooking;
    if (saved.tripsTab) state.tripsTab = saved.tripsTab;
    if (Array.isArray(saved.myTrips) && saved.myTrips.length) state.myTrips = saved.myTrips;
  } catch (e) {
    // Ignore parse / storage errors
  }
}

function queueToast(message, type = 'info') {
  try {
    sessionStorage.setItem(PENDING_TOAST_KEY, JSON.stringify({ message, type }));
  } catch (e) {}
}

function flushPendingToast() {
  try {
    const raw = sessionStorage.getItem(PENDING_TOAST_KEY);
    if (!raw) return;
    sessionStorage.removeItem(PENDING_TOAST_KEY);
    const data = JSON.parse(raw);
    if (data && data.message && typeof showToast === 'function') {
      showToast(data.message, data.type || 'info');
    }
  } catch (e) {}
}

// Hydrate persisted state immediately on page load
loadState();
