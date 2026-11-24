/* =========================================================================
 * 10. SCREEN 2: SEARCH RESULTS (60FPS MAX PRICE SLIDER)
 * ========================================================================= */
let priceSliderRafId = null;
let lastRenderedTripKey = '';

function getFilteredTrips() {
  return state.trips
    .filter(t => state.filterBusTypes.length === 0 || state.filterBusTypes.includes(t.busType))
    .filter(t => t.price <= state.filterMaxPrice)
    .filter(t => state.filterAmenities.length === 0 || state.filterAmenities.every(a => t.amenities.includes(a)))
    .sort((a, b) => {
      if (state.sortBy === 'cheapest') return a.price - b.price;
      if (state.sortBy === 'rated') return b.rating - a.rating;
      return a.departureTime.localeCompare(b.departureTime);
    });
}

function getPricePercent(val) {
  return Math.max(0, Math.min(100, ((val - 100000) / (800000 - 100000)) * 100));
}

function renderFiltersHtml() {
  const busTypes = ['Seater', 'Sleeper', 'VIP Limousine'];
  const pct = getPricePercent(state.filterMaxPrice);
  const hasActiveFilter = state.filterBusTypes.length > 0 || state.filterAmenities.length > 0 || state.filterMaxPrice < 800000;

  return `
    <div class="card p-5 flex flex-col gap-4">
      <div>
        <h3 class="font-bold text-sm mb-3">Bus type</h3>
        <div class="flex flex-col gap-2">
          ${busTypes.map(bt => {
            const chk = state.filterBusTypes.includes(bt);
            return `
              <div class="check-row" onclick="toggleBusType('${bt}')">
                <span class="check-box ${chk ? 'checked' : ''}">${chk ? icon('check', 11) : ''}</span>
                <span>${bt}</span>
              </div>
            `;
          }).join('')}
        </div>
      </div>

      <div style="border-top:1px solid var(--border);padding-top:16px;">
        <div class="flex items-center justify-between mb-2">
          <h3 class="font-bold text-sm">Max price</h3>
          <strong class="text-sm text-orange js-max-price-value">${formatVND(state.filterMaxPrice)}</strong>
        </div>
        <input
          type="range"
          class="price-slider"
          min="100000"
          max="800000"
          step="5000"
          value="${state.filterMaxPrice}"
          style="--val:${pct}%"
          aria-label="Max price"
          oninput="onPriceSliderInput(this)"
        />
        <div class="flex justify-between text-xs text-muted mt-2">
          <span>100,000₫</span><span>800,000₫</span>
        </div>
      </div>

      <div style="border-top:1px solid var(--border);padding-top:16px;">
        <h3 class="font-bold text-sm mb-3">Amenities</h3>
        <div class="flex flex-col gap-2">
          ${Object.keys(AMENITIES).map(k => {
            const chk = state.filterAmenities.includes(k);
            return `
              <div class="check-row" onclick="toggleAmenity('${k}')">
                <span class="check-box ${chk ? 'checked' : ''}">${chk ? icon('check', 11) : ''}</span>
                <span class="flex items-center gap-2"><span class="text-muted">${icon(AMENITIES[k].icon, 13)}</span> ${AMENITIES[k].label}</span>
              </div>
            `;
          }).join('')}
        </div>
      </div>

      <button type="button" class="btn btn-outline btn-sm btn-full js-reset-filter-btn" style="display:${hasActiveFilter ? 'inline-flex' : 'none'};" onclick="resetFilters()">
        Reset filters
      </button>
    </div>
  `;
}

