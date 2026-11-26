/* =========================================================================
 * 7. HEADER, STEPPER & AUTH MODAL (MULTI-PAGE LINKS)
 * ========================================================================= */
function renderHeader() {
  const isHome = state.screen === 'home';
  return `
    <header class="header ${isHome ? 'header-navy' : 'header-white'}">
      <div class="container header-inner">
        <a href="index.html" class="brand-btn" onclick="saveState()">
          <span class="brand-badge">H</span>
          <span class="brand-text">Coach</span>
        </a>

        <nav class="nav-links">
          <a href="search.html" class="nav-link" onclick="executeSearch(event)">${icon('bus', 16)} Book Tickets</a>
          <a href="index.html#popular-routes" class="nav-link" onclick="scrollToEl('popular-routes', event)">${icon('mapPin', 16)} Routes</a>
          <a href="index.html#deals-section" class="nav-link" onclick="scrollToEl('deals-section', event)">${icon('tag', 16)} Deals</a>
          <a href="index.html#footer-section" class="nav-link" onclick="scrollToEl('footer-section', event)">${icon('headphones', 16)} Help</a>
        </nav>

        <div class="nav-actions">
          <a href="trips.html" class="nav-link" onclick="saveState()">
            ${icon('ticket', 16)} My Trips
          </a>
          ${state.authUser ? `
            <button type="button" class="btn btn-sm ${isHome ? 'btn-primary' : 'btn-outline'}" onclick="logoutUser()" title="Click to log out">
              ${icon('user', 16)} ${escapeHtml(state.authUser.name)}
            </button>
          ` : `
            <button type="button" class="btn btn-sm ${isHome ? 'btn-primary' : 'btn-navy'}" onclick="openAuthModal()">
              ${icon('user', 16)} Log in / Sign up
            </button>
          `}
        </div>

        <button type="button" class="mobile-menu-btn" aria-label="Menu" onclick="state.mobileMenuOpen=!state.mobileMenuOpen; render();">
          ${icon(state.mobileMenuOpen ? 'x' : 'menu', 22)}
        </button>
      </div>

      ${state.mobileMenuOpen ? `
        <div class="mobile-drawer">
          <a href="search.html" class="mobile-drawer-link" onclick="state.mobileMenuOpen=false; executeSearch(event);">${icon('bus', 16)} Book Tickets</a>
          <a href="index.html#popular-routes" class="mobile-drawer-link" onclick="state.mobileMenuOpen=false; scrollToEl('popular-routes', event);">${icon('mapPin', 16)} Routes</a>
          <a href="index.html#deals-section" class="mobile-drawer-link" onclick="state.mobileMenuOpen=false; scrollToEl('deals-section', event);">${icon('tag', 16)} Deals</a>
          <a href="index.html#footer-section" class="mobile-drawer-link" onclick="state.mobileMenuOpen=false; scrollToEl('footer-section', event);">${icon('headphones', 16)} Help</a>
          <a href="trips.html" class="mobile-drawer-link" onclick="state.mobileMenuOpen=false; saveState();">${icon('ticket', 16)} My Trips</a>
          <div class="mt-2">
            ${state.authUser ? `
              <button type="button" class="btn btn-outline btn-full" onclick="state.mobileMenuOpen=false; logoutUser();">${icon('user', 16)} Log out (${escapeHtml(state.authUser.name)})</button>
            ` : `
              <button type="button" class="btn btn-primary btn-full" onclick="state.mobileMenuOpen=false; openAuthModal();">${icon('user', 16)} Log in / Sign up</button>
            `}
          </div>
        </div>
      ` : ''}
    </header>
  `;
}

function renderStepper(activeStep) {
  const steps = ['Seats', 'Details', 'Payment', 'Done'];
  return `
    <div class="stepper">
      ${steps.map((lbl, idx) => `
        <div class="step-item ${idx < steps.length - 1 ? 'grow' : ''}">
          <div class="step-circle ${idx < activeStep ? 'done' : idx === activeStep ? 'active' : ''}">
            ${idx < activeStep ? icon('check', 14) : idx + 1}
          </div>
          <span class="step-label ${idx < activeStep ? 'done' : idx === activeStep ? 'active' : ''}">${lbl}</span>
          ${idx < steps.length - 1 ? `<div class="step-line ${idx < activeStep ? 'done' : ''}"></div>` : ''}
        </div>
      `).join('')}
    </div>
  `;
}

function renderAuthModal() {
  if (!state.authModalOpen) return '';
  const isSignup = state.authTab === 'signup';
  return `
    <div class="modal-backdrop" onclick="closeAuthModal()">
      <div class="modal-box" onclick="event.stopPropagation()">
        <div class="flex items-center justify-between mb-4">
          <h2 style="font-size:1.2rem;font-weight:800;">${isSignup ? 'Create account' : 'Welcome back'}</h2>
          <button type="button" onclick="closeAuthModal()" style="width:32px;height:32px;border-radius:50%;background:var(--bg);color:var(--muted);display:flex;align-items:center;justify-content:center;">${icon('x', 16)}</button>
        </div>

        <div class="deck-tabs mb-4" style="display:flex;width:100%;">
          <button type="button" class="deck-tab ${!isSignup ? 'active' : ''}" style="flex:1;" onclick="state.authTab='login'; render();">Log in</button>
          <button type="button" class="deck-tab ${isSignup ? 'active' : ''}" style="flex:1;" onclick="state.authTab='signup'; render();">Sign up</button>
        </div>

        <form onsubmit="submitAuth(event)" class="flex flex-col gap-3">
          ${isSignup ? `
            <div class="input-wrap">
              <span class="input-icon">${icon('user', 15)}</span>
              <input class="input input-with-icon" type="text" required placeholder="Full name (e.g. Minh Anh Nguyen)" value="${escapeHtml(state.authForm.name)}" oninput="state.authForm.name=this.value"/>
            </div>
          ` : ''}
          <div class="input-wrap">
            <span class="input-icon">${icon('mail', 15)}</span>
            <input class="input input-with-icon" type="email" required placeholder="Email address" value="${escapeHtml(state.authForm.email)}" oninput="state.authForm.email=this.value"/>
          </div>
          <div class="input-wrap">
            <span class="input-icon">${icon('lock', 15)}</span>
            <input class="input input-with-icon" type="password" required placeholder="Password" value="${escapeHtml(state.authForm.password)}" oninput="state.authForm.password=this.value"/>
          </div>
          <button type="submit" class="btn btn-primary btn-lg btn-full mt-2">
            ${isSignup ? 'Create account' : 'Log in'}
          </button>
        </form>
      </div>
    </div>
  `;
}
