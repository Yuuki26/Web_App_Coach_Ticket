/* =========================================================================
 * 12. SCREEN 4: PASSENGER DETAILS
 * ========================================================================= */
function isPassengerValid() {
  const p = state.passenger;
  return p.name.trim().length >= 2 && p.email.includes('@') && p.phone.replace(/\s+/g, '').length >= 9;
}

function syncPassengerBtn() {
  saveState();
  const ok = isPassengerValid();
  document.querySelectorAll('.js-pax-next').forEach(b => { b.disabled = !ok; });
}

function renderPassengerScreen() {
  const trip = state.selectedTrip;
  if (!trip) return '';
  const subtotal = trip.price * state.selectedSeats.length;
  const valid = isPassengerValid();

  return `
    <div style="background:#fff;border-bottom:1px solid var(--border);padding:14px 0;">
      <div class="container-md flex items-center gap-4">
        <button type="button" style="padding:8px;color:var(--muted);" onclick="navigate('seats')">${icon('arrowLeft', 18)}</button>
        ${renderStepper(1)}
      </div>
    </div>

    <div class="container-md" style="padding-top:28px;padding-bottom:64px;display:flex;flex-direction:column;gap:20px;">
      <div class="card p-6">
        <h2 class="font-extrabold mb-4" style="font-size:1.15rem;">Passenger details</h2>
        <div class="grid-2">
          <div class="col-span-2">
            <label class="field-label">Full name</label>
            <div class="input-wrap">
              <span class="input-icon">${icon('user', 15)}</span>
              <input class="input input-with-icon" type="text" placeholder="e.g. Minh Anh Nguyen" value="${escapeHtml(state.passenger.name)}" oninput="state.passenger.name=this.value; syncPassengerBtn();"/>
            </div>
          </div>
          <div>
            <label class="field-label">Email address</label>
            <div class="input-wrap">
              <span class="input-icon">${icon('mail', 15)}</span>
              <input class="input input-with-icon" type="email" placeholder="minhanh@example.com" value="${escapeHtml(state.passenger.email)}" oninput="state.passenger.email=this.value; syncPassengerBtn();"/>
            </div>
          </div>
          <div>
            <label class="field-label">Phone number (+84)</label>
            <div class="input-wrap">
              <span class="input-icon">${icon('phone', 15)}</span>
              <input class="input input-with-icon" type="tel" placeholder="+84 912 345 678" value="${escapeHtml(state.passenger.phone)}" oninput="state.passenger.phone=this.value; syncPassengerBtn();"/>
            </div>
          </div>
        </div>
      </div>

      <div class="card p-6">
        <h3 class="font-bold mb-3">Trip summary</h3>
        <div class="flex items-center gap-3" style="background:var(--bg);padding:14px;border-radius:12px;">
          <div style="width:40px;height:40px;border-radius:10px;background:${trip.color};color:#fff;font-size:0.75rem;font-weight:900;display:flex;align-items:center;justify-content:center;">${trip.initials}</div>
          <div style="flex:1;min-width:0;">
            <div class="font-bold text-sm truncate">${escapeHtml(trip.operator)} (${escapeHtml(state.query.from)} → ${escapeHtml(state.query.to)})</div>
            <div class="text-xs text-muted">${formatDateEN(state.query.date)} · ${trip.departureTime} → ${trip.arrivalTime} · Seats ${state.selectedSeats.join(', ')}</div>
          </div>
          <strong class="text-orange">${formatVND(subtotal)}</strong>
        </div>
      </div>

      <div class="card p-6">
        <h3 class="font-bold mb-3">Promo code</h3>
        <div class="flex gap-2">
          <div class="input-wrap" style="flex:1;">
            <span class="input-icon">${icon('tag', 14)}</span>
            <input class="input input-with-icon" style="text-transform:uppercase;" type="text" ${state.appliedPromo ? 'disabled' : ''} placeholder="Enter code (FIRSTRIDE, WEEKEND15, DALAT50K)" value="${escapeHtml(state.passenger.promo)}" oninput="state.passenger.promo=this.value.toUpperCase(); saveState();"/>
          </div>
          ${state.appliedPromo ? `
            <button type="button" class="btn" style="background:var(--success-soft);color:var(--success);" onclick="removePromo()">${icon('checkCircle', 14)} Remove</button>
          ` : `
            <button type="button" class="btn btn-outline" onclick="applyPromo()">Apply</button>
          `}
        </div>
        ${state.appliedPromo ? `<div class="text-xs font-bold text-success mt-2">✓ ${PROMO_CODES[state.appliedPromo].label}</div>` : ''}
      </div>

      <div class="flex gap-3">
        <button type="button" class="btn btn-outline btn-lg" style="flex:1;" onclick="navigate('seats')">Back</button>
        <button type="button" class="btn btn-primary btn-lg js-pax-next" style="flex:2;" ${!valid ? 'disabled' : ''} onclick="navigate('payment')">
          Continue to payment ${icon('arrowRight', 16)}
        </button>
      </div>
    </div>
  `;
}