function renderTripListHtml(list) {
  const badgeCls = { 'Seater': 'badge-gray', 'Sleeper': 'badge-navy', 'VIP Limousine': 'badge-orange' };

  if (list.length === 0) {
    return `
      <div class="card p-6" style="text-align:center;padding:56px 20px;">
        <div class="text-muted mb-3">${icon('alertCircle', 40)}</div>
        <h3 class="font-bold mb-2">No trips found</h3>
        <p class="text-sm text-muted mb-4">Try adjusting your max price slider or bus type filters.</p>
        <button type="button" class="btn btn-navy" onclick="resetFilters()">Clear filters</button>
      </div>
    `;
  }

  return list.map(t => {
    const exp = state.expandedTripIds.has(t.id);
    return `
      <div class="card">
        <div class="p-5">
          <div class="trip-card-top">
            <div class="flex items-center gap-3" style="flex:1.1;min-width:0;">
              <div style="width:44px;height:44px;border-radius:12px;background:${t.color};color:#fff;font-size:0.8rem;font-weight:900;display:flex;align-items:center;justify-content:center;flex-shrink:0;">${t.initials}</div>
              <div style="min-width:0;">
                <div class="font-bold text-sm truncate">${escapeHtml(t.operator)}</div>
                <div class="flex items-center gap-2 mt-2" style="flex-wrap:wrap;">
                  ${renderStars(t.rating, t.reviews)}
                  <span class="badge ${badgeCls[t.busType] || 'badge-navy'}">${t.busType}</span>
                </div>
              </div>
            </div>

            <div class="flex items-center justify-center gap-4" style="flex:1;">
              <div style="text-align:center;">
                <div style="font-size:1.2rem;font-weight:800;">${t.departureTime}</div>
                <div class="text-xs text-muted truncate" style="max-width:118px;">${escapeHtml(t.pickupStation)}</div>
              </div>
              <div style="text-align:center;">
                <div class="text-xs text-muted">${t.duration}</div>
                <div class="flex items-center gap-2" style="gap:4px;margin-top:4px;">
                  <span style="width:7px;height:7px;border-radius:50%;border:2px solid var(--navy);"></span>
                  <span style="width:46px;height:2px;background:rgba(15,76,129,0.28);"></span>
                  <span style="width:7px;height:7px;border-radius:50%;background:var(--navy);"></span>
                </div>
              </div>
              <div style="text-align:center;">
                <div style="font-size:1.2rem;font-weight:800;">${t.arrivalTime}</div>
                <div class="text-xs text-muted truncate" style="max-width:118px;">${escapeHtml(t.dropStation)}</div>
              </div>
            </div>

            <div class="flex items-center justify-between gap-4" style="justify-content:flex-end;">
              <div style="text-align:right;">
                <div class="text-xs font-bold ${t.seatsLeft <= 5 ? 'text-danger' : 'text-success'}">${t.seatsLeft <= 5 ? `Only ${t.seatsLeft} seats left` : `${t.seatsLeft} seats left`}</div>
                <div style="font-size:1.35rem;font-weight:800;color:var(--orange);">${formatVND(t.price)}</div>
                <div class="text-xs text-muted">per person</div>
              </div>
              <button type="button" class="btn btn-navy" onclick="selectTrip('${t.id}')">Select Seats</button>
            </div>
          </div>

          <div class="flex items-center justify-between gap-3 mt-4" style="padding-top:14px;border-top:1px solid var(--border);flex-wrap:wrap;">
            <div class="flex gap-2" style="flex-wrap:wrap;">
              ${t.amenities.map(k => AMENITIES[k] ? `<span class="text-xs text-muted" style="background:var(--bg);padding:4px 9px;border-radius:8px;display:inline-flex;align-items:center;gap:5px;">${icon(AMENITIES[k].icon, 12)} ${AMENITIES[k].label}</span>` : '').join('')}
            </div>
            <button type="button" class="text-xs font-bold text-navy flex items-center gap-2" style="gap:4px;" onclick="toggleTripExpand('${t.id}')">
              ${exp ? 'Less details' : 'More details'} ${icon(exp ? 'chevronUp' : 'chevronDown', 14)}
            </button>
          </div>
        </div>

        ${exp ? `
          <div style="border-top:1px solid var(--border);background:var(--bg);padding:18px 20px;border-radius:0 0 16px 16px;" class="grid-3 text-sm">
            <div>
              <div class="font-bold mb-2">Pickup point</div>
              <div class="text-muted flex items-center gap-2">${icon('mapPin', 14)} ${escapeHtml(t.pickupStation)}</div>
            </div>
            <div>
              <div class="font-bold mb-2">Drop-off point</div>
              <div class="text-muted flex items-center gap-2">${icon('mapPin', 14)} ${escapeHtml(t.dropStation)}</div>
            </div>
            <div>
              <div class="font-bold mb-2">Cancellation policy</div>
              <div class="text-muted">Free cancellation up to 24h before departure. 50% refund within 24h.</div>
            </div>
          </div>
        ` : ''}
      </div>
    `;
  }).join('');
}

