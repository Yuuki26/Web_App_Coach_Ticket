/* =========================================================================
 * 2. POPULAR ROUTES, OPERATORS, REVIEWS & PROMOS (100% VIETNAM DATA)
 * ========================================================================= */
const POPULAR_ROUTES = [
  {
    slug: 'dalat',
    from: 'Ho Chi Minh City',
    to: 'Lam Dong (Da Lat)',
    duration: '6h 30m',
    fromPrice: 280000,
    tag: 'Most Popular',
    enTitle: 'Da_Lat',
    viTitle: 'Đà_Lạt',
    altTitle: 'Xuan_Huong_Lake'
  },
  {
    slug: 'sapa',
    from: 'Hanoi',
    to: 'Lao Cai (Sa Pa)',
    duration: '5h 30m',
    fromPrice: 320000,
    tag: 'Hot Deal',
    enTitle: 'Sa_Pa',
    viTitle: 'Sa_Pa',
    altTitle: 'Fansipan'
  },
  {
    slug: 'nhatrang',
    from: 'Ho Chi Minh City',
    to: 'Khanh Hoa (Nha Trang)',
    duration: '7h 45m',
    fromPrice: 260000,
    tag: null,
    enTitle: 'Nha_Trang',
    viTitle: 'Nha_Trang',
    altTitle: 'Nha_Trang_Bay'
  },
  {
    slug: 'haiphong',
    from: 'Hanoi',
    to: 'Hai Phong',
    duration: '1h 45m',
    fromPrice: 120000,
    tag: null,
    enTitle: 'Haiphong',
    viTitle: 'Hải_Phòng',
    altTitle: 'Hai_Phong'
  },
  {
    slug: 'hue',
    from: 'Da Nang',
    to: 'Thua Thien Hue',
    duration: '2h 15m',
    fromPrice: 110000,
    tag: null,
    enTitle: 'Imperial_City_of_Huế',
    viTitle: 'Kinh_thành_Huế',
    altTitle: 'Meridian_Gate'
  },
  {
    slug: 'cantho',
    from: 'Ho Chi Minh City',
    to: 'Can Tho',
    duration: '3h 30m',
    fromPrice: 165000,
    tag: null,
    enTitle: 'Can_Tho',
    viTitle: 'Cần_Thơ',
    altTitle: 'Can_Tho_Bridge'
  }
];

// In-memory photo cache & backup URL queues so returning to Home renders instantly
const routeImageCache = {};
const routeImageLoaded = {};
const routeFallbackTried = {};

const PROMO_CODES = {
  FIRSTRIDE: { type: 'percent', value: 20, label: '20% off your first booking' },
  WEEKEND15: { type: 'percent', value: 15, label: '15% off weekend Limousine travel' },
  DALAT50K:  { type: 'fixed', value: 50000, label: '50,000₫ off Ho Chi Minh City → Da Lat' }
};

const DEAL_CARDS = [
  {
    id: 'deal-firstride',
    code: 'FIRSTRIDE',
    title: '20% off your first booking',
    desc: 'New to H Coach Vietnam? Save instantly on any intercity route.',
    bg: '#FF7A1A',
    claimedPct: 84,
    endText: 'Only 19 codes left',
    durationMs: ((2 * 60 + 48) * 60 + 19) * 1000,
    expiresAt: Date.now() + (((2 * 60 + 48) * 60 + 19) * 1000)
  },
  {
    id: 'deal-weekend15',
    code: 'WEEKEND15',
    title: '15% off weekend Limousine travel',
    desc: 'Travel Fri–Sun on any VIP Limousine coach nationwide.',
    bg: '#0F4C81',
    claimedPct: 76,
    endText: 'Weekend flash sale',
    durationMs: ((5 * 60 + 19) * 60 + 42) * 1000,
    expiresAt: Date.now() + (((5 * 60 + 19) * 60 + 42) * 1000)
  },
  {
    id: 'deal-dalat50k',
    code: 'DALAT50K',
    title: '50,000₫ off Ho Chi Minh City → Da Lat',
    desc: 'Limited seats on our most-traveled highland corridor.',
    bg: '#059669',
    claimedPct: 91,
    endText: '12 seats left',
    durationMs: ((0 * 60 + 44) * 60 + 55) * 1000,
    expiresAt: Date.now() + (((0 * 60 + 44) * 60 + 55) * 1000)
  }
];

