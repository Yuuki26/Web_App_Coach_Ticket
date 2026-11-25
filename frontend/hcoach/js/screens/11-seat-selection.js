/* =========================================================================
 * 11. SCREEN 3: SEAT SELECTION
 * ========================================================================= */
function renderSeatScreen() {
  const trip = state.selectedTrip;
  if (!trip) return '';
  const pax = state.query.passengers;
  const map = state.seatMap;
  const real = !!trip.apiId;
  if (real && !map) return renderSeatMapStatus(trip);
  const rowCount = real ? map.rows : 11;
  const seats = real ? realSeats(map) : generateSeats(state.activeDeck);
  const total = seatsSubtotal(trip, state.selectedSeats);

  const seatBtn = (s) => {
    const code = `${state.activeDeck === 'upper' ? 'U' : ''}${s.id}`;
    const sel = state.selectedSeats.includes(code);
    const cls = s.status === 'booked' ? 'booked' : sel ? 'selected' : s.status === 'legroom' ? 'legroom' : '';
    return `<button type="button" class="seat-btn ${cls}" ${s.status === 'booked' ? 'disabled' : `onclick="toggleSeat('${code}')"`} title="Seat ${code}">${s.col}</button>`;
  };

  const rowsHtml = Array.from({ length: rowCount }, (_, i) => i + 1).map(row => {
    const rs = seats.filter(s => s.row === row);
    return `
      <div class="flex items-center gap-2 mb-2">
        <span class="text-xs text-muted font-bold" style="width:24px;text-align:right;">${row}</span>
        <div class="flex gap-2">${rs.filter(s => s.col === 'A' || s.col === 'B').map(seatBtn).join('')}</div>
        <span class="text-muted" style="width:28px;text-align:center;opacity:0.35;">│</span>
        <div class="flex gap-2">${rs.filter(s => s.col === 'C' || s.col === 'D').map(seatBtn).join('')}</div>
      </div>
    `;
  }).join('');

  return `
    <div style="background:#fff;border-bottom:1px solid var(--border);padding:14px 0;">
      <div class="container flex items-center gap-4">
        <button type="button" style="padding:8px;color:var(--muted);" onclick="navigate('search')">${icon('arrowLeft', 18)}</button>
        ${renderStepper(0)}
      </div>
    </div>

    <div class="container seat-layout">
      <div class="card p-6" style="flex:1;min-width:0;">
        <div class="flex items-center justify-between gap-3 mb-4" style="flex-wrap:wrap;">
          <div>
            <h2 class="font-extrabold" style="font-size:1.15rem;">Choose your seat${pax > 1 ? 's' : ''}</h2>
            <p class="text-xs text-muted mt-2">Select ${pax} seat${pax > 1 ? 's' : ''} for your journey</p>
          </div>
          ${trip.hasUpperDeck ? `
            <div class="deck-tabs">
              <button type="button" class="deck-tab ${state.activeDeck === 'lower' ? 'active' : ''}" onclick="state.activeDeck='lower'; render();">Lower deck</button>
              <button type="button" class="deck-tab ${state.activeDeck === 'upper' ? 'active' : ''}" onclick="state.activeDeck='upper'; render();">Upper deck</button>
            </div>
          ` : ''}
        </div>

        <div class="flex gap-4 mb-4 text-xs text-muted" style="flex-wrap:wrap;padding-bottom:14px;border-bottom:1px solid var(--border);">
          <span class="flex items-center gap-2"><span style="width:18px;height:18px;border-radius:5px;border:1px solid var(--border);background:#fff;display:inline-block;"></span> Available</span>
          <span class="flex items-center gap-2"><span style="width:18px;height:18px;border-radius:5px;background:var(--navy);display:inline-block;"></span> Selected</span>
          <span class="flex items-center gap-2"><span style="width:18px;height:18px;border-radius:5px;background:#F1F5F9;border:1px solid var(--border);display:inline-block;"></span> Booked</span>
          <span class="flex items-center gap-2"><span style="width:18px;height:18px;border-radius:5px;border:2px solid var(--success);background:#fff;display:inline-block;"></span> Extra legroom${real ? ` (+${formatVND(map.legroomSurcharge)})` : ''}</span>
        </div>

        <div style="overflow-x:auto;display:flex;justify-content:center;">
          <div style="min-width:max-content;padding:6px 0;">
            <div class="flex items-center justify-between text-xs font-bold text-muted mb-3" style="background:var(--bg);padding:8px 14px;border-radius:10px;">
              <span>Front (Driver)</span>
              <span>${trip.hasUpperDeck ? (state.activeDeck === 'lower' ? 'Lower deck' : 'Upper deck') : 'Main cabin'}</span>
            </div>
            <div class="flex items-center gap-2 mb-2" style="margin-left:32px;">
              <div class="flex gap-2">
                <span class="text-xs font-bold text-muted" style="width:40px;text-align:center;">A</span>
                <span class="text-xs font-bold text-muted" style="width:40px;text-align:center;">B</span>
              </div>
              <span class="text-xs text-muted" style="width:28px;text-align:center;">Aisle</span>
              <div class="flex gap-2">
                <span class="text-xs font-bold text-muted" style="width:40px;text-align:center;">C</span>
                <span class="text-xs font-bold text-muted" style="width:40px;text-align:center;">D</span>
              </div>
            </div>
            ${rowsHtml}
          </div>
        </div>
      </div>

      <div class="seat-summary-col">
        <div class="card p-6">
          <h3 class="font-bold mb-4">Booking summary</h3>
          <div class="flex items-center gap-3 mb-4" style="padding-bottom:14px;border-bottom:1px solid var(--border);">
            <div style="width:38px;height:38px;border-radius:10px;background:${trip.color};color:#fff;font-size:0.75rem;font-weight:900;display:flex;align-items:center;justify-content:center;">${trip.initials}</div>
            <div>
              <div class="font-bold text-sm">${escapeHtml(trip.operator)}</div>
              <div class="text-xs text-muted">${trip.departureTime} → ${trip.arrivalTime} · ${trip.busType}</div>
            </div>
          </div>

          <div class="flex flex-col gap-3 text-sm mb-4">
            <div class="flex gap-2"><span class="text-navy">${icon('mapPin', 14)}</span><div><div class="text-xs text-muted">Pick up</div><div class="font-bold">${escapeHtml(trip.pickupStation)}</div></div></div>
            <div class="flex gap-2"><span class="text-orange">${icon('mapPin', 14)}</span><div><div class="text-xs text-muted">Drop off</div><div class="font-bold">${escapeHtml(trip.dropStation)}</div></div></div>
          </div>

          <div style="background:var(--bg);border-radius:12px;padding:12px 14px;" class="mb-4">
            <div class="text-xs text-muted">Selected seats (${state.selectedSeats.length}/${pax})</div>
            <div class="font-bold text-navy mt-2">${state.selectedSeats.length ? state.selectedSeats.join(', ') : 'None selected yet'}</div>
          </div>

          <div class="flex items-center justify-between mb-4">
            <span class="text-sm text-muted">Subtotal</span>
            <span style="font-size:1.4rem;font-weight:800;color:var(--orange);">${state.selectedSeats.length ? formatVND(total) : '—'}</span>
          </div>

          <button type="button" class="btn btn-primary btn-lg btn-full" ${state.selectedSeats.length !== pax ? 'disabled' : ''} onclick="goToPassenger()">
            Continue ${icon('arrowRight', 16)}
          </button>
        </div>
      </div>
    </div>

    <div class="mobile-sticky-bar">
      <div>
        <div class="text-xs text-muted">${state.selectedSeats.length}/${pax} seat${pax > 1 ? 's' : ''}</div>
        <div class="font-extrabold text-orange" style="font-size:1.1rem;">${state.selectedSeats.length ? formatVND(total) : '—'}</div>
      </div>
      <button type="button" class="btn btn-primary" style="flex:1;" ${state.selectedSeats.length !== pax ? 'disabled' : ''} onclick="goToPassenger()">
        Continue ${icon('arrowRight', 15)}
      </button>
    </div>
  `;
}

