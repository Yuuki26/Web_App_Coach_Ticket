/* H Coach frontend. All data comes from the Spring Boot API under /api/v1. */

const $ = s => document.querySelector(s), $$ = s => [...document.querySelectorAll(s)];

// ---------- API ----------

/** api('/path') is a GET; api('/path', body) is a POST with a JSON body. Throws an Error with the server's message. */
async function api(path, body) {
    let res;
    try {
        res = await fetch('/api/v1' + path, body === undefined ? undefined : {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
    } catch (e) {
        throw new Error('Can\'t reach the server. Check your connection and try again');
    }
    const data = await res.json().catch(() => null);
    if (!res.ok) throw new Error((data && data.detail) || 'Something went wrong. Please try again');
    return data;
}

// ---------- Presentation-only data (not stored on the server) ----------

/** Gradient and photo for the popular-route cards, in the order the API returns the routes. */
const GRADIENTS = ['#0F4C81,#3B82F6', '#C2410C,#FBBF24', '#0E7490,#34D399', '#4338CA,#A78BFA', '#BE185D,#FB923C', '#047857,#67E8F9'];
const PHOTOS = ['1605833556294-ea5c7a74f57d', '1534430480872-3498386e7856', '1533929736458-ca588d08c8be', '1502175353174-a7a70e73b362', '1477959858617-67f85cf4f1df', '1449824913935-59a10b8d2000']
    .map(i => `https://images.unsplash.com/photo-${i}?w=600&h=400&fit=crop&auto=format`);

// ---------- State ----------

const iso = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
const TODAY = new Date();
const D = new Date();
D.setDate(D.getDate() + 3);

let S = {
    scr: 'home',
    q: { from: 'TP. Hồ Chí Minh', to: 'Đà Lạt', date: iso(D), pax: 1 },
    sort: 'Cheapest',
    type: 'All',
    cities: [],      // from GET /cities
    popular: [],     // from GET /routes/popular
    results: [],     // from GET /trips/search
    traffic: null,   // from GET /routes/{id}/traffic, for the route being searched
    trip: null,      // the trip being booked
    seatMap: null,   // from GET /trips/{id}/seats
    seats: [],       // selected seat labels, e.g. ['3A']
    quote: null,     // from POST /bookings/quote
    promoCode: '',
    pay: 'card',
    tab: 'Upcoming',
    B: [],           // bookings shown on My Trips
    last: null       // the booking just made
};

// ---------- Helpers ----------

const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
/** Money in Vietnamese đồng, e.g. 280.000 ₫ */
const m = n => Number(n).toLocaleString('vi-VN', { maximumFractionDigits: 0 }) + ' ₫';
const fd = d => new Date(d.slice(0, 10) + 'T00:00').toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
const tm = t => new Date(t).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
const dur = min => `${Math.floor(min / 60)}h ${String(min % 60).padStart(2, '0')}m`;
const cap = s => s.charAt(0) + s.slice(1).toLowerCase();
const plural = (n, word) => `${n} ${word}${n > 1 ? 's' : ''}`;

/**
 * Booking codes + emails this browser has booked or looked up, so My Trips can reload them.
 * Kept only in this browser; the server still checks code and email on every request.
 */
const refs = {
    all() {
        try { return JSON.parse(localStorage.getItem('hcoach.bookings')) || []; } catch (e) { return []; }
    },
    add(code, email) {
        const list = this.all().filter(r => r.code !== code);
        list.unshift({ code, email });
        try { localStorage.setItem('hcoach.bookings', JSON.stringify(list)); } catch (e) { /* private mode: list lasts for this visit only */ }
    }
};

function toast(t) {
    const e = $('#toast');
    e.textContent = t;
    e.style.display = 'block';
    clearTimeout(toast.t);
    toast.t = setTimeout(() => e.style.display = 'none', 2600);
}

function go(s) {
    S.scr = s;
    render();
    scrollTo(0, 0);
}

function render() {
    $('#app').innerHTML = V[S.scr]();
    if (S.scr == 'pay') sum();
}

const opts = (a, v) => a.map(c => `<option ${c == v ? 'selected' : ''}>${esc(c)}</option>`).join('');

const steps = n => `<div class="steps">${['Seats', 'Details', 'Payment', 'Done'].map((s, i) => `<span class="${i <= n ? 'on' : ''}">${s}</span>`).join('')}</div>`;

/** Decorative QR-style pattern drawn from the booking code. */
const qr = s => {
    let h = 7, o = '';
    for (const c of s) h = (h * 31 + c.charCodeAt(0)) >>> 0;
    for (let y = 0; y < 21; y++) {
        for (let x = 0; x < 21; x++) {
            if ((x < 8 && y < 8) || (x > 12 && y < 8) || (x < 8 && y > 12)) continue;
            h = (h * 1664525 + 1013904223) >>> 0;
            if ((h >>> 28) & 1) o += `<rect x="${x}" y="${y}" width="1" height="1"/>`;
        }
    }
    [[0, 0], [14, 0], [0, 14]].forEach(([x, y]) => o += `<path fill-rule="evenodd" d="M${x} ${y}h7v7h-7zM${x + 1} ${y + 1}v5h5v-5z"/><rect x="${x + 2}" y="${y + 2}" width="3" height="3"/>`);
    return `<svg viewBox="0 0 21 21" width="120" height="120" shape-rendering="crispEdges">${o}</svg>`;
};

// ---------- Actions ----------

async function search() {
    if (S.q.from == S.q.to) return toast('Choose two different cities');
    if (!S.q.date) return toast('Choose a travel date');
    try {
        const q = S.q;
        S.results = await api(`/trips/search?from=${encodeURIComponent(q.from)}&to=${encodeURIComponent(q.to)}&date=${q.date}`);
        S.seats = [];
        S.traffic = null;
        go('search');
        if (S.results.length) loadTraffic(S.results[0].routeId);
    } catch (e) {
        toast(e.message);
    }
}

/** Live traffic on the route; the line simply stays hidden if the server can't get it. */
async function loadTraffic(routeId) {
    try {
        const t = await api(`/routes/${routeId}/traffic`);
        if (S.scr == 'search' && S.results.length && S.results[0].routeId == routeId) {
            S.traffic = t;
            render();
        }
    } catch (e) { /* traffic is optional */ }
}

const trafficLine = t => !t ? '' : `
    <div class="traffic ${t.level.toLowerCase()}">
        <b>Road now: ${dur(t.currentMinutes)}</b>
        <span class="mu">(${dur(t.typicalMinutes)} without traffic) · ${cap(t.level)} traffic${t.delayMinutes ? `, +${t.delayMinutes} min` : ''}
        · checked ${tm(t.checkedAt)}${t.source == 'SIMULATED' ? ' · simulated data' : ''}${t.stale ? ' · may be out of date' : ''}</span>
    </div>`;

function pick(i) {
    S.q.from = S.popular[i].origin;
    S.q.to = S.popular[i].destination;
    search();
}

async function choose(id) {
    try {
        S.seatMap = await api(`/trips/${id}/seats`);
        S.trip = S.results.find(t => t.id == id);
        S.seats = [];
        S.quote = null;
        S.promoCode = '';
        go('seats');
    } catch (e) {
        toast(e.message);
    }
}

/** Asks the server for the price of the current selection. */
async function requote() {
    S.quote = S.seats.length
        ? await api('/bookings/quote', { tripId: S.trip.id, seats: S.seats, promoCode: S.promoCode })
        : null;
}

async function seat(id) {
    const i = S.seats.indexOf(id);
    if (i > -1) {
        S.seats.splice(i, 1);
    } else if (S.seats.length < S.q.pax) {
        S.seats.push(id);
    } else {
        return toast(`You can select ${plural(S.q.pax, 'seat')}`);
    }
    try {
        await requote();
    } catch (e) {
        toast(e.message);
    }
    render();
}

function setPay(v) {
    S.pay = v;
    $$('.opt').forEach(o => o.classList.toggle('on', o.dataset.v == v));
    $('#cf').style.display = v == 'card' ? 'grid' : 'none';
}

function sum() {
    const c = S.quote;
    if (!c) return;
    $('#sum').innerHTML = `
        <div class="r">
            <span>Fare (${plural(S.seats.length, 'seat')})</span>
            <span>${m(c.fare)}</span>
        </div>
        ${c.promoApplied ? `<div class="r" style="color:var(--ok)"><span>Promo ${esc(c.promoCode)}</span><span>−${m(c.discount)}</span></div>` : ''}
        <div class="r">
            <span>Taxes & fees</span>
            <span>${m(c.tax)}</span>
        </div>
        <div class="r tot">
            <span>Total</span>
            <span>${m(c.total)}</span>
        </div>`;
}

async function promo() {
    S.promoCode = $('#pc').value.trim();
    try {
        await requote();
    } catch (e) {
        return toast(e.message);
    }
    $('#pm').textContent = S.quote.promoMessage || '';
    $('#pm').style.color = S.quote.promoApplied ? 'var(--ok)' : 'var(--er)';
    sum();
}

async function payNow() {
    const nm = $('#nm').value.trim(), em = $('#em').value.trim();
    if (!nm || !/\S+@\S+\.\S+/.test(em)) return toast('Enter your name and a valid email');
    if (!$('#tc').checked) return toast('Accept the terms to continue');

    const b = $('#pb');
    b.disabled = true;
    b.textContent = 'Processing…';

    try {
        // Card fields are part of the demo design only. They are never read or sent anywhere.
        S.last = await api('/bookings', {
            tripId: S.trip.id,
            passengerName: nm,
            email: em,
            phone: $('#ph').value.trim() || null,
            seats: S.seats,
            promoCode: S.promoCode || null,
            paymentMethod: S.pay.toUpperCase()
        });
        refs.add(S.last.bookingCode, S.last.email);
        go('done');
    } catch (e) {
        toast(e.message);
        b.disabled = false;
        b.textContent = 'Pay Now';
    }
}

/** Loads every booking this browser knows about, then shows My Trips. */
async function openTrips(tab) {
    if (tab) S.tab = tab;
    const found = await Promise.all(refs.all().map(r =>
        api('/bookings/lookup', { bookingCode: r.code, email: r.email }).catch(() => null)));
    S.B = found.filter(Boolean);
    go('trips');
}

function cancelAsk(id) {
    $('#modal').innerHTML = `
        <div class="md">
            <div class="card">
                <h3>Cancel this trip?</h3>
                <p class="mu">You'll get a full refund to your original payment method within 5–7 business days.</p>
                <div class="fr" style="margin-top:16px">
                    <button class="btn s" onclick="$('#modal').innerHTML=''">Keep trip</button>
                    <button class="btn" onclick="cancelGo('${esc(id)}')">Cancel trip</button>
                </div>
            </div>
        </div>`;
}

async function cancelGo(id) {
    const b = S.B.find(b => b.bookingCode == id);
    $('#modal').innerHTML = '';
    try {
        const updated = await api('/bookings/cancel', { bookingCode: b.bookingCode, email: b.email });
        S.B[S.B.indexOf(b)] = updated;
        toast('Trip cancelled. Refund on its way');
        render();
    } catch (e) {
        toast(e.message);
    }
}

async function find() {
    const code = $('#lc').value.trim(), email = $('#le').value.trim();
    if (!code || !email) return toast('Enter your booking code and email');
    try {
        const b = await api('/bookings/lookup', { bookingCode: code, email });
        refs.add(b.bookingCode, b.email);
        await openTrips(b.status);
        toast('Booking found');
    } catch (e) {
        toast(e.message);
    }
}

// ---------- Views ----------

const form = () => `
    <div class="sf">
        <div>
            <label class="f">From</label>
            <select onchange="S.q.from=this.value">${opts(S.cities, S.q.from)}</select>
        </div>
        <div>
            <label class="f">To</label>
            <select onchange="S.q.to=this.value">${opts(S.cities, S.q.to)}</select>
        </div>
        <div>
            <label class="f">Departure</label>
            <input type="date" value="${S.q.date}" min="${iso(TODAY)}" onchange="S.q.date=this.value">
        </div>
        <div>
            <label class="f">Passengers</label>
            <select onchange="S.q.pax=+this.value">
                ${[1, 2, 3, 4, 5, 6].map(n => `<option ${n == S.q.pax ? 'selected' : ''}>${n}</option>`).join('')}
            </select>
        </div>
        <button class="btn" onclick="search()">Search Trips</button>
    </div>`;

const BUS = `
    <svg viewBox="0 0 420 200">
        <rect x="10" y="30" width="400" height="120" rx="22" fill="#2B6CB0"/>
        <rect x="10" y="108" width="400" height="14" fill="#FF7A1A"/>
        ${[0, 1, 2, 3, 4].map(i => `<rect x="${34 + i * 58}" y="48" width="44" height="40" rx="8" fill="#A9CCF2"/>`).join('')}
        <rect x="320" y="48" width="70" height="52" rx="8" fill="#A9CCF2"/>
        <text x="150" y="140" fill="#9fc5ec" font-weight="800" font-size="14" letter-spacing="6">H COACH</text>
        <circle cx="95" cy="154" r="24" fill="#12395f"/>
        <circle cx="95" cy="154" r="9" fill="#9fc5ec"/>
        <circle cx="325" cy="154" r="24" fill="#12395f"/>
        <circle cx="325" cy="154" r="9" fill="#9fc5ec"/>
    </svg>`;

const V = {
    home: () => `
        <section class="hero">
            <div class="w">
                <div>
                    <span class="bd">15,000+ daily trips across the US</span>
                    <h1>Book your coach ticket in <em>60 seconds</em></h1>
                    <p>Fast, affordable intercity travel on verified coach operators. Instant e-tickets and free cancellation up to 24 hours before departure.</p>
                    <div class="stats">
                        <div><b>200+</b>Routes</div>
                        <div><b>4.8★</b>App rating</div>
                        <div><b>2M+</b>Happy riders</div>
                    </div>
                </div>
                ${BUS}
            </div>
        </section>
        <div class="w">
            ${form()}
            <h2 style="margin-bottom:4px">Popular routes</h2>
            <p class="mu" style="margin-bottom:16px">Thousands of daily departures on our most-traveled corridors</p>
            <div class="g3">
                ${S.popular.map((r, i) => `
                    <div class="card rt" style="background:linear-gradient(180deg,transparent 35%,rgba(10,30,60,.9)),url('${PHOTOS[i % PHOTOS.length]}') center/cover,linear-gradient(135deg,${GRADIENTS[i % GRADIENTS.length]})" onclick="pick(${i})">
                        ${i < 2 ? `<span class="rb">${i ? 'Top Deal' : 'Most Popular'}</span>` : ''}
                        <small>${dur(r.durationMinutes)} · direct</small>
                        <h3>${esc(r.origin)} to ${esc(r.destination)}</h3>
                        <span>From <b>${m(r.fromPrice)}</b></span>
                    </div>`).join('')}
            </div>
            <h2>Why choose H Coach</h2>
            <div class="g4">
                <div class="card"><b>Instant e-ticket</b><span class="mu">Get your QR ticket the moment you pay.</span></div>
                <div class="card"><b>Verified operators</b><span class="mu">Every partner is checked for safety and comfort.</span></div>
                <div class="card"><b>Free cancellation</b><span class="mu">Change your plans up to 24 hours before departure.</span></div>
                <div class="card"><b>24/7 support</b><span class="mu">Real people, any time of day.</span></div>
            </div>
            <h2>How it works</h2>
            <div class="g4">
                <div class="card"><b>1. Search</b><span class="mu">Pick your cities and travel date.</span></div>
                <div class="card"><b>2. Choose a seat</b><span class="mu">See the seat map and select yours.</span></div>
                <div class="card"><b>3. Pay and go</b><span class="mu">Card, Apple Pay, Google Pay or PayPal.</span></div>
            </div>
            <h2>Current deals</h2>
            <div class="g3">
                <div class="dl" style="background:linear-gradient(135deg,#FF7A1A,#FF9F4A)">
                    <b style="font-size:20px">10% off your first ride</b>
                    <span>New to H Coach? Save on any route.</span>
                    <code>FIRSTRIDE</code>
                </div>
                <div class="dl" style="background:linear-gradient(135deg,#0F4C81,#2F7FC4)">
                    <b style="font-size:20px">Weekend getaways from $19</b>
                    <span>Seattle, Portland and more.</span>
                    <code>WEEKEND</code>
                </div>
                <div class="dl" style="background:linear-gradient(135deg,#22A06B,#4CC38A)">
                    <b style="font-size:20px">Kids under 5 ride free</b>
                    <span>One free child seat per adult.</span>
                    <code>FAMILY</code>
                </div>
            </div>
            <h2>What riders say</h2>
            <div class="g3 rv">
                ${[['Maria L.', 'Chicago to Detroit', 'Seats were roomy and the e-ticket scanned instantly. Booking took less than a minute.'],
                   ['James K.', 'New York to Boston', 'Cheapest option I found, and the Wi-Fi actually worked the whole ride.'],
                   ['Aisha R.', 'Los Angeles to Las Vegas', 'Cancelled the night before and got my refund without any hassle.']]
                .map(r => `
                    <div class="card">
                        <div class="str">★★★★★</div>
                        <p>${r[2]}</p>
                        <b>${r[0]}</b> <span class="mu">· ${r[1]}</span>
                    </div>`).join('')}
            </div>
            <div class="app">
                <div>
                    <h3 style="margin:0">Get the H Coach app</h3>
                    <span style="opacity:.85">Book faster and keep every ticket in your pocket.</span>
                </div>
                <div class="fr">
                    <button class="btn" onclick="toast('App Store link (demo)')">App Store</button>
                    <button class="btn s" onclick="toast('Google Play link (demo)')">Google Play</button>
                </div>
            </div>
        </div>
        <footer>
            <div class="w fc">
                <div>
                    <b style="font-size:20px">H Coach</b>
                    Intercity coach tickets across the US.<br>Demo project with sample data only.
                </div>
                <div><b>Company</b><a>About</a><a>Careers</a><a>Press</a></div>
                <div><b>Support</b><a>Help center</a><a>Contact us</a><a>Refunds</a></div>
                <div><b>Legal</b><a>Terms</a><a>Privacy</a><a>Cookies</a></div>
            </div>
        </footer>`,

    search: () => {
        const L = S.results.filter(t => S.type == 'All' || cap(t.coachType) == S.type);
        L.sort((a, b) => S.sort == 'Cheapest' ? a.price - b.price
            : S.sort == 'Earliest' ? a.departureTime.localeCompare(b.departureTime)
            : b.rating - a.rating);

        const empty = S.results.length
            ? `<b>No ${S.type} trips on this route</b><p class="mu">Try another bus type.</p>`
            : `<b>No trips available on ${fd(S.q.date)}</b><p class="mu">Try another date or route.</p>`;

        return `
            <div class="top">
                <div class="w">
                    <div>
                        <b>${esc(S.q.from)} to ${esc(S.q.to)}</b>
                        <div class="mu">${fd(S.q.date)} · ${plural(S.q.pax, 'passenger')}</div>
                    </div>
                    <button class="btn s" onclick="go('home')">Edit search</button>
                </div>
            </div>
            <div class="w">
                <div class="chips">
                    ${['Cheapest', 'Earliest', 'Top rated'].map(x => `<span class="chip ${S.sort == x ? 'on' : ''}" onclick="S.sort='${x}';render()">${x}</span>`).join('')}
                    <span style="flex:1"></span>
                    ${['All', 'Standard', 'Premium', 'Sleeper'].map(x => `<span class="chip ${S.type == x ? 'on' : ''}" onclick="S.type='${x}';render()">${x}</span>`).join('')}
                </div>
                ${trafficLine(S.traffic)}
                ${L.length ? L.map(t => `
                    <div class="card trip">
                        <div class="op">
                            <div class="dot" style="background:${esc(t.operatorColor)}">${esc(t.operatorName[0])}</div>
                            <div>
                                <b>${esc(t.operatorName)}</b>
                                <div class="mu">★ ${t.rating.toFixed(1)} · <span class="tag">${cap(t.coachType)}</span></div>
                            </div>
                        </div>
                        <div>
                            <div class="tm">
                                <b>${tm(t.departureTime)}</b>
                                <div class="ln">${dur(t.durationMinutes)}</div>
                                <b>${tm(t.arrivalTime)}</b>
                            </div>
                            <div class="mu" style="margin-top:6px">${esc(t.originStation)} → ${esc(t.destinationStation)} · Wi-Fi · Power outlets</div>
                            <div class="${t.seatsLeft < 8 ? 'low' : 'mu'}">${t.seatsLeft} seats left</div>
                        </div>
                        <div>
                            <div class="price">${m(t.price)}</div>
                            <button class="btn" ${t.seatsLeft < S.q.pax ? 'disabled' : ''} onclick="choose(${t.id})">${t.seatsLeft < S.q.pax ? 'Not enough seats' : 'Select Seats'}</button>
                        </div>
                    </div>`).join('')
                : `<div class="card" style="text-align:center">${empty}</div>`}
            </div>`;
    },

    seats: () => {
        const t = S.trip, sm = S.seatMap;
        const cols = [...sm.columns.slice(0, 2), '', ...sm.columns.slice(2)];
        let h = '';
        for (let r = 1; r <= sm.rows; r++) {
            h += `<div class="row"><i>${r}</i>`;
            cols.forEach(k => {
                if (!k) {
                    h += '<span class="ai"></span>';
                    return;
                }
                const id = r + k, bk = sm.bookedSeats.includes(id), sl = S.seats.includes(id);
                h += `<button class="seat ${bk ? 'bk' : sl ? 'sel' : r <= sm.legroomRows ? 'lg' : ''}" ${bk ? 'disabled' : ''} onclick="seat('${id}')" aria-label="Seat ${id}">${k}</button>`;
            });
            h += '</div>';
        }
        const need = S.q.pax - S.seats.length;

        return `
            ${steps(0)}
            <div class="w two">
                <div class="card">
                    <h3>Choose your seats</h3>
                    <div class="bus">${h}</div>
                    <div class="lgd">
                        <span style="--c:#9ec5ea">Available</span>
                        <span style="--c:#FF7A1A">Selected</span>
                        <span style="--c:#c9d1db">Booked</span>
                        <span style="--c:#22A06B">Extra legroom (+${m(sm.legroomSurcharge)})</span>
                    </div>
                </div>
                <div class="card sum">
                    <h3>${esc(t.origin)} to ${esc(t.destination)}</h3>
                    <div class="mu">${esc(t.operatorName)} · ${fd(t.departureTime)}<br>${tm(t.departureTime)} → ${tm(t.arrivalTime)}</div>
                    <hr style="border:0;border-top:1px solid var(--b);margin:12px 0">
                    <div class="r"><span>Seats</span><b>${S.seats.join(', ') || 'None yet'}</b></div>
                    <div class="r"><span>Pick-up</span><span>${esc(t.originStation)}</span></div>
                    <div class="r tot"><span>Total</span><span>${m(S.quote ? S.quote.total : 0)}</span></div>
                    <button class="btn" style="width:100%;margin-top:14px" ${need == 0 && S.quote ? '' : 'disabled'} onclick="go('pay')">
                        ${need == 0 ? 'Continue' : `Select ${need} more seat${need > 1 ? 's' : ''}`}
                    </button>
                </div>
            </div>`;
    },

    pay: () => `
        ${steps(2)}
        <div class="w two">
            <div>
                <div class="card">
                    <h3>Contact details</h3>
                    <div class="fg">
                        <input id="nm" placeholder="Full name" autocomplete="name" maxlength="100">
                        <input id="ph" placeholder="+1 (555) 123-4567" autocomplete="tel" maxlength="30">
                        <input id="em" placeholder="Email address" autocomplete="email" maxlength="150">
                    </div>
                </div>
                <div class="card">
                    <h3>Promo code</h3>
                    <div class="fr">
                        <input id="pc" placeholder="Try FIRSTRIDE" value="${esc(S.promoCode)}">
                        <button class="btn s" onclick="promo()">Apply</button>
                    </div>
                    <small id="pm"></small>
                </div>
                <div class="card">
                    <h3>Payment method</h3>
                    <div class="pm">
                        ${[['card', 'Credit / Debit'], ['apple', 'Apple Pay'], ['google', 'Google Pay'], ['paypal', 'PayPal']].map(p => `
                            <label class="opt ${S.pay == p[0] ? 'on' : ''}" data-v="${p[0]}">
                                <input type="radio" name="pay" ${S.pay == p[0] ? 'checked' : ''} onchange="setPay('${p[0]}')">${p[1]}
                            </label>`).join('')}
                    </div>
                    <div class="fg" id="cf" style="display:${S.pay == 'card' ? 'grid' : 'none'}">
                        <input placeholder="Card number" inputmode="numeric" autocomplete="off">
                        <input placeholder="MM / YY" autocomplete="off">
                        <input placeholder="CVC" inputmode="numeric" autocomplete="off">
                    </div>
                </div>
            </div>
            <div class="card sum">
                <h3>Price summary</h3>
                <div id="sum"></div>
                <label style="display:flex;gap:8px;margin:14px 0;font-size:14px">
                    <input id="tc" type="checkbox" style="width:auto;margin:3px 0 0">I accept the terms and cancellation policy
                </label>
                <button id="pb" class="btn" style="width:100%" onclick="payNow()">Pay Now</button>
                <div class="mu" style="font-size:12px;margin-top:10px;text-align:center">Secure payment · This is a demo, no card is charged</div>
            </div>
        </div>`,

    done: () => {
        const b = S.last;
        return `
            ${steps(3)}
            <div class="w">
                <svg class="ok" viewBox="0 0 76 76">
                    <circle cx="38" cy="38" r="38"/>
                    <path d="M22 39l11 11 21-23"/>
                </svg>
                <h2 style="text-align:center;margin:16px 0 0">You're booked!</h2>
                <p class="mu" style="text-align:center">Keep your booking code and ${esc(b.email)} to find this trip later</p>
                <div class="card tk">
                    <div class="h">
                        <b>H Coach e-ticket</b><b>${esc(b.bookingCode)}</b>
                    </div>
                    <div class="bd">
                        <div>
                            <div class="mu">Route</div>
                            <b>${esc(b.origin)} → ${esc(b.destination)}</b>
                            <div class="mu" style="margin-top:10px">Date and time</div>
                            <b>${fd(b.departureTime)} · ${tm(b.departureTime)}</b>
                            <div class="mu" style="margin-top:10px">Seats · Operator</div>
                            <b>${b.seats.join(', ')} · ${esc(b.operatorName)}</b>
                        </div>
                        ${qr(b.bookingCode)}
                    </div>
                    <div class="ft">
                        <button class="btn" onclick="print()">Download</button>
                        <button class="btn s" onclick="toast('Added to wallet (demo)')">Add to Wallet</button>
                        <button class="btn s" onclick="toast('Link copied (demo)')">Share</button>
                        <button class="btn s" onclick="openTrips('Upcoming')">Manage Booking</button>
                    </div>
                </div>
            </div>`;
    },

    trips: () => {
        const L = S.B.filter(b => b.status == S.tab);
        return `
            <div class="w">
                <h2 style="margin-top:32px">My Trips</h2>
                <div class="chips">
                    ${['Upcoming', 'Completed', 'Cancelled'].map(x => `<span class="chip ${S.tab == x ? 'on' : ''}" onclick="S.tab='${x}';render()">${x}</span>`).join('')}
                </div>
                <div class="two">
                    <div>
                        ${L.length ? L.map(b => `
                            <div class="card">
                                <div style="display:flex;justify-content:space-between;gap:10px">
                                    <b>${esc(b.origin)} to ${esc(b.destination)}</b>
                                    <span class="st ${b.status}">${b.status}</span>
                                </div>
                                <div class="mu">${fd(b.departureTime)} · ${tm(b.departureTime)} · Seats ${b.seats.join(', ')} · ${esc(b.operatorName)}</div>
                                <div style="display:flex;justify-content:space-between;align-items:center;margin-top:12px">
                                    <span>Booking ${esc(b.bookingCode)} · <b>${m(b.total)}</b></span>${b.status == 'Upcoming' ? `<button class="btn s" onclick="cancelAsk('${esc(b.bookingCode)}')">Cancel trip</button>` : ''}
                                </div>
                            </div>`).join('')
                        : `<div class="card" style="text-align:center">
                                <b>No ${S.tab.toLowerCase()} trips</b>
                                <p class="mu">Book a trip and it will show up here.</p>
                                <button class="btn" style="margin-top:10px" onclick="go('home')">Search trips</button>
                           </div>`}
                    </div>
                    <div class="card sum">
                        <h3>Find my booking</h3>
                        <label class="f">Booking code</label>
                        <input id="lc" placeholder="HC-48213">
                        <label class="f" style="margin-top:10px">Email</label>
                        <input id="le" placeholder="sam@example.com">
                        <button class="btn" style="width:100%;margin-top:14px" onclick="find()">Find booking</button>
                    </div>
                </div>
            </div>`;
    }
};

// ---------- Start ----------

(async function start() {
    render();
    try {
        [S.cities, S.popular] = await Promise.all([api('/cities'), api('/routes/popular')]);
        if (S.popular.length) {
            S.q.from = S.popular[0].origin;
            S.q.to = S.popular[0].destination;
        }
        if (S.scr == 'home') render();
    } catch (e) {
        toast(e.message);
    }
})();