let dealTimerInterval = null;

function formatDealCountdown(deal) {
  let remaining = deal.expiresAt - Date.now();
  if (remaining <= 0) {
    deal.expiresAt = Date.now() + deal.durationMs;
    remaining = deal.durationMs;
  }
  const totalSec = Math.floor(remaining / 1000);
  const hrs = String(Math.floor(totalSec / 3600)).padStart(2, '0');
  const mins = String(Math.floor((totalSec % 3600) / 60)).padStart(2, '0');
  const secs = String(totalSec % 60).padStart(2, '0');
  return `${hrs}:${mins}:${secs}`;
}

function updateDealTimers() {
  const nodes = document.querySelectorAll('.deal-timer[data-deal-id]');
  if (!nodes.length) return;
  nodes.forEach((el) => {
    const deal = DEAL_CARDS.find(d => d.id === el.getAttribute('data-deal-id'));
    if (deal) {
      el.textContent = `Ends in ${formatDealCountdown(deal)}`;
    }
  });
}

function startDealCountdown() {
  if (dealTimerInterval) clearInterval(dealTimerInterval);
  dealTimerInterval = setInterval(updateDealTimers, 1000);
}

const TESTIMONIALS = [
  {
    name: 'Minh Anh Nguyen',
    location: 'Ho Chi Minh City',
    rating: 5,
    avatar: 'MN',
    bg: '#0F4C81',
    text: 'Booked a sleeper coach from Ho Chi Minh City to Da Lat in under a minute. Picked my lower-deck berth, paid via VietQR, and boarded smoothly with the QR e-ticket!'
  },
  {
    name: 'David Miller',
    location: 'Hanoi → Sa Pa',
    rating: 5,
    avatar: 'DM',
    bg: '#059669',
    text: 'Took the Sao Viet VIP Limousine from Hanoi to Sa Pa. Spacious reclining seats, fast Wi-Fi, USB charging at every seat, and a super smooth ride up the mountain.'
  },
  {
    name: 'Thu Ha Le',
    location: 'Da Nang',
    rating: 5,
    avatar: 'TL',
    bg: '#6366F1',
    text: 'Transparent VND pricing with zero hidden fees. My Da Nang to Hue trip departed right on schedule and the driver was courteous and professional.'
  },
  {
    name: 'Sarah Jenkins',
    location: 'Nha Trang',
    rating: 5,
    avatar: 'SJ',
    bg: '#FF7A1A',
    text: 'Loved how easy it was to compare Phuong Trang and Thanh Buoi coaches to Nha Trang. The interactive seat map made choosing window seats effortless.'
  },
  {
    name: 'Quoc Huy Pham',
    location: 'Can Tho',
    rating: 5,
    avatar: 'QP',
    bg: '#0D9488',
    text: 'I commute weekly between Ho Chi Minh City and Can Tho. Instant e-tickets and reliable departure times make H Coach my go-to booking platform.'
  },
  {
    name: 'Lucas Moreau',
    location: 'Ha Long Bay',
    rating: 4,
    avatar: 'LM',
    bg: '#D97706',
    text: 'Had to reschedule my Hanoi to Ha Long trip last minute—free cancellation over 24 hours before departure worked seamlessly. Great customer support!'
  }
];

const PARTNERS = [
  { name: 'Phuong Trang Express', initials: 'PT', color: '#FF7A1A' },
  { name: 'Sao Viet Limousine', initials: 'SV', color: '#0F4C81' },
  { name: 'Hoang Long Coach', initials: 'HL', color: '#059669' },
  { name: 'Thanh Buoi Lines', initials: 'TB', color: '#6366F1' },
  { name: 'Hai Van Express', initials: 'HV', color: '#D97706' }
];

const AMENITIES = {
  wifi:     { label: 'Wi-Fi', icon: 'wifi' },
  power:    { label: 'Power outlets', icon: 'zap' },
  restroom: { label: 'Restroom', icon: 'wind' },
  legroom:  { label: 'Extra legroom', icon: 'arrowRight' },
  coffee:   { label: 'Coffee', icon: 'coffee' }
};
