/* =========================================================================
 * 15c. SCREEN: CITY BUSES (Ho Chi Minh City bus lines, live from the backend)
 * -------------------------------------------------------------------------
 * GET /api/v1/city-buses?q=           lines; q = number, street, stop name
 * GET /api/v1/city-buses/{id}         one line: each direction's path + stops
 * GET /api/v1/city-buses/stops/near   stops near a point (used on Routes too)
 * Data © OpenStreetMap contributors (ODbL). Map: Leaflet + OSM tiles.
 * ========================================================================= */
let cityBusMap = null;      // the Leaflet map on screen
let cityBusStopMarkers = []; // markers of the shown direction, for "show on map"
let cityBusSearchTimer = null;

// ---------- formatting ----------

/** A line's colour from OSM, only when it is a plain hex colour. */
function lineColour(c) {
  return /^#[0-9a-fA-F]{3,8}$/.test(c || '') ? c : '#0F4C81';
}

/** "Mo-Su 05:00-22:00" → "Daily 05:00–22:00"; other formats are shown as they are. */
function fmtBusHours(h) {
  if (!h) return '—';
  return h.replace(/^Mo-Su\s+/, 'Daily ').replace(/(\d{2}:\d{2})-(\d{2}:\d{2})/g, '$1–$2');
}

/** "00:08-00:20" → "Every 8–20 min"; "00:15" → "Every 15 min". */
function fmtHeadway(h) {
  if (!h) return '—';
  const mins = h.match(/\d{1,2}:\d{2}/g);
  if (!mins) return h;
  const toMin = t => { const [a, b] = t.split(':').map(Number); return a * 60 + b; };
  const v = mins.map(toMin);
  return v.length > 1 && v[0] !== v[1] ? `Every ${v[0]}–${v[1]} min` : `Every ${v[0]} min`;
}

function fmtBusFare(line) {
  return line.fareVnd ? formatVND(line.fareVnd) : (line.fare || '—');
}

/** A coloured line-number badge; size is '', 'big' or 'small'. */
function lineBadge(ref, colour, size) {
  return `<span class="line-badge ${size || ''}" style="background:${lineColour(colour)};">${escapeHtml(ref)}</span>`;
}

// ---------- actions ----------

/** Opens the City buses screen, optionally on one line (e.g. from a Routes chip). */
async function openCityBuses(lineId) {
  const c = state.cityBus;
  if (lineId) { c.selectedId = lineId; c.dir = 0; }
  navigate('citybus');
  if (!c.list && !c.loading) await loadCityBusList();
  if (c.selectedId) loadCityBusLine(c.selectedId);
}

async function loadCityBusList() {
  const c = state.cityBus;
  c.loading = true;
  c.error = null;
  render();
  try {
    c.list = await apiGet('/city-buses');
    if (!c.selectedId && c.list.length) c.selectedId = c.list[0].id;
  } catch (e) {
    c.error = e.message;
  }
  c.loading = false;
  if (state.screen !== 'citybus') return;
  render();
  if (c.selectedId) loadCityBusLine(c.selectedId);
}

/** Search runs on the server (it also matches stop names), a moment after typing stops. */
function onCityBusSearch(val) {
  state.cityBus.q = val;
  clearTimeout(cityBusSearchTimer);
  cityBusSearchTimer = setTimeout(async () => {
    const c = state.cityBus;
    const q = c.q.trim();
    try {
      c.results = q ? await apiGet(`/city-buses?q=${encodeURIComponent(q)}`) : null;
    } catch (e) {
      c.results = [];
    }
    if (q !== c.q.trim()) return; // typed more meanwhile
    const el = document.getElementById('citybus-list');
    if (el) el.innerHTML = renderCityBusListItems();
    const count = document.getElementById('citybus-count');
    if (count) count.textContent = (c.results || c.list || []).length;
  }, 250);
}

