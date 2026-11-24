/* =========================================================================
 * 15. SCREEN 7: MY TRIPS
 * ========================================================================= */
function renderMyTripsScreen() {
  const tabs = [
    { id: 'upcoming', lbl: 'Upcoming' },
    { id: 'completed', lbl: 'Completed' },
    { id: 'cancelled', lbl: 'Cancelled' }
  ];
  const badgeMap = {
    upcoming: { lbl: 'Upcoming', cls: 'badge-green' },
    completed: { lbl: 'Completed', cls: 'badge-gray' },
    cancelled: { lbl: 'Cancelled', cls: 'badge-red' }
  };
  const items = state.myTrips.filter(t => t.status === state.tripsTab);
  const qrTrip = state.qrModalTripId ? state.myTrips.find(t => t.id === state.qrModalTripId) : null;

  return `
    <div style="background:var(--navy);color:#fff;padding:34px 0 46px;">
      <div class="container-md">
        <button type="button" class="flex items-center gap-2 text-sm mb-3" style="color:rgba(255,255,255,0.78);" onclick="navigate('home')">${icon('arrowLeft', 15)} Back to home</button>
        <h1 style="font-size:1.6rem;font-weight:800;">My Trips</h1>
        <p class="text-sm mt-2" style="color:rgba(255,255,255,0.72);">All your H Coach Vietnam bookings & QR e-tickets in one place</p>
      </div>
    </div>

    <div class="container-md" style="margin-top:-24px;padding-bottom:60px;display:flex;flex-direction:column;gap:18px;">
      <div class="card" style="padding:6px;display:flex;gap:4px;">
        ${tabs.map(t => `
          <button type="button" class="deck-tab ${state.tripsTab === t.id ? 'active' : ''}" style="flex:1;padding:10px;${state.tripsTab === t.id ? 'background:var(--navy);color:#fff;' : ''}" onclick="state.tripsTab='${t.id}'; render();">${t.lbl}</button>
        `).join('')}
      </div>

      ${items.length === 0 ? `
        <div class="card" style="text-align:center;padding:52px 20px;">
          <div class="text-muted mb-3">${icon('ticket', 38)}</div>
          <h3 class="font-bold mb-2">No ${state.tripsTab} trips</h3>
          <p class="text-sm text-muted mb-4">Ready to explore Vietnam on your next intercity journey?</p>
          <button type="button" class="btn btn-primary" onclick="navigate('home')">Browse routes</button>
        </div>
      ` : items.map(t => `
        <div class="card p-5">
          <div class="flex items-center justify-between gap-3 mb-3" style="align-items:flex-start;">
            <div>
              <div class="flex items-center gap-2 font-extrabold" style="font-size:1.05rem;flex-wrap:wrap;">
                <span>${escapeHtml(t.from)}</span>
                <span class="text-orange">${icon('arrowRight', 14)}</span>
                <span>${escapeHtml(t.to)}</span>
              </div>
              <div class="text-xs text-muted mt-2">${t.date} · Booking ID: <strong class="text-navy" style="font-family:monospace;">${t.id}</strong></div>
            </div>
            <span class="badge ${badgeMap[t.status].cls}">${badgeMap[t.status].lbl}</span>
          </div>

          <div class="grid-3 mb-4" style="grid-template-columns:repeat(3,1fr);gap:12px;">
            <div><div class="text-xs text-muted">Operator</div><div class="font-bold text-sm">${escapeHtml(t.operator)}</div></div>
            <div><div class="text-xs text-muted">Seats</div><div class="font-bold text-sm">${t.seats.join(', ')}</div></div>
            <div><div class="text-xs text-muted">Total paid</div><div class="font-extrabold text-sm text-orange">${formatVND(t.price)}</div></div>
          </div>

          <div class="flex items-center justify-between gap-3" style="padding-top:12px;border-top:1px solid var(--border);flex-wrap:wrap;">
            <span class="text-xs text-muted flex items-center gap-2">${icon('clock', 12)} ${t.departure} → ${t.arrival} (${t.busType})</span>
            <div class="flex gap-3">
              <button type="button" class="text-xs font-bold text-navy flex items-center gap-2" style="gap:4px;" onclick="state.qrModalTripId='${t.id}'; render();">
                ${icon('qr', 13)} View E-Ticket
              </button>
              ${t.status === 'upcoming' ? `
                <button type="button" class="text-xs font-bold text-danger" onclick="state.cancelModalTripId='${t.id}'; render();">Cancel</button>
              ` : ''}
            </div>
          </div>
        </div>
      `).join('')}
    </div>

    ${qrTrip ? `
      <div class="modal-backdrop" onclick="state.qrModalTripId=null; render();">
        <div class="modal-box" style="text-align:center;" onclick="event.stopPropagation()">
          <div class="flex items-center justify-between mb-3">
            <strong class="text-navy" style="font-family:monospace;">${qrTrip.id}</strong>
            <button type="button" onclick="state.qrModalTripId=null; render();">${icon('x', 18)}</button>
          </div>
          <div class="font-extrabold mb-2">${escapeHtml(qrTrip.from)} → ${escapeHtml(qrTrip.to)}</div>
          <div class="text-xs text-muted mb-4">${qrTrip.date} · ${qrTrip.departure} · Seats ${qrTrip.seats.join(', ')}</div>
          <div style="display:inline-block;padding:12px;border:1px solid var(--border);border-radius:14px;background:var(--bg);margin-bottom:14px;">
            ${renderQrSvg(qrTrip.id, 140)}
          </div>
          <div class="text-xs text-muted mb-4">${escapeHtml(qrTrip.operator)} · ${escapeHtml(qrTrip.pickupStation || '')}</div>
          <button type="button" class="btn btn-primary btn-full" onclick="showToast('Saved e-ticket ${qrTrip.id}!', 'success')">${icon('download', 15)} Download E-Ticket</button>
        </div>
      </div>
    ` : ''}

    ${state.cancelModalTripId ? `
      <div class="modal-backdrop" onclick="state.cancelModalTripId=null; render();">
        <div class="modal-box" onclick="event.stopPropagation()">
          <h3 class="font-extrabold mb-2" style="font-size:1.1rem;">Cancel booking?</h3>
          <p class="text-sm text-muted mb-4">You'll receive a full refund since your trip is more than 24h away. This action cannot be undone.</p>
          <div class="flex gap-3">
            <button type="button" class="btn btn-outline" style="flex:1;" onclick="state.cancelModalTripId=null; render();">Keep booking</button>
            <button type="button" class="btn btn-danger" style="flex:1;" onclick="confirmCancelTrip()">Yes, cancel</button>
          </div>
        </div>
      </div>
    ` : ''}
  `;
}
