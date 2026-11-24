/* =========================================================================
 * 6. DYNAMIC VIETNAM TRIP GENERATOR & SEAT MAP GENERATOR
 * ========================================================================= */
function generateTrips(from, to) {
  const pickup = getStationName(from);
  const drop = getStationName(to);
  const matched = POPULAR_ROUTES.find(r => (r.from === from && r.to === to) || (r.from === to && r.to === from));

  let basePrice = 240000;
  let durMin = 300;
  if (matched) {
    basePrice = matched.fromPrice;
    const m = matched.duration.match(/(\d+)h\s*(\d+)m/);
    if (m) durMin = parseInt(m[1], 10) * 60 + parseInt(m[2], 10);
  } else {
    const seed = (from.length * 13 + to.length * 29) % 6;
    const presets = [
      { p: 150000, d: 150 }, { p: 210000, d: 260 }, { p: 280000, d: 380 },
      { p: 340000, d: 460 }, { p: 185000, d: 210 }, { p: 410000, d: 540 }
    ];
    basePrice = presets[seed].p;
    durMin = presets[seed].d;
  }

  const fmtDur = (m) => `${Math.floor(m / 60)}h ${String(m % 60).padStart(2, '0')}m`;
  const addMins = (hhmm, mins) => {
    const [h, m] = hhmm.split(':').map(Number);
    const tot = (h * 60 + m + mins) % (24 * 60);
    return `${String(Math.floor(tot / 60)).padStart(2, '0')}:${String(tot % 60).padStart(2, '0')}`;
  };
  const round10k = (v) => Math.max(100000, Math.min(780000, Math.round(v / 10000) * 10000));

  return [
    {
      id: 'vn-1', operator: 'Phuong Trang Express', initials: 'PT', color: '#FF7A1A',
      rating: 4.8, reviews: 3420, departureTime: '06:30', arrivalTime: addMins('06:30', durMin),
      duration: fmtDur(durMin), busType: 'Sleeper', pickupStation: pickup, dropStation: drop,
      seatsLeft: 11, amenities: ['wifi', 'power'], price: round10k(basePrice), hasUpperDeck: true
    },
    {
      id: 'vn-2', operator: 'Sao Viet Limousine', initials: 'SV', color: '#0F4C81',
      rating: 4.9, reviews: 2185, departureTime: '08:00', arrivalTime: addMins('08:00', Math.max(90, durMin - 20)),
      duration: fmtDur(Math.max(90, durMin - 20)), busType: 'VIP Limousine', pickupStation: pickup, dropStation: drop,
      seatsLeft: 4, amenities: ['wifi', 'power', 'restroom', 'legroom', 'coffee'], price: round10k(basePrice * 1.45), hasUpperDeck: true
    },
    {
      id: 'vn-3', operator: 'Hoang Long Coach', initials: 'HL', color: '#059669',
      rating: 4.6, reviews: 1640, departureTime: '11:30', arrivalTime: addMins('11:30', durMin + 15),
      duration: fmtDur(durMin + 15), busType: 'Seater', pickupStation: pickup, dropStation: drop,
      seatsLeft: 16, amenities: ['wifi'], price: round10k(basePrice * 0.8), hasUpperDeck: false
    },
    {
      id: 'vn-4', operator: 'Thanh Buoi Lines', initials: 'TB', color: '#6366F1',
      rating: 4.7, reviews: 2890, departureTime: '14:15', arrivalTime: addMins('14:15', durMin),
      duration: fmtDur(durMin), busType: 'Sleeper', pickupStation: pickup, dropStation: drop,
      seatsLeft: 3, amenities: ['wifi', 'power', 'restroom'], price: round10k(basePrice * 1.1), hasUpperDeck: true
    },
    {
      id: 'vn-5', operator: 'Hai Van Express', initials: 'HV', color: '#D97706',
      rating: 4.8, reviews: 1290, departureTime: '21:00', arrivalTime: addMins('21:00', Math.max(90, durMin - 10)),
      duration: fmtDur(Math.max(90, durMin - 10)), busType: 'VIP Limousine', pickupStation: pickup, dropStation: drop,
      seatsLeft: 7, amenities: ['wifi', 'power', 'restroom', 'legroom', 'coffee'], price: round10k(basePrice * 1.35), hasUpperDeck: true
    }
  ];
}

function generateSeats(deck) {
  const bookedLower = new Set(['1A', '1B', '3C', '4B', '5D', '6A', '7B', '8C', '9A', '9D', '10C', '11B']);
  const bookedUpper = new Set(['2A', '2B', '4C', '5D', '6B', '7A', '8C', '10A']);
  const legLower = new Set([1, 6]);
  const legUpper = new Set([1, 5]);
  const booked = deck === 'lower' ? bookedLower : bookedUpper;
  const leg = deck === 'lower' ? legLower : legUpper;
  const res = [];
  for (let r = 1; r <= 11; r++) {
    for (const c of ['A', 'B', 'C', 'D']) {
      const id = `${r}${c}`;
      res.push({ id, row: r, col: c, status: booked.has(id) ? 'booked' : leg.has(r) ? 'legroom' : 'available' });
    }
  }
  return res;
}