function selectCityBus(id) {
  state.cityBus.selectedId = id;
  state.cityBus.dir = 0;
  render();
  if (window.innerWidth < 900) {
    const el = document.getElementById('citybus-detail');
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  loadCityBusLine(id);
}

async function loadCityBusLine(id) {
  const c = state.cityBus;
  if (c.detail[id] || c.detailLoading[id]) return;
  c.detailLoading[id] = true;
  delete c.detailError[id];
  try {
    c.detail[id] = await apiGet(`/city-buses/${id}`);
  } catch (e) {
    c.detailError[id] = e.message;
  }
  c.detailLoading[id] = false;
  if (state.screen === 'citybus' && c.selectedId === id) render();
}

function setCityBusDirection(i) {
  state.cityBus.dir = i;
  render();
}

/** Centres the map on the n-th stop of the shown direction and shows its name. */
function showBusStop(index) {
  const m = cityBusStopMarkers[index];
  if (!m || !cityBusMap) return;
  cityBusMap.setView(m.getLatLng(), Math.max(cityBusMap.getZoom(), 16));
  m.openTooltip();
  const mapEl = document.getElementById('citybus-map');
  if (mapEl && window.innerWidth < 900) mapEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

// ---------- rendering ----------

function renderCityBusScreen() {
  const c = state.cityBus;
  const shown = c.results || c.list || [];
  return `
    <div class="sub-bar">
      <div class="container sub-bar-inner">
        <button type="button" style="padding:8px;border-radius:10px;color:var(--muted);" onclick="navigate('home')" aria-label="Back to home">${icon('arrowLeft', 18)}</button>
        <div class="font-bold">City buses</div>
        <span class="text-sm text-muted">· Ho Chi Minh City bus lines, stops and maps</span>
        ${c.list ? `<span class="text-sm text-muted" style="margin-left:auto;"><strong id="citybus-count" style="color:var(--ink);">${shown.length}</strong> lines</span>` : ''}
      </div>
    </div>

    <div class="container">
      ${c.loading && !c.list ? `
        <div class="routes-empty card">${icon('clock', 22)}<div>Loading bus lines…</div></div>
      ` : c.error && !c.list ? `
        <div class="routes-empty card">
          <span style="color:var(--danger);">${icon('alertCircle', 24)}</span>
          <div class="font-bold">Couldn't load bus lines</div>
          <div class="text-sm text-muted">${escapeHtml(c.error)}</div>
          <button type="button" class="btn btn-navy btn-sm mt-2" onclick="loadCityBusList()">Try again</button>
        </div>
      ` : `
        <div class="routes-layout">
          <div class="routes-list-col">
            <div class="input-wrap">
              <span class="input-icon">${icon('search', 15)}</span>
              <input class="input input-with-icon" type="search" placeholder="Line number, street or stop (e.g. 19, Bến Thành)"
                     value="${escapeHtml(c.q)}" oninput="onCityBusSearch(this.value)" aria-label="Search bus lines"/>
            </div>
            <div id="citybus-list" class="routes-list">${renderCityBusListItems()}</div>
          </div>
          <div id="citybus-detail" class="routes-detail-col">${renderCityBusDetail()}</div>
        </div>
      `}
    </div>
  `;
}

function renderCityBusListItems() {
  const c = state.cityBus;
  const list = c.results || c.list || [];
  if (!list.length) {
    return `<div class="routes-empty card" style="padding:28px;"><div class="font-bold">No bus lines match</div><div class="text-sm text-muted">Try a line number or another stop name.</div></div>`;
  }
  return list.map(x => `
    <button type="button" class="route-row card ${x.id === c.selectedId ? 'selected' : ''}" onclick="selectCityBus(${x.id})">
      ${lineBadge(x.ref, x.colour)}
      <div style="min-width:0;flex:1;">
        <div class="route-row-title truncate">${escapeHtml(x.name)}</div>
        <div class="route-row-meta" style="margin-top:4px;">
          <span>${icon('clock', 13)} ${escapeHtml(fmtBusHours(x.openingHours))}</span>
          <span>${escapeHtml(fmtHeadway(x.headway))}</span>
          <span>${escapeHtml(fmtBusFare(x))}</span>
        </div>
      </div>
    </button>
  `).join('');
}

function renderCityBusDetail() {
  const c = state.cityBus;
  const id = c.selectedId;
  if (!id) return '';
  const d = c.detail[id];
  if (!d) {
    return `<div class="card route-detail-card">${c.detailError[id] ? `
        <div class="font-bold">Couldn't load this line</div>
        <div class="text-sm text-muted">${escapeHtml(c.detailError[id])}</div>
        <button type="button" class="btn btn-outline btn-sm" onclick="loadCityBusLine(${id})">Try again</button>`
      : `<div class="text-muted">${icon('clock', 16)} Loading line…</div>`}</div>`;
  }
  const line = d.line;
  const dir = d.directions[Math.min(c.dir, d.directions.length - 1)];
  return `
    <div class="card route-detail-card">
      <div class="flex items-center gap-3">
        ${lineBadge(line.ref, line.colour, 'big')}
        <div style="min-width:0;">
          <h2 class="route-detail-title" style="margin-bottom:2px;">${escapeHtml(line.name)}</h2>
          <div class="text-sm text-muted truncate">${escapeHtml(line.operator || '')}</div>
        </div>
      </div>

      ${d.directions.length > 1 ? `
        <div class="sort-bar">
          ${d.directions.map((x, i) => `
            <button type="button" class="sort-pill ${i === c.dir ? 'active' : ''}" onclick="setCityBusDirection(${i})">
              To ${escapeHtml(x.to || x.name)}
            </button>`).join('')}
        </div>` : ''}

      <div id="citybus-map" class="route-map" role="img" aria-label="Map of bus line ${escapeHtml(line.ref)}"></div>

      <div class="route-facts" style="grid-template-columns:repeat(2,1fr);">
        <div><div class="text-sm text-muted">Hours</div><div class="font-bold">${escapeHtml(fmtBusHours(line.openingHours))}</div></div>
        <div><div class="text-sm text-muted">Frequency</div><div class="font-bold">${escapeHtml(fmtHeadway(line.headway))}</div></div>
        <div><div class="text-sm text-muted">Single ticket</div><div class="font-bold text-orange">${escapeHtml(fmtBusFare(line))}</div></div>
        <div><div class="text-sm text-muted">This direction</div><div class="font-bold">${dir.lengthKm} km · ${dir.stops.length} stops</div></div>
      </div>

      <div>
        <div class="font-bold text-sm mb-2">Stops to ${escapeHtml(dir.to || '')}</div>
        <ol class="bus-stop-list">
          ${dir.stops.map((s, i) => `
            <li><button type="button" onclick="showBusStop(${i})">
              <span class="stop-num" style="border-color:${lineColour(line.colour)};">${i + 1}</span>
              <span>${escapeHtml(s.name)}</span>
            </button></li>`).join('')}
        </ol>
      </div>
      <div class="text-xs text-muted">${escapeHtml(d.attribution)} · Times and fares may change; check at the stop.</div>
    </div>
  `;
}

/** Called by render(): draws the shown direction (solid) and the other direction (faint). */
function mountCityBusMap() {
  if (cityBusMap) { cityBusMap.remove(); cityBusMap = null; }
  cityBusStopMarkers = [];
  const el = document.getElementById('citybus-map');
  const c = state.cityBus;
  const d = c.detail[c.selectedId];
  if (!el || !d) return;
  if (typeof L === 'undefined') {
    el.innerHTML = '<div class="routes-empty">Map unavailable (no internet connection?)</div>';
    return;
  }
  const colour = lineColour(d.line.colour);
  cityBusMap = L.map(el, { scrollWheelZoom: false });
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  }).addTo(cityBusMap);

  const shownIndex = Math.min(c.dir, d.directions.length - 1);
  d.directions.forEach((dir, i) => {
    if (i === shownIndex) return;
    L.polyline(decodePolyline(dir.polyline), { color: colour, weight: 3, opacity: 0.35, dashArray: '4 8' }).addTo(cityBusMap);
  });
  const dir = d.directions[shownIndex];
  const path = decodePolyline(dir.polyline);
  L.polyline(path, { color: '#fff', weight: 8, opacity: 0.9 }).addTo(cityBusMap);
  L.polyline(path, { color: colour, weight: 5 }).addTo(cityBusMap);
  dir.stops.forEach((s, i) => {
    const end = i === 0 || i === dir.stops.length - 1;
    const m = L.circleMarker([s.lat, s.lng], {
      radius: end ? 7 : 4, color: end ? '#fff' : colour, weight: end ? 3 : 2,
      fillColor: end ? (i === 0 ? '#0F4C81' : '#FF7A1A') : '#fff', fillOpacity: 1
    }).bindTooltip(`${i + 1}. ${escapeHtml(s.name)}`).addTo(cityBusMap);
    cityBusStopMarkers.push(m);
  });
  fitMapWhenSized(cityBusMap, el, L.latLngBounds(path.length ? path : dir.stops.map(s => [s.lat, s.lng])), 20);
}