/** Seats of a real coach: every row × A–D, sold seats booked, rows 1–2 with extra legroom. */
function realSeats(map) {
  const booked = new Set(map.bookedSeats);
  const res = [];
  for (let r = 1; r <= map.rows; r++) {
    for (const c of map.columns) {
      const id = `${r}${c}`;
      res.push({ id, row: r, col: c, status: booked.has(id) ? 'booked' : r <= map.legroomRows ? 'legroom' : 'available' });
    }
  }
  return res;
}

/** While the seat plan loads, or if it couldn't be loaded. */
function renderSeatMapStatus(trip) {
  return `
    <div style="background:#fff;border-bottom:1px solid var(--border);padding:14px 0;">
      <div class="container flex items-center gap-4">
        <button type="button" style="padding:8px;color:var(--muted);" onclick="navigate('search')">${icon('arrowLeft', 18)}</button>
        ${renderStepper(0)}
      </div>
    </div>
    <div class="container" style="padding:32px 16px 48px;">
      <div class="card p-6" style="text-align:center;padding:56px 20px;">
        ${state.seatMapError ? `
          <div class="text-muted mb-3">${icon('alertCircle', 40)}</div>
          <h3 class="font-bold mb-2">Couldn't load the seat plan</h3>
          <p class="text-sm text-muted mb-4">${escapeHtml(state.seatMapError)}</p>
          <button type="button" class="btn btn-navy" onclick="loadSeatMap(${trip.apiId})">Try again</button>
        ` : `
          <div class="text-muted mb-3">${icon('clock', 40)}</div>
          <h3 class="font-bold">Loading seats…</h3>
        `}
      </div>
    </div>
  `;
}
