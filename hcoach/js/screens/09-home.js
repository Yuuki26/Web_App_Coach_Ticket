/* =========================================================================
 * 9. SCREEN 1: HOME
 * ========================================================================= */
function renderHomeScreen() {
  const features = [
    { ic: 'ticket', title: 'Instant e-ticket', desc: 'Book and receive your QR ticket in seconds. No printing needed — just show your phone.', color: '#FF7A1A' },
    { ic: 'shield', title: 'Verified operators', desc: 'Every Vietnamese coach partner is vetted for safety, licensing, and real rider reviews.', color: '#0F4C81' },
    { ic: 'clock', title: 'Free cancellation', desc: 'Cancel up to 24 hours before departure for a full refund, no questions asked.', color: '#059669' },
    { ic: 'headphones', title: '24/7 support', desc: 'Our bilingual support team is online around the clock for every step of your trip.', color: '#6366F1' }
  ];

  const steps = [
    { ic: 'search', title: 'Search your trip', desc: 'Enter your Vietnam route across 63 provinces, departure date, and passenger count.' },
    { ic: 'users', title: 'Choose your seat', desc: 'Pick your preferred berth or recliner on the interactive lower or upper deck seat map.' },
    { ic: 'checkCircle', title: 'Pay and go', desc: 'Pay securely via Card, MoMo, ZaloPay, VietQR, or Apple/Google Pay for an instant QR ticket.' }
  ];

  return `
    <!-- Hero -->
    <section class="hero">
      <div class="container hero-grid">
        <div>
          <div class="hero-pill">
            <span class="pulse-dot"></span>
            <span>15,000+ daily trips across Vietnam</span>
          </div>
          <h1 class="hero-title">
            Book your Vietnam coach<br/>ticket in <span style="color:var(--orange);">60 seconds</span>
          </h1>
          <p class="hero-desc">
            Fast, affordable intercity travel across all 63 provinces on verified, premium Vietnamese coach & limousine operators. Instant QR e-tickets, flexible cancellation.
          </p>
          <div class="hero-stats">
            <div>
              <div class="hero-stat-val">63</div>
              <div class="hero-stat-lbl">Provinces</div>
            </div>
            <div class="hero-stat-div"></div>
            <div>
              <div class="hero-stat-val">4.8★</div>
              <div class="hero-stat-lbl">App rating</div>
            </div>
            <div class="hero-stat-div"></div>
            <div>
              <div class="hero-stat-val">2M+</div>
              <div class="hero-stat-lbl">Happy riders</div>
            </div>
          </div>
        </div>
        <div class="hero-art">${renderCoachIllustration()}</div>
      </div>
    </section>

    <!-- Floating Search Card -->
    <div class="container search-float-wrap">
      <div class="search-card">
        <div class="search-row">
          <div class="search-cities">
            ${renderCityPicker('from', 'From')}
            <button type="button" class="swap-btn" title="Swap cities" onclick="swapCities()">${icon('swap', 16)}</button>
            ${renderCityPicker('to', 'To')}
          </div>

          <div class="search-meta-grid">
            <div>
              <label class="field-label-muted">Departure</label>
              <div class="date-input-box">
                <span class="text-navy">${icon('calendar', 14)}</span>
                <input type="date" value="${state.query.date}" onchange="state.query.date=this.value; saveState();"/>
              </div>
            </div>
            <div>
              <label class="field-label-muted">Return (optional)</label>
              <div class="date-input-box">
                <span class="text-muted">${icon('calendar', 14)}</span>
                <input type="date" value="${state.query.returnDate}" onchange="state.query.returnDate=this.value; saveState();" style="color:var(--muted);"/>
              </div>
            </div>
            <div class="pax-col">
              <label class="field-label-muted">Passengers</label>
              <div class="pax-box">
                <span class="text-navy">${icon('users', 14)}</span>
                <div class="flex items-center gap-2">
                  <button type="button" class="pax-step-btn" onclick="changePassengers(-1)">${icon('minus', 11)}</button>
                  <strong style="min-width:16px;text-align:center;font-size:0.9rem;">${state.query.passengers}</strong>
                  <button type="button" class="pax-step-btn" onclick="changePassengers(1)">${icon('plus', 11)}</button>
                </div>
              </div>
            </div>
          </div>

          <button type="button" class="btn btn-primary btn-lg" style="height:44px;" onclick="executeSearch()">
            ${icon('search', 16)} Search Trips
          </button>
        </div>
      </div>
    </div>

    <!-- Popular Routes -->
    <section id="popular-routes" class="section">
      <div class="container">
        <div class="section-hdr">
          <div>
            <h2 class="section-title">Popular routes in Vietnam</h2>
            <p class="section-sub">Thousands of daily departures on Vietnam’s most-traveled corridors</p>
          </div>
          <button type="button" class="btn btn-outline btn-sm" onclick="executeSearch()">
            View all ${icon('chevronRight', 14)}
          </button>
        </div>

        <div class="grid-3">
          ${POPULAR_ROUTES.map(r => {
            const cached = routeImageCache[r.slug];
            const isLoaded = routeImageLoaded[r.slug];
            return `
              <button type="button" class="route-card" onclick="searchPopularRoute('${escapeHtml(r.from)}', '${escapeHtml(r.to)}')">
                <div class="route-bg-svg">${renderDestinationSvg(r.slug)}</div>
                <div id="route-photo-${r.slug}" class="route-bg-img-wrap">
                  ${cached ? `<img src="${cached}" alt="${escapeHtml(r.from)} to ${escapeHtml(r.to)}" referrerpolicy="no-referrer" decoding="async" class="route-bg-img ${isLoaded ? 'loaded' : ''}" onload="handleRouteImgLoad(this, '${r.slug}')" onerror="handleRouteImgError(this, '${r.slug}', 0)"/>` : ''}
                </div>
                <div class="route-overlay"></div>
                ${r.tag ? `<div class="route-tag"><span class="badge badge-orange">${r.tag}</span></div>` : ''}
                <div class="route-info">
                  <div style="min-width:0;">
                    <div class="route-name truncate">${escapeHtml(r.from)} → ${escapeHtml(r.to)}</div>
                    <div class="route-dur">${icon('clock', 13)} ${r.duration}</div>
                  </div>
                  <div style="flex-shrink:0;">
                    <div class="route-price-lbl">From</div>
                    <div class="route-price">${formatVND(r.fromPrice)}</div>
                  </div>
                </div>
              </button>
            `;
          }).join('')}
        </div>
      </div>
    </section>

    <!-- Why travelers choose H Coach -->
    <section class="section section-white">
      <div class="container">
        <div style="text-align:center;margin-bottom:36px;">
          <h2 class="section-title">Why travelers choose H Coach</h2>
          <p class="section-sub">We make intercity travel across Vietnam feel effortless</p>
        </div>
        <div class="grid-4">
          ${features.map(f => `
            <div class="feature-card">
              <div class="feature-icon" style="background:${f.color}18;color:${f.color};">${icon(f.ic, 22)}</div>
              <h3 class="font-bold mb-2" style="font-size:1rem;">${f.title}</h3>
              <p class="text-sm text-muted">${f.desc}</p>
            </div>
          `).join('')}
        </div>
      </div>
    </section>

    <!-- Current Deals -->
    <section id="deals-section" class="section">
      <div class="container">
        <div class="section-hdr">
          <div>
            <h2 class="section-title">Current deals</h2>
            <p class="section-sub">Click any promo code to copy and apply at checkout</p>
          </div>
          <div class="deals-live-pill">
            <span class="deals-live-dot"></span>
            <span>Flash offers live · Ends soon</span>
          </div>
        </div>
        <div class="grid-3">
          ${DEAL_CARDS.map(p => `
            <div class="promo-card" style="background:${p.bg};">
              <div class="promo-circle-1"></div>
              <div class="promo-circle-2"></div>
              <div style="position:relative;">
                <div class="deal-top-row">
                  <span style="opacity:0.9;display:inline-flex;">${icon('tag', 18)}</span>
                  <div class="deal-timer-pill">
                    <span class="deal-timer-dot"></span>
                    ${icon('clock', 12)}
                    <span class="deal-timer" data-deal-id="${p.id}">Ends in ${formatDealCountdown(p)}</span>
                  </div>
                </div>
                <h3 class="font-extrabold mb-2" style="font-size:1.08rem;">${p.title}</h3>
                <p class="text-sm mb-4" style="color:rgba(255,255,255,0.86);">${p.desc}</p>
                <div class="deal-progress-wrap">
                  <div class="deal-progress-meta">
                    <span>${p.claimedPct}% claimed</span>
                    <span>Hurry!</span>
                  </div>
                  <div class="deal-progress-bar">
                    <div class="deal-progress-fill" style="width:${p.claimedPct}%;"></div>
                  </div>
                </div>
                <div class="flex items-center justify-between gap-2">
                  <button type="button" class="promo-code-btn" onclick="copyPromo('${p.code}')">
                    ${p.code} ${icon('copy', 13)}
                  </button>
                  <span class="text-xs font-bold" style="color:rgba(255,255,255,0.9);">${p.endText}</span>
                </div>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    </section>

    <!-- How it works -->
    <section class="section section-white">
      <div class="container">
        <div style="text-align:center;margin-bottom:36px;">
          <h2 class="section-title">How it works</h2>
          <p class="section-sub">Three simple steps from search to boarding</p>
        </div>
        <div class="grid-3">
          ${steps.map((s, idx) => `
            <div class="step-box">
              <div class="step-icon-wrap">
                ${icon(s.ic, 28)}
                <span class="step-num">${idx + 1}</span>
              </div>
              <h3 class="font-bold mb-2">${s.title}</h3>
              <p class="text-sm text-muted" style="max-width:290px;">${s.desc}</p>
            </div>
          `).join('')}
        </div>
      </div>
    </section>

    <!-- Rider Reviews (Continuous Auto-Scrolling Marquee) -->
    <section class="section">
      <div class="container">
        <div class="section-hdr">
          <div>
            <h2 class="section-title">What riders say</h2>
            <div class="mt-2 flex items-center gap-2">
              ${renderStars(4.8)}
              <span class="text-sm text-muted">Based on 18,400+ verified Vietnam trip reviews</span>
            </div>
          </div>
        </div>
        <div class="testimonial-marquee" aria-label="Verified rider reviews">
          <div class="testimonial-track">
            ${[...TESTIMONIALS, ...TESTIMONIALS].map(t => `
              <article class="card p-6 testimonial-item">
                <div>
                  ${renderStars(t.rating)}
                  <p class="text-sm mt-3 mb-4" style="line-height:1.6;">“${t.text}”</p>
                </div>
                <div class="flex items-center gap-3">
                  <div style="width:38px;height:38px;border-radius:50%;background:${t.bg};color:#fff;font-size:0.78rem;font-weight:800;display:flex;align-items:center;justify-content:center;flex-shrink:0;">${t.avatar}</div>
                  <div>
                    <div class="text-sm font-bold">${t.name}</div>
                    <div class="text-xs text-muted">${t.location}</div>
                  </div>
                </div>
              </article>
            `).join('')}
          </div>
        </div>
      </div>
    </section>

    <!-- Operator Partners Bar -->
    <section class="partners-strip">
      <div class="container">
        <p class="text-xs font-bold text-muted" style="text-align:center;text-transform:uppercase;letter-spacing:0.1em;margin-bottom:20px;">Our verified Vietnam operator partners</p>
        <div class="partners-list">
          ${PARTNERS.map(p => `
            <div class="partner-pill">
              <span class="partner-badge" style="background:${p.color};">${p.initials}</span>
              <span>${p.name}</span>
            </div>
          `).join('')}
        </div>
      </div>
    </section>

    <!-- Mobile App Promo Banner -->
    <section class="section">
      <div class="container">
        <div class="app-banner">
          <div class="promo-circle-1" style="width:180px;height:180px;right:-40px;top:-40px;"></div>
          <div style="position:relative;max-width:560px;">
            <h2 style="font-size:1.75rem;font-weight:800;line-height:1.25;margin-bottom:10px;">Travel smarter with the H Coach app</h2>
            <p class="text-sm mb-4" style="color:rgba(255,255,255,0.78);line-height:1.6;">
              Manage bookings offline, track your coach live along Vietnam's highways, and unlock exclusive app-only fares on iOS and Android.
            </p>
            <div class="flex gap-3" style="flex-wrap:wrap;">
              <button type="button" class="app-store-btn" style="background:#fff;color:var(--navy);" onclick="showToast('H Coach for iOS — App Store link copied!', 'info')">
                ${icon('smartphone', 16)} App Store
              </button>
              <button type="button" class="app-store-btn" style="background:rgba(255,255,255,0.14);color:#fff;border:1px solid rgba(255,255,255,0.28);" onclick="showToast('H Coach for Android — Google Play link copied!', 'info')">
                ${icon('smartphone', 16)} Google Play
              </button>
            </div>
          </div>
          <div style="position:relative;background:rgba(255,255,255,0.12);border:1px solid rgba(255,255,255,0.22);border-radius:18px;padding:18px 20px;text-align:center;">
            <div style="background:#fff;padding:8px;border-radius:12px;display:inline-block;">
              ${renderQrSvg('HCOACH-VN-APP', 92)}
            </div>
            <div class="text-xs mt-2" style="color:rgba(255,255,255,0.75);font-weight:600;">Scan to download</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer id="footer-section" class="footer">
      <div class="container">
        <div class="footer-grid">
          <div class="footer-col-full">
            <div class="flex items-center gap-2 mb-3">
              <span class="brand-badge">H</span>
              <span style="font-size:1.15rem;font-weight:800;">Coach</span>
            </div>
            <p class="text-sm" style="color:rgba(255,255,255,0.6);max-width:260px;">
              Vietnam's friendliest intercity coach booking platform.
            </p>
          </div>
          <div>
            <h4 class="font-bold text-sm mb-3">Company</h4>
            <ul class="footer-links">
              <li><a href="javascript:void(0)" onclick="showToast('H Coach connects all 63 provinces across Vietnam.', 'info')">About us</a></li>
              <li><a href="javascript:void(0)">Careers</a></li>
              <li><a href="javascript:void(0)">Press</a></li>
              <li><a href="javascript:void(0)">Travel blog</a></li>
            </ul>
          </div>
          <div>
            <h4 class="font-bold text-sm mb-3">Support</h4>
            <ul class="footer-links">
              <li><a href="javascript:void(0)" onclick="showToast('24/7 Vietnam Hotline: 1900 6868', 'info')">24/7 Hotline: 1900 6868</a></li>
              <li><a href="javascript:void(0)">Help center</a></li>
              <li><a href="javascript:void(0)">Safety standards</a></li>
              <li><a href="javascript:void(0)">Accessibility</a></li>
            </ul>
          </div>
          <div>
            <h4 class="font-bold text-sm mb-3">Legal</h4>
            <ul class="footer-links">
              <li><a href="javascript:void(0)">Privacy policy</a></li>
              <li><a href="javascript:void(0)">Terms of service</a></li>
              <li><a href="javascript:void(0)">Refund policy</a></li>
            </ul>
          </div>
        </div>
        <div style="border-top:1px solid rgba(255,255,255,0.1);padding-top:20px;display:flex;flex-wrap:wrap;justify-content:space-between;gap:10px;font-size:0.82rem;color:rgba(255,255,255,0.45);">
          <span>© 2026 H Coach Vietnam. All rights reserved.</span>
          <span class="flex items-center gap-2">${icon('globe', 14)} English (VN) — VND (₫)</span>
        </div>
      </div>
    </footer>
  `;
}