function onPriceSliderInput(el) {
  const val = Number(el.value);
  state.filterMaxPrice = val;
  const pct = getPricePercent(val);
  const formatted = formatVND(val);
  const hasActiveFilter = state.filterBusTypes.length > 0 || state.filterAmenities.length > 0 || val < 800000;

  // Update slider track fill & price labels directly without re-rendering sidebar
  el.style.setProperty('--val', pct + '%');
  document.querySelectorAll('.price-slider').forEach(slider => {
    if (slider !== el) {
      slider.value = val;
      slider.style.setProperty('--val', pct + '%');
    }
  });
  document.querySelectorAll('.js-max-price-value').forEach(lbl => {
    lbl.textContent = formatted;
  });
  document.querySelectorAll('.js-reset-filter-btn').forEach(btn => {
    btn.style.display = hasActiveFilter ? 'inline-flex' : 'none';
  });

  // Only patch trip results container via requestAnimationFrame when matched list changes
  if (priceSliderRafId) cancelAnimationFrame(priceSliderRafId);
  priceSliderRafId = requestAnimationFrame(() => {
    const list = getFilteredTrips();
    const key = list.map(t => t.id).join(',');
    if (key === lastRenderedTripKey) return;
    lastRenderedTripKey = key;

    const listEl = document.getElementById('trip-results-list');
    if (listEl) listEl.innerHTML = renderTripListHtml(list);

    const countEl = document.getElementById('trip-count-text');
    if (countEl) countEl.textContent = list.length;

    const wordEl = document.getElementById('trip-count-word');
    if (wordEl) wordEl.textContent = list.length === 1 ? 'trip' : 'trips';

    document.querySelectorAll('.js-mobile-filter-count').forEach(span => {
      span.textContent = list.length;
    });
  });
}

function renderSearchScreen() {
  const list = getFilteredTrips();
  lastRenderedTripKey = list.map(t => t.id).join(',');

  return `
    <div class="sub-bar">
      <div class="container sub-bar-inner">
        <button type="button" style="padding:8px;border-radius:10px;color:var(--muted);" onclick="navigate('home')">${icon('arrowLeft', 18)}</button>
        <div class="flex items-center gap-2 font-bold">
          <span>${escapeHtml(state.query.from)}</span>
          <span class="text-orange">${icon('arrowRight', 15)}</span>
          <span>${escapeHtml(state.query.to)}</span>
        </div>
        <span class="text-sm text-muted">· ${formatDateEN(state.query.date)} · ${state.query.passengers} passenger${state.query.passengers > 1 ? 's' : ''}</span>
        <div style="margin-left:auto;" class="flex items-center gap-3">
          <span class="text-sm text-muted"><strong id="trip-count-text" style="color:var(--ink);">${list.length}</strong> <span id="trip-count-word">${list.length === 1 ? 'trip' : 'trips'}</span> found</span>
          <button type="button" class="btn btn-navy btn-sm mobile-filter-btn" onclick="state.mobileFilterOpen=true; render();">${icon('filter', 13)} Filter</button>
        </div>
      </div>
    </div>

    <div class="container search-layout">
      <aside class="filter-sidebar">
        <div class="flex items-center gap-2 font-bold text-sm mb-3">${icon('sliders', 15)} Filters</div>
        ${renderFiltersHtml()}
      </aside>

      <div class="results-col">
        <div class="sort-bar">
          ${[['cheapest', 'Cheapest'], ['earliest', 'Earliest'], ['rated', 'Highest rated']].map(([k, lbl]) => `
            <button type="button" class="sort-pill ${state.sortBy === k ? 'active' : ''}" onclick="state.sortBy='${k}'; render();">${lbl}</button>
          `).join('')}
        </div>

        <div id="trip-results-list" class="flex flex-col gap-4">
          ${renderTripListHtml(list)}
        </div>
      </div>
    </div>

    ${state.mobileFilterOpen ? `
      <div class="modal-backdrop" onclick="state.mobileFilterOpen=false; render();">
        <div class="modal-box" onclick="event.stopPropagation()">
          <div class="flex items-center justify-between mb-4">
            <h3 class="font-bold">Filters</h3>
            <button type="button" onclick="state.mobileFilterOpen=false; render();">${icon('x', 18)}</button>
          </div>
          ${renderFiltersHtml()}
          <button type="button" class="btn btn-primary btn-full mt-4" onclick="state.mobileFilterOpen=false; render();">Apply filters (<span class="js-mobile-filter-count">${list.length}</span>)</button>
        </div>
      </div>
    ` : ''}
  `;
}
