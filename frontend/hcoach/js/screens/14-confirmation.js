/* =========================================================================
 * 14. SCREEN 6: BOOKING CONFIRMATION
 * ========================================================================= */
function renderConfirmationScreen() {
  const b = state.lastBooking;
  if (!b) return '';

  return `
    <div class="container" style="padding:40px 16px 64px;display:flex;justify-content:center;">
      <div style="width:100%;max-width:520px;">
        <div style="text-align:center;margin-bottom:28px;">
          <div style="width:74px;height:74px;border-radius:50%;background:var(--success);color:#fff;display:flex;align-items:center;justify-content:center;margin:0 auto 14px;box-shadow:0 10px 24px rgba(5,150,105,0.28);animation:popIn 0.3s ease;">
            ${icon('check', 34)}
          </div>
          <h1 style="font-size:1.55rem;font-weight:800;">Booking confirmed!</h1>
          <p class="text-sm text-muted mt-2">Keep your booking code. With <strong>${escapeHtml(b.email)}</strong> it finds this booking in My Trips.</p>
        </div>

        <div class="card" style="overflow:hidden;margin-bottom:18px;">
          <div style="background:var(--navy);color:#fff;padding:22px 24px;">
            <div class="flex items-center justify-between mb-4">
              <div class="flex items-center gap-2">
                <span class="brand-badge" style="width:28px;height:28px;font-size:0.8rem;">H</span>
                <strong>Coach Vietnam</strong>
              </div>
              <span class="badge badge-green">Confirmed</span>
            </div>

            <div class="flex items-center justify-between gap-2">
              <div style="min-width:0;">
                <div class="text-xs" style="color:rgba(255,255,255,0.65);">From</div>
                <div class="font-extrabold truncate">${escapeHtml(b.from)}</div>
                <div class="text-xs truncate" style="color:rgba(255,255,255,0.75);">${escapeHtml(b.pickupStation)}</div>
                <div class="font-bold text-orange mt-2">${b.departure}</div>
              </div>
              <div style="text-align:center;flex-shrink:0;">
                <div class="text-xs" style="color:rgba(255,255,255,0.7);">${b.duration}</div>
                <div style="color:rgba(255,255,255,0.7);margin-top:4px;">── ${icon('arrowRight', 13)}</div>
              </div>
              <div style="text-align:right;min-width:0;">
                <div class="text-xs" style="color:rgba(255,255,255,0.65);">To</div>
                <div class="font-extrabold truncate">${escapeHtml(b.to)}</div>
                <div class="text-xs truncate" style="color:rgba(255,255,255,0.75);">${escapeHtml(b.dropStation)}</div>
                <div class="font-bold text-orange mt-2">${b.arrival}</div>
              </div>
            </div>
          </div>

          <div class="ticket-tear"><div class="ticket-tear-line"></div></div>

          <div class="p-6">
            <div class="grid-2 mb-4" style="gap:14px;">
              <div><div class="text-xs text-muted">Passenger</div><div class="font-bold text-sm">${escapeHtml(b.passengerName)}</div></div>
              <div><div class="text-xs text-muted">Booking ID</div><div class="font-extrabold text-sm text-navy" style="font-family:monospace;">${b.id}</div></div>
              <div><div class="text-xs text-muted">Departure Date</div><div class="font-bold text-sm">${b.date}</div></div>
              <div><div class="text-xs text-muted">Seat${b.seats.length > 1 ? 's' : ''}</div><div class="font-bold text-sm">${b.seats.join(', ')}</div></div>
              <div><div class="text-xs text-muted">Operator</div><div class="font-bold text-sm">${escapeHtml(b.operator)}</div></div>
              <div><div class="text-xs text-muted">Total Paid</div><div class="font-extrabold text-sm text-orange">${formatVND(b.price)}</div></div>
            </div>

            <div style="border-top:1px dashed var(--border);padding-top:18px;text-align:center;">
              <div style="display:inline-block;padding:10px;border:1px solid var(--border);border-radius:14px;background:var(--bg);">
                ${renderQrSvg(b.id, 112)}
              </div>
              <div class="text-xs text-muted mt-2">Show this QR code at boarding</div>
            </div>
          </div>
        </div>

        <div class="grid-2 mb-3">
          <button type="button" class="btn btn-outline" onclick="showToast('E-ticket ${b.id} downloaded!', 'success')">${icon('download', 15)} Download</button>
          <button type="button" class="btn btn-outline" onclick="copyText('H Coach Ticket ${b.id}: ${b.from} -> ${b.to} (${b.date})'); showToast('Ticket details copied to clipboard!', 'success');">${icon('share', 15)} Share</button>
        </div>
        <div class="grid-2">
          <button type="button" class="btn btn-outline" onclick="navigate('home')">Back to Home</button>
          <button type="button" class="btn btn-primary" onclick="openMyTrips()">View My Trips ${icon('arrowRight', 15)}</button>
        </div>
      </div>
    </div>
  `;
}
