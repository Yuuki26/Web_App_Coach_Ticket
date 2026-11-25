/* =========================================================================
 * 15b. SCREEN: ROUTES (live data from the backend)
 * -------------------------------------------------------------------------
 * GET /api/v1/routes                every route with stations and map positions
 * GET /api/v1/routes/{id}/traffic   live traffic (TomTom), only for the route
 *                                   being viewed, to stay within the daily limit
 * The map uses Leaflet + OpenStreetMap tiles (free, no key).
 * ========================================================================= */
const HCMC_NAME = 'TP. Hồ Chí Minh';

const TRAFFIC_COLORS = { NORMAL: '#059669', SLOW: '#FF7A1A', TRAFFIC_JAM: '#DC2626' };
const TRAFFIC_REFRESH_MS = 5 * 60 * 1000; // the backend refreshes each route every 5 minutes

let routeMap = null; // the Leaflet map currently on screen

// ---------- helpers ----------

function fmtMinutes(min) {
  const h = Math.floor(min / 60), m = min % 60;
  return h ? `${h}h ${String(m).padStart(2, '0')}m` : `${m}m`;
}

/** Decodes Google's encoded polyline format into [[lat, lng], ...]. */
function decodePolyline(str) {
  const points = [];
  let i = 0, lat = 0, lng = 0;
  while (i < str.length) {
    for (const axis of [0, 1]) {
      let shift = 0, result = 0, b;
      do {
        b = str.charCodeAt(i++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      const delta = (result & 1) ? ~(result >> 1) : (result >> 1);
      if (axis === 0) lat += delta; else lng += delta;
    }
    points.push([lat / 1e5, lng / 1e5]);
  }
  return points;
}

function getVisibleRoutes() {
  const r = state.routes;
  const q = normalizeSearch(r.q || '');
  return (r.list || [])
    .filter(x => r.filter === 'all'
      || (r.filter === 'from' && x.origin === HCMC_NAME)
      || (r.filter === 'to' && x.destination === HCMC_NAME))
    .filter(x => !q || normalizeSearch(`${x.origin} ${x.destination} ${x.originStation} ${x.destinationStation}`).includes(q));
}

function selectedRoute() {
  return (state.routes.list || []).find(x => x.id === state.routes.selectedId) || null;
}

// ---------- actions ----------

async function openRoutes() {
  navigate('routes');
  if (state.routes.loading) return;
  if (!state.routes.list) return loadRoutes();
  // The list may already be here because a search loaded it.
  if (!state.routes.selectedId && state.routes.list.length) {
    state.routes.selectedId = (state.routes.list.find(x => x.popular) || state.routes.list[0]).id;
    render();
  }
  if (state.routes.selectedId) loadRouteExtras(state.routes.selectedId);
}

async function loadRoutes() {
  state.routes.loading = true;
  state.routes.error = null;
  render();
  try {
    state.routes.list = await apiGet('/routes');
    if (!state.routes.selectedId && state.routes.list.length) {
      const first = state.routes.list.find(x => x.popular) || state.routes.list[0];
      state.routes.selectedId = first.id;
    }
  } catch (e) {
    state.routes.error = e.message;
  }
  state.routes.loading = false;
  if (state.screen !== 'routes') return;
  render();
  if (state.routes.selectedId) loadRouteExtras(state.routes.selectedId);
}

function selectRoute(id) {
  state.routes.selectedId = id;
  render();
  if (window.innerWidth < 900) {
    const el = document.getElementById('route-detail');
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
  loadRouteExtras(id);
}

/** What the detail panel loads for a route: live traffic and the city buses at its HCMC station. */
function loadRouteExtras(id) {
  loadTraffic(id);
  loadStationBuses(id);
}

async function loadTraffic(id, force) {
  const r = state.routes;
  const have = r.traffic[id];
  if (!force && have && Date.now() - have.fetchedAt < TRAFFIC_REFRESH_MS) return;
  if (r.trafficLoading[id]) return;
  r.trafficLoading[id] = true;
  delete r.trafficError[id];
  refreshRouteDetail(id);
  try {
    const data = await apiGet(`/routes/${id}/traffic`);
    r.traffic[id] = { data, fetchedAt: Date.now() };
  } catch (e) {
    r.trafficError[id] = e.message;
  }
  r.trafficLoading[id] = false;
  refreshRouteDetail(id);
}

/** Re-draws the detail panel only if that route is still the one on screen. */
function refreshRouteDetail(id) {
  if (state.screen === 'routes' && state.routes.selectedId === id) render();
}

function setRouteFilter(f) {
  state.routes.filter = f;
  updateRouteList();
}

function onRouteSearch(val) {
  state.routes.q = val;
  updateRouteList();
}

/** Updates only the list, so the search box keeps focus and the map isn't rebuilt. */
function updateRouteList() {
  const el = document.getElementById('routes-list');
  if (el) el.innerHTML = renderRouteListItems();
  document.querySelectorAll('[data-route-filter]').forEach(b =>
    b.classList.toggle('active', b.getAttribute('data-route-filter') === state.routes.filter));
}

function findTripsOnRoute(id) {
  const r = (state.routes.list || []).find(x => x.id === id);
  if (!r) return;
  searchPopularRoute(CITY_TO_PROVINCE[r.origin] || r.origin, CITY_TO_PROVINCE[r.destination] || r.destination);
}

// ---------- rendering ----------

function renderRoutesScreen() {
  const r = state.routes;
  const count = r.list ? getVisibleRoutes().length : 0;
  return `
    <div class="sub-bar">
      <div class="container sub-bar-inner">
        <button type="button" style="padding:8px;border-radius:10px;color:var(--muted);" onclick="navigate('home')" aria-label="Back to home">${icon('arrowLeft', 18)}</button>
        <div class="font-bold">Our routes</div>
        <span class="text-sm text-muted">· Intercity coaches from Ho Chi Minh City's bus stations</span>
        ${r.list ? `<span class="text-sm text-muted" style="margin-left:auto;"><strong style="color:var(--ink);">${r.list.length}</strong> routes</span>` : ''}
      </div>
    </div>

    <div class="container">
      ${r.loading && !r.list ? `
        <div class="routes-empty card">${icon('clock', 22)}<div>Loading routes…</div></div>
      ` : r.error && !r.list ? `
        <div class="routes-empty card">
          <span style="color:var(--danger);">${icon('alertCircle', 24)}</span>
          <div class="font-bold">Couldn't load routes</div>
          <div class="text-sm text-muted">${escapeHtml(r.error)}</div>
          <button type="button" class="btn btn-navy btn-sm mt-2" onclick="loadRoutes()">Try again</button>
        </div>
      ` : `
        <div class="routes-layout">
          <div class="routes-list-col">
            <div class="input-wrap">
              <span class="input-icon">${icon('search', 15)}</span>
              <input class="input input-with-icon" type="search" placeholder="Search a city or bus station"
                     value="${escapeHtml(r.q)}" oninput="onRouteSearch(this.value)" aria-label="Search routes"/>
            </div>
            <div class="sort-bar">
              ${[['all', 'All routes'], ['from', 'From Ho Chi Minh City'], ['to', 'To Ho Chi Minh City']].map(([k, lbl]) => `
                <button type="button" class="sort-pill ${r.filter === k ? 'active' : ''}" data-route-filter="${k}" onclick="setRouteFilter('${k}')">${lbl}</button>
              `).join('')}
            </div>
            <div id="routes-list" class="routes-list" aria-label="${count} routes">${renderRouteListItems()}</div>
          </div>
          <div id="route-detail" class="routes-detail-col">${renderRouteDetail(selectedRoute())}</div>
        </div>
      `}
    </div>
  `;
}

function renderRouteListItems() {
  const list = getVisibleRoutes();
  if (!list.length) {
    return `<div class="routes-empty card" style="padding:28px;"><div class="font-bold">No routes match</div><div class="text-sm text-muted">Try another city or station name.</div></div>`;
  }
  return list.map(x => {
    const t = state.routes.traffic[x.id];
    const level = t && t.data.level;
    return `
      <button type="button" class="route-row card ${x.id === state.routes.selectedId ? 'selected' : ''}" onclick="selectRoute(${x.id})">
        <div style="min-width:0;flex:1;">
          <div class="route-row-title truncate">
            ${escapeHtml(x.origin)} <span class="text-orange">${icon('arrowRight', 14)}</span> ${escapeHtml(x.destination)}
            ${x.popular ? '<span class="badge badge-orange">Popular</span>' : ''}
          </div>
          <div class="text-sm text-muted truncate">${escapeHtml(x.originStation)} → ${escapeHtml(x.destinationStation)}</div>
          <div class="route-row-meta">
            <span>${icon('mapPin', 13)} ${x.distanceKm} km</span>
            <span>${icon('clock', 13)} ${fmtMinutes(x.durationMinutes)}</span>
            ${level ? `<span class="traffic-dot ${level.toLowerCase()}">Traffic: ${level.charAt(0) + level.slice(1).toLowerCase()}</span>` : ''}
          </div>
        </div>
        <div class="route-row-price">
          <div class="text-sm text-muted">From</div>
          <div class="font-bold text-orange">${formatVND(x.fromPrice)}</div>
        </div>
      </button>
    `;
  }).join('');
}

function renderRouteDetail(x) {
  if (!x) return '';
  return `
    <div class="card route-detail-card">
      <div class="route-detail-head">
        <div>
          <h2 class="route-detail-title">${escapeHtml(x.origin)} → ${escapeHtml(x.destination)}</h2>
          <div class="route-stations">
            <div><span class="station-dot from"></span>${escapeHtml(x.originStation)}</div>
            <div><span class="station-dot to"></span>${escapeHtml(x.destinationStation)}</div>
          </div>
        </div>
        <button type="button" class="btn btn-primary" onclick="findTripsOnRoute(${x.id})">${icon('bus', 16)} Find trips</button>
      </div>

      <div id="route-map" class="route-map" role="img" aria-label="Map of the route from ${escapeHtml(x.originStation)} to ${escapeHtml(x.destinationStation)}"></div>
      <div class="map-legend text-sm text-muted">
        <span><i style="background:${TRAFFIC_COLORS.NORMAL}"></i>Flowing</span>
        <span><i style="background:${TRAFFIC_COLORS.SLOW}"></i>Slow</span>
        <span><i style="background:${TRAFFIC_COLORS.TRAFFIC_JAM}"></i>Jam</span>
      </div>

      ${renderTrafficPanel(x)}
      ${renderStationBuses(x)}

      <div class="route-facts">
        <div><div class="text-sm text-muted">Distance</div><div class="font-bold">${x.distanceKm} km</div></div>
        <div><div class="text-sm text-muted">Timetable</div><div class="font-bold">${fmtMinutes(x.durationMinutes)}</div></div>
        <div><div class="text-sm text-muted">Fares from</div><div class="font-bold text-orange">${formatVND(x.fromPrice)}</div></div>
      </div>
    </div>
  `;
}

function renderTrafficPanel(x) {
  const r = state.routes;
  const t = r.traffic[x.id];
  const err = r.trafficError[x.id];
  if (r.trafficLoading[x.id] && !t) {
    return `<div class="traffic-panel"><span class="text-muted">${icon('clock', 16)} Checking live traffic…</span></div>`;
  }
  if (err && !t) {
    return `
      <div class="traffic-panel">
        <div class="text-sm"><strong>Live traffic unavailable.</strong> <span class="text-muted">${escapeHtml(err)}</span></div>
        <button type="button" class="btn btn-outline btn-sm" onclick="loadTraffic(${x.id}, true)">Try again</button>
      </div>`;
  }
  if (!t) return '';
  const d = t.data;
  const level = d.level.toLowerCase();
  const badge = { light: 'badge-green', moderate: 'badge-orange', heavy: 'badge-red' }[level];
  const source = { TOMTOM: 'TomTom', GOOGLE: 'Google Maps', SIMULATED: 'simulated data' }[d.source] || d.source;
  return `
    <div class="traffic-panel ${level}">
      <div>
        <div class="text-sm text-muted">Driving time right now</div>
        <div class="traffic-now">${fmtMinutes(d.currentMinutes)} <span class="badge ${badge}">${level.charAt(0).toUpperCase() + level.slice(1)} traffic</span></div>
        <div class="text-sm text-muted">
          ${fmtMinutes(d.typicalMinutes)} on an empty road${d.delayMinutes ? ` · <strong style="color:var(--ink);">+${d.delayMinutes} min</strong> from traffic` : ''}
        </div>
      </div>
      <div class="text-sm text-muted traffic-src">
        Updated ${d.checkedAt.slice(11, 16)} · ${escapeHtml(source)}
        ${d.stale ? `<div style="color:var(--danger);">${escapeHtml(d.note || 'May be out of date')}</div>` : ''}
        <button type="button" class="link-btn" onclick="loadTraffic(${x.id}, true)" ${r.trafficLoading[x.id] ? 'disabled' : ''}>${r.trafficLoading[x.id] ? 'Refreshing…' : 'Refresh'}</button>
      </div>
    </div>`;
}

/** Called by render() after the HTML is on the page: (re)builds the Leaflet map. */
function mountRouteMap() {
  if (routeMap) { routeMap.remove(); routeMap = null; }
  const el = document.getElementById('route-map');
  const x = selectedRoute();
  if (!el || !x) return;
  if (typeof L === 'undefined') {
    el.innerHTML = '<div class="routes-empty">Map unavailable (no internet connection?)</div>';
    return;
  }
  if (x.originLat == null || x.destinationLat == null) {
    el.innerHTML = '<div class="routes-empty">No map position for this route yet</div>';
    return;
  }

  routeMap = L.map(el, { scrollWheelZoom: false, attributionControl: true });
  L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 18,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
  }).addTo(routeMap);

  const from = [x.originLat, x.originLng];
  const to = [x.destinationLat, x.destinationLng];
  const t = state.routes.traffic[x.id];
  let bounds = L.latLngBounds([from, to]);

  if (t && t.data.encodedPolyline) {
    const pts = decodePolyline(t.data.encodedPolyline);
    const intervals = t.data.speedIntervals && t.data.speedIntervals.length
      ? t.data.speedIntervals
      : [{ start: 0, end: pts.length - 1, speed: 'NORMAL' }];
    // A wide white casing under the coloured line keeps it readable on the map.
    L.polyline(pts, { color: '#fff', weight: 9, opacity: 0.9 }).addTo(routeMap);
    intervals.forEach(iv => {
      L.polyline(pts.slice(iv.start, iv.end + 1), { color: TRAFFIC_COLORS[iv.speed] || TRAFFIC_COLORS.NORMAL, weight: 5 })
        .addTo(routeMap);
    });
    bounds = L.latLngBounds(pts);
  } else {
    L.polyline([from, to], { color: '#0F4C81', weight: 3, dashArray: '6 8', opacity: 0.7 }).addTo(routeMap);
  }

  L.circleMarker(from, { radius: 8, color: '#fff', weight: 3, fillColor: '#0F4C81', fillOpacity: 1 })
    .bindTooltip(x.originStation).addTo(routeMap);
  L.circleMarker(to, { radius: 8, color: '#fff', weight: 3, fillColor: '#FF7A1A', fillOpacity: 1 })
    .bindTooltip(x.destinationStation).addTo(routeMap);
  fitMapWhenSized(routeMap, el, bounds, 24);
}

/**
 * Fits a Leaflet map to bounds. If its box has no size yet (page not laid out, e.g. a background
 * tab), waits until it has one; otherwise Leaflet would zoom all the way in.
 */
function fitMapWhenSized(map, el, bounds, padding) {
  // A first view makes Leaflet "loaded"; until then it ignores invalidateSize().
  map.setView(bounds.getCenter(), 12, { animate: false });
  const tryFit = () => {
    if (!(el.clientWidth > 0 && el.clientHeight > 0)) return false;
    map.invalidateSize(false);
    if (map.getSize().x === 0) return false;
    map.fitBounds(bounds, { padding: [padding, padding], animate: false });
    return true;
  };
  if (tryFit()) return;
  const ro = new ResizeObserver(() => {
    if (!el.isConnected || tryFit()) ro.disconnect();
  });
  ro.observe(el);
}

// ---------- City buses at the HCMC station (see 15c-city-buses.js) ----------

/** The HCMC end of a route: where riders change to a city bus. */
function hcmcStation(x) {
  if (x.origin === HCMC_NAME) return { name: x.originStation, lat: x.originLat, lng: x.originLng };
  if (x.destination === HCMC_NAME) return { name: x.destinationStation, lat: x.destinationLat, lng: x.destinationLng };
  return null;
}

async function loadStationBuses(routeId) {
  const r = state.routes;
  const x = (r.list || []).find(y => y.id === routeId);
  const st = x && hcmcStation(x);
  if (!st || st.lat == null || r.stationBuses[st.name]) return;
  r.stationBuses[st.name] = { loading: true };
  try {
    const stops = await apiGet(`/city-buses/stops/near?lat=${st.lat}&lng=${st.lng}&radius=400`);
    const lines = new Map();
    stops.forEach(s => s.lines.forEach(l => { if (!lines.has(l.id)) lines.set(l.id, l); }));
    r.stationBuses[st.name] = { lines: [...lines.values()] };
  } catch (e) {
    r.stationBuses[st.name] = { error: true };
  }
  refreshRouteDetail(routeId);
}

function renderStationBuses(x) {
  const st = hcmcStation(x);
  const info = st && state.routes.stationBuses[st.name];
  if (!info || info.loading || info.error || !info.lines.length) return '';
  const sorted = [...info.lines].sort((a, b) => (parseInt(a.ref, 10) || 9999) - (parseInt(b.ref, 10) || 9999) || a.ref.localeCompare(b.ref));
  return `
    <div class="station-buses">
      <div class="text-sm"><strong>City buses at ${escapeHtml(st.name)}</strong> <span class="text-muted">· within 400 m</span></div>
      <div class="station-buses-row">
        ${sorted.map(l => `<button type="button" onclick="openCityBuses(${l.id})" title="Bus line ${escapeHtml(l.ref)}">${lineBadge(l.ref, l.colour, 'small')}</button>`).join('')}
      </div>
    </div>`;
}
