/* =========================================================================
 * 3. APPLICATION STATE
 * ========================================================================= */
const state = {
  screen: 'home', // home | search | seats | passenger | payment | confirmation | trips
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
  myTrips: [
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
  ]
};
