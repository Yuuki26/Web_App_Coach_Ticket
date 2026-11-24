/* =========================================================================
 * 4. SVG ICONS, REDESIGNED LUXURY COACH & VIETNAM LANDMARK ILLUSTRATIONS
 * ========================================================================= */
function icon(name, size = 16) {
  const paths = {
    arrowLeft: '<path d="m12 19-7-7 7-7"/><path d="M19 12H5"/>',
    arrowRight: '<path d="M5 12h14"/><path d="m12 5 7 7-7 7"/>',
    bus: '<path d="M8 6v6"/><path d="M15 6v6"/><path d="M2 12h19.6"/><path d="M18 18h3s.5-1.7.8-2.8c.1-.4.2-.8.2-1.2 0-.4-.1-.8-.2-1.2l-1.4-5C20.1 6.8 19.1 6 18 6H4a2 2 0 0 0-2 2v10h3"/><circle cx="7" cy="18" r="2"/><path d="M9 18h5"/><circle cx="16" cy="18" r="2"/>',
    swap: '<path d="M8 3 4 7l4 4"/><path d="M4 7h16"/><path d="m16 21 4-4-4-4"/><path d="M20 17H4"/>',
    calendar: '<path d="M8 2v4"/><path d="M16 2v4"/><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M3 10h18"/>',
    check: '<path d="M20 6 9 17l-5-5"/>',
    checkCircle: '<path d="M21.8 10A10 10 0 1 1 17 3.3"/><path d="m9 11 3 3L22 4"/>',
    chevronDown: '<path d="m6 9 6 6 6-6"/>',
    chevronUp: '<path d="m18 15-6-6-6 6"/>',
    chevronRight: '<path d="m9 18 6-6-6-6"/>',
    alertCircle: '<circle cx="12" cy="12" r="10"/><line x1="12" x2="12" y1="8" y2="12"/><line x1="12" x2="12.01" y1="16" y2="16"/>',
    clock: '<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>',
    coffee: '<path d="M10 2v2"/><path d="M14 2v2"/><path d="M16 8a1 1 0 0 1 1 1v8a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1h14a4 4 0 1 1 0 8h-1"/><path d="M6 2v2"/>',
    creditCard: '<rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/>',
    download: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" x2="12" y1="15" y2="3"/>',
    filter: '<polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>',
    sliders: '<line x1="21" x2="14" y1="4" y2="4"/><line x1="10" x2="3" y1="4" y2="4"/><line x1="21" x2="12" y1="12" y2="12"/><line x1="8" x2="3" y1="12" y2="12"/><line x1="21" x2="16" y1="20" y2="20"/><line x1="12" x2="3" y1="20" y2="20"/><line x1="14" x2="14" y1="2" y2="6"/><line x1="8" x2="8" y1="10" y2="14"/><line x1="16" x2="16" y1="18" y2="22"/>',
    globe: '<circle cx="12" cy="12" r="10"/><path d="M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"/><path d="M2 12h20"/>',
    headphones: '<path d="M3 14h3a2 2 0 0 1 2 2v3a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a9 9 0 0 1 18 0v7a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3"/>',
    lock: '<rect width="18" height="11" x="3" y="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>',
    mail: '<rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>',
    mapPin: '<path d="M20 10c0 5-5.5 10.2-7.4 11.8a1 1 0 0 1-1.2 0C9.5 20.2 4 15 4 10a8 8 0 0 1 16 0"/><circle cx="12" cy="10" r="3"/>',
    menu: '<line x1="4" x2="20" y1="12" y2="12"/><line x1="4" x2="20" y1="6" y2="6"/><line x1="4" x2="20" y1="18" y2="18"/>',
    minus: '<path d="M5 12h14"/>',
    plus: '<path d="M5 12h14"/><path d="M12 5v14"/>',
    phone: '<path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.8 19.8 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.8 19.8 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.13.96.36 1.9.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.91.34 1.85.57 2.81.7A2 2 0 0 1 22 16.92z"/>',
    qr: '<rect width="5" height="5" x="3" y="3" rx="1"/><rect width="5" height="5" x="16" y="3" rx="1"/><rect width="5" height="5" x="3" y="16" rx="1"/><path d="M21 16h-3a2 2 0 0 0-2 2v3"/><path d="M21 21v.01"/><path d="M12 7v3a2 2 0 0 1-2 2H7"/><path d="M3 12h.01"/><path d="M12 3h.01"/><path d="M12 16v.01"/><path d="M16 12h1"/><path d="M21 12v.01"/><path d="M12 21v-1"/>',
    search: '<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>',
    share: '<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" x2="15.42" y1="13.51" y2="17.49"/><line x1="15.41" x2="8.59" y1="6.51" y2="10.49"/>',
    shield: '<path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"/>',
    smartphone: '<rect width="14" height="20" x="5" y="2" rx="2"/><path d="M12 18h.01"/>',
    tag: '<path d="M12.59 2.59A2 2 0 0 0 11.17 2H4a2 2 0 0 0-2 2v7.17a2 2 0 0 0 .59 1.42l8.7 8.7a2.43 2.43 0 0 0 3.42 0l6.58-6.58a2.43 2.43 0 0 0 0-3.42z"/><circle cx="7.5" cy="7.5" r=".5" fill="currentColor"/>',
    ticket: '<path d="M2 9a3 3 0 0 1 0 6v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2a3 3 0 0 1 0-6V7a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2Z"/><path d="M13 5v2"/><path d="M13 17v2"/><path d="M13 11v2"/>',
    user: '<path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
    users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
    wifi: '<path d="M12 20h.01"/><path d="M2 8.82a15 15 0 0 1 20 0"/><path d="M5 12.86a10 10 0 0 1 14 0"/><path d="M8.5 16.43a5 5 0 0 1 7 0"/>',
    wind: '<path d="M12.8 19.6A2 2 0 1 0 14 16H2"/><path d="M17.5 8a2.5 2.5 0 1 1 2 4H2"/><path d="M9.8 4.4A2 2 0 1 1 11 8H2"/>',
    x: '<path d="M18 6 6 18"/><path d="m6 6 12 12"/>',
    zap: '<path d="M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z"/>',
    wallet: '<path d="M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1"/><path d="M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"/>',
    copy: '<rect width="14" height="14" x="8" y="8" rx="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/>'
  };
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${paths[name] || ''}</svg>`;
}

function renderStars(rating, count) {
  const full = Math.floor(rating);
  let s = '';
  for (let i = 1; i <= 5; i++) {
    const fill = i <= full ? '#FBBF24' : '#E2E8F0';
    s += `<svg width="13" height="13" viewBox="0 0 24 24" fill="${fill}" stroke="${fill}" stroke-width="1.5"><path d="M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.12 2.12 0 0 0 1.6 1.16l5.16.756a.53.53 0 0 1 .29.904l-3.74 3.638a2.12 2.12 0 0 0-.61 1.878l.88 5.14a.53.53 0 0 1-.77.56l-4.62-2.428a2.12 2.12 0 0 0-1.97 0l-4.62 2.428a.53.53 0 0 1-.77-.56l.88-5.14a2.12 2.12 0 0 0-.61-1.878L2.16 9.795a.53.53 0 0 1 .29-.906l5.16-.755a2.12 2.12 0 0 0 1.6-1.16z"/></svg>`;
  }
  return `
    <span style="display:inline-flex;align-items:center;gap:5px;">
      <span style="display:inline-flex;gap:2px;">${s}</span>
      <strong style="font-size:0.84rem;color:var(--ink);">${rating}</strong>
      ${count !== undefined ? `<span class="text-xs text-muted">(${count.toLocaleString('en-US')})</span>` : ''}
    </span>
  `;
}

/**
 * Redesigned sleek luxury intercity coach illustration (facing right).
 * Generous viewBox="0 0 600 320" guarantees zero clipping on wheels, headlights, or shadows.
 */
function renderCoachIllustration() {
  const renderAlloyWheel = (cx, cy) => {
    const spokes = [0, 72, 144, 216, 288].map(deg => {
      const rad = (deg - 90) * Math.PI / 180;
      const x2 = (cx + 16.5 * Math.cos(rad)).toFixed(1);
      const y2 = (cy + 16.5 * Math.sin(rad)).toFixed(1);
      const lx = (cx + 9.5 * Math.cos(rad + 0.31)).toFixed(1);
      const ly = (cy + 9.5 * Math.sin(rad + 0.31)).toFixed(1);
      return `
        <line x1="${cx}" y1="${cy}" x2="${x2}" y2="${y2}" stroke="#E2E8F0" stroke-width="3.6" stroke-linecap="round"/>
        <circle cx="${lx}" cy="${ly}" r="1.4" fill="#64748B"/>
      `;
    }).join('');

    return `
      <g>
        <!-- Outer tire -->
        <circle cx="${cx}" cy="${cy}" r="33" fill="#0F172A"/>
        <circle cx="${cx}" cy="${cy}" r="27" fill="#1E293B"/>
        <!-- Alloy rim -->
        <circle cx="${cx}" cy="${cy}" r="19" fill="#94A3B8"/>
        <circle cx="${cx}" cy="${cy}" r="16.5" fill="#334155"/>
        ${spokes}
        <!-- Center hub -->
        <circle cx="${cx}" cy="${cy}" r="6.5" fill="#CBD5E1"/>
        <circle cx="${cx}" cy="${cy}" r="3" fill="#0F4C81"/>
      </g>
    `;
  };

  const bodyPath = 'M 64 82 L 494 82 C 516 82 531 94 538 118 L 552 174 C 556 188 557 204 555 224 C 554 236 546 244 534 244 L 62 244 C 52 244 46 237 46 226 L 46 102 C 46 90 54 82 64 82 Z';

  return `
    <svg viewBox="0 0 600 320" fill="none" xmlns="http://www.w3.org/2000/svg" style="width:100%;height:auto;overflow:visible;" aria-label="H Coach Vietnam luxury intercity bus illustration">
      <defs>
        <linearGradient id="coach-body-grad" x1="0" y1="80" x2="0" y2="245" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stop-color="#FFFFFF"/>
          <stop offset="68%" stop-color="#F1F5F9"/>
          <stop offset="100%" stop-color="#DCE6F2"/>
        </linearGradient>
        <linearGradient id="coach-glass-grad" x1="0" y1="95" x2="0" y2="165" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stop-color="#0B2239"/>
          <stop offset="55%" stop-color="#153E66"/>
          <stop offset="100%" stop-color="#0D2C4D"/>
        </linearGradient>
        <linearGradient id="coach-beam" x1="548" y1="212" x2="598" y2="224" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stop-color="#FEF08A" stop-opacity="0.55"/>
          <stop offset="100%" stop-color="#FEF08A" stop-opacity="0"/>
        </linearGradient>
        <linearGradient id="coach-orange-grad" x1="46" y1="184" x2="555" y2="184" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stop-color="#FF7A1A"/>
          <stop offset="80%" stop-color="#FF8C38"/>
          <stop offset="100%" stop-color="#F97316"/>
        </linearGradient>
        <clipPath id="coach-body-clip">
          <path d="${bodyPath}"/>
        </clipPath>
      </defs>

      <!-- Soft Ground Shadow (entire wheels & shadow well inside 320px height) -->
      <ellipse cx="302" cy="280" rx="248" ry="12" fill="rgba(4, 18, 36, 0.36)"/>
      <ellipse cx="302" cy="279" rx="210" ry="6" fill="rgba(4, 18, 36, 0.22)"/>

      <!-- Soft LED Headlight Glow Cone -->
      <polygon points="548,205 596,194 596,238 548,223" fill="url(#coach-beam)"/>

      <!-- Rooftop Aerodynamic AC Unit -->
      <path d="M 192 82 L 204 66 C 208 62 214 61 222 61 L 348 61 C 362 61 374 68 384 82 Z" fill="#E2E8F0"/>
      <path d="M 205 76 L 372 76" stroke="#CBD5E1" stroke-width="2" stroke-linecap="round"/>
      <rect x="236" y="67" width="38" height="4" rx="2" fill="#94A3B8" opacity="0.65"/>
      <rect x="284" y="67" width="38" height="4" rx="2" fill="#94A3B8" opacity="0.65"/>

      <!-- Main Coach Shell -->
      <g clip-path="url(#coach-body-clip)">
        <!-- Body Base -->
        <path d="${bodyPath}" fill="url(#coach-body-grad)"/>

        <!-- Subtle Top Roofline Sheen -->
        <rect x="46" y="82" width="512" height="10" fill="rgba(255,255,255,0.75)"/>

        <!-- Lower Metallic Navy Skirt -->
        <rect x="46" y="200" width="514" height="46" fill="#0B355C"/>
        <rect x="46" y="233" width="514" height="13" fill="#072542"/>

        <!-- Dynamic Integrated Orange Accent Stripe -->
        <path d="M 46 184 L 466 184 C 492 184 516 177 556 164 L 556 180 C 516 193 492 199 466 199 L 46 199 Z" fill="url(#coach-orange-grad)"/>
        <!-- Thin Navy Pinstripe Accent -->
        <rect x="46" y="199" width="445" height="3" fill="#0F4C81"/>

        <!-- Panoramic Dark-Tinted Passenger Windows -->
        <rect x="64" y="99" width="386" height="62" rx="8" fill="url(#coach-glass-grad)"/>
        <!-- Subtle Diagonal Glass Reflections -->
        <g opacity="0.13" fill="#FFFFFF">
          <polygon points="86,99 112,99 88,161 64,161"/>
          <polygon points="184,99 202,99 178,161 160,161"/>
          <polygon points="292,99 318,99 294,161 268,161"/>
          <polygon points="396,99 414,99 390,161 372,161"/>
        </g>
        <!-- Clean Window Pillars -->
        <g fill="#0A2036">
          <rect x="138" y="99" width="5" height="62"/>
          <rect x="215" y="99" width="5" height="62"/>
          <rect x="292" y="99" width="5" height="62"/>
          <rect x="369" y="99" width="5" height="62"/>
        </g>

        <!-- Front Passenger Door (right behind the curved nose & ahead of front wheel) -->
        <rect x="464" y="99" width="32" height="130" rx="5" fill="#E2E8F0" stroke="#94A3B8" stroke-width="1.5"/>
        <rect x="468" y="103" width="24" height="58" rx="3" fill="url(#coach-glass-grad)"/>
        <polygon points="472,103 482,103 472,135 468,135" fill="#FFFFFF" opacity="0.16"/>
        <rect x="470" y="168" width="6" height="10" rx="2" fill="#475569"/>

        <!-- Aerodynamic Curved Front Windshield -->
        <path d="M 504 97 L 522 97 C 531 97 537 104 541 118 L 552 162 L 504 162 Z" fill="url(#coach-glass-grad)"/>
        <polygon points="512,99 524,99 536,160 524,160" fill="#FFFFFF" opacity="0.18"/>

        <!-- H COACH Side Branding -->
        <g transform="translate(158, 169)">
          <rect x="0" y="0" width="14" height="12" rx="3" fill="#FF7A1A"/>
          <text x="7" y="9.2" text-anchor="middle" font-size="8.5" font-weight="900" fill="#FFFFFF" font-family="Plus Jakarta Sans, Arial, sans-serif">H</text>
          <text x="20" y="9.5" font-size="11.5" font-weight="800" fill="#0F4C81" letter-spacing="2.2" font-family="Plus Jakarta Sans, Arial, sans-serif">COACH</text>
          <text x="84" y="9.2" font-size="7.5" font-weight="700" fill="#64748B" letter-spacing="1.4" font-family="Plus Jakarta Sans, Arial, sans-serif">VIETNAM EXPRESS</text>
        </g>

        <!-- Under-Floor Luggage Compartment Seams & Handles -->
        <g stroke="rgba(255,255,255,0.18)" stroke-width="1.4">
          <line x1="204" y1="203" x2="204" y2="233"/>
          <line x1="266" y1="203" x2="266" y2="233"/>
          <line x1="328" y1="203" x2="328" y2="233"/>
          <line x1="390" y1="203" x2="390" y2="233"/>
          <line x1="204" y1="233" x2="390" y2="233"/>
        </g>
        <g fill="#94A3B8" opacity="0.85">
          <rect x="230" y="216" width="11" height="3.2" rx="1.5"/>
          <rect x="292" y="216" width="11" height="3.2" rx="1.5"/>
          <rect x="354" y="216" width="11" height="3.2" rx="1.5"/>
        </g>

        <!-- Rear Red LED Taillights (Flush-mounted inside rear body) -->
        <rect x="46" y="196" width="9" height="15" rx="2" fill="#EF4444"/>
        <rect x="46" y="213" width="9" height="7" rx="1.5" fill="#F59E0B"/>
        <rect x="46" y="96" width="6" height="6" rx="1.5" fill="#EF4444" opacity="0.85"/>

        <!-- Front Glowing LED Headlight & Amber Turn Signal (Flush-mounted on front bumper) -->
        <path d="M 526 206 L 555 204 L 554 218 L 528 218 C 525 218 524 206 526 206 Z" fill="#FEFCE8"/>
        <rect x="530" y="209" width="22" height="5" rx="2.5" fill="#FFFFFF"/>
        <rect x="515" y="209" width="8" height="6" rx="2" fill="#FBBF24"/>
        <!-- Front Grille Air Intake -->
        <rect x="526" y="224" width="26" height="4" rx="2" fill="#1E3A5F"/>
      </g>

      <!-- Crisp Subtle Body Outline -->
      <path d="${bodyPath}" fill="none" stroke="rgba(255,255,255,0.22)" stroke-width="1.2"/>

      <!-- Attached Aerodynamic Side Rearview Mirror -->
      <path d="M 532 104 L 552 100 C 556 100 558 103 558 108 L 558 118" stroke="#1E293B" stroke-width="3.2" stroke-linecap="round" fill="none"/>
      <rect x="551" y="109" width="12" height="26" rx="4" fill="#1E293B"/>
      <rect x="553" y="112" width="4" height="20" rx="2" fill="#64748B" opacity="0.6"/>

      <!-- Wheel Arches (Recessed Dark Wells) -->
      <path d="M 110 245 A 40 40 0 0 1 190 245 Z" fill="#06192E"/>
      <path d="M 392 245 A 40 40 0 0 1 472 245 Z" fill="#06192E"/>

      <!-- Detailed Multi-Spoke Alloy Wheels -->
      ${renderAlloyWheel(150, 245)}
      ${renderAlloyWheel(432, 245)}
    </svg>
  `;
}

// Custom Vietnam destination fallback SVG illustrations (6 iconic destinations)
function renderDestinationSvg(slug) {
  switch (slug) {
    case 'dalat':
      return `<svg viewBox="0 0 600 400" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="g-dl-sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#1E3A5F"/><stop offset="55%" stop-color="#6B8EAE"/><stop offset="100%" stop-color="#DBEAFE"/></linearGradient>
          <linearGradient id="g-dl-water" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#4A7C9B"/><stop offset="100%" stop-color="#16324F"/></linearGradient>
        </defs>
        <rect width="600" height="400" fill="url(#g-dl-sky)"/>
        <circle cx="470" cy="85" r="34" fill="#FDE68A" opacity="0.9"/>
        <path d="M0 220 Q120 140 250 185 T520 165 L600 190 L600 265 L0 265 Z" fill="#3B5B76" opacity="0.7"/>
        <path d="M0 245 Q150 180 310 215 T600 205 L600 275 L0 275 Z" fill="#234157"/>
        <rect x="0" y="265" width="600" height="135" fill="url(#g-dl-water)"/>
        <ellipse cx="300" cy="268" rx="340" ry="22" fill="#FFFFFF" opacity="0.22"/>
        <g fill="#153243">
          <polygon points="45,270 65,180 85,270"/><polygon points="85,272 108,165 131,272"/>
          <polygon points="125,272 142,200 159,272"/><polygon points="460,270 482,175 504,270"/>
          <polygon points="500,272 526,155 552,272"/><polygon points="545,272 565,195 585,272"/>
        </g>
      </svg>`;
    case 'sapa':
      return `<svg viewBox="0 0 600 400" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
        <defs><linearGradient id="g-sp-sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#1E4068"/><stop offset="60%" stop-color="#7BA7C9"/><stop offset="100%" stop-color="#E2EDF5"/></linearGradient></defs>
        <rect width="600" height="400" fill="url(#g-sp-sky)"/>
        <polygon points="0,230 140,95 270,210 410,75 540,190 600,150 600,280 0,280" fill="#365A6D"/>
        <polygon points="60,250 220,130 370,235 495,120 600,210 600,300 0,300" fill="#2A4D5E"/>
        <ellipse cx="180" cy="185" rx="130" ry="18" fill="#FFFFFF" opacity="0.45"/>
        <ellipse cx="440" cy="165" rx="150" ry="20" fill="#FFFFFF" opacity="0.4"/>
        <path d="M0 250 Q180 215 360 255 T600 240 L600 400 L0 400 Z" fill="#2D6A4F"/>
        <path d="M0 282 Q190 250 375 288 T600 272 L600 400 L0 400 Z" fill="#40916C"/>
        <path d="M0 315 Q200 286 390 322 T600 308 L600 400 L0 400 Z" fill="#52B788"/>
        <path d="M0 348 Q210 322 410 356 T600 344 L600 400 L0 400 Z" fill="#1B4332"/>
      </svg>`;
    case 'nhatrang':
      return `<svg viewBox="0 0 600 400" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="g-nt-sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#0284C7"/><stop offset="65%" stop-color="#38BDF8"/><stop offset="100%" stop-color="#BAE6FD"/></linearGradient>
          <linearGradient id="g-nt-sea" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#0369A1"/><stop offset="100%" stop-color="#0E7490"/></linearGradient>
        </defs>
        <rect width="600" height="400" fill="url(#g-nt-sky)"/>
        <circle cx="485" cy="78" r="32" fill="#FEF08A"/>
        <path d="M310 215 Q380 155 465 182 Q525 162 595 215 Z" fill="#1E4E68"/>
        <path d="M90 215 Q145 182 215 215 Z" fill="#2A6582"/>
        <rect x="0" y="212" width="600" height="115" fill="url(#g-nt-sea)"/>
        <path d="M0 315 Q170 285 350 318 T600 305 L600 400 L0 400 Z" fill="#E6C999"/>
        <path d="M0 315 Q170 285 350 318 T600 305" fill="none" stroke="#FFFFFF" stroke-width="5" opacity="0.85"/>
        <path d="M45 355 Q58 270 92 185" stroke="#5C4033" stroke-width="8" fill="none" stroke-linecap="round"/>
        <g fill="#15803D">
          <path d="M92 185 Q45 165 22 190 Q58 188 92 185Z"/>
          <path d="M92 185 Q138 158 168 182 Q130 185 92 185Z"/>
          <path d="M92 185 Q75 138 110 128 Q104 158 92 185Z"/>
        </g>
      </svg>`;
    case 'haiphong':
      return `<svg viewBox="0 0 600 400" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
        <defs><linearGradient id="g-hp-sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#1E3A8A"/><stop offset="65%" stop-color="#3B82F6"/><stop offset="100%" stop-color="#FCA5A5"/></linearGradient></defs>
        <rect width="600" height="400" fill="url(#g-hp-sky)"/>
        <rect x="0" y="265" width="600" height="135" fill="#0F3D66"/>
        <rect x="0" y="252" width="600" height="14" fill="#1E293B"/>
        <polygon points="185,252 200,95 215,252" fill="#E2E8F0"/>
        <polygon points="405,252 420,95 435,252" fill="#E2E8F0"/>
        <g stroke="#F8FAFC" stroke-width="1.6" opacity="0.85">
          <line x1="200" y1="110" x2="95" y2="252"/><line x1="200" y1="125" x2="135" y2="252"/>
          <line x1="200" y1="110" x2="305" y2="252"/><line x1="200" y1="125" x2="265" y2="252"/>
          <line x1="420" y1="110" x2="315" y2="252"/><line x1="420" y1="125" x2="355" y2="252"/>
          <line x1="420" y1="110" x2="525" y2="252"/><line x1="420" y1="125" x2="485" y2="252"/>
        </g>
        <g fill="#DC2626" opacity="0.92"><circle cx="42" cy="215" r="28"/><circle cx="78" cy="200" r="24"/><circle cx="548" cy="210" r="30"/><circle cx="515" cy="222" r="22"/></g>
      </svg>`;
    case 'hue':
      return `<svg viewBox="0 0 600 400" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
        <defs><linearGradient id="g-hue-sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#312E81"/><stop offset="55%" stop-color="#7C3AED"/><stop offset="100%" stop-color="#FDBA74"/></linearGradient></defs>
        <rect width="600" height="400" fill="url(#g-hue-sky)"/>
        <circle cx="475" cy="95" r="30" fill="#FDE68A" opacity="0.9"/>
        <rect x="0" y="285" width="600" height="115" fill="#1E3A5F"/>
        <rect x="85" y="218" width="430" height="67" fill="#57534E"/>
        <path d="M265 285 V245 Q300 225 335 245 V285 Z" fill="#1C1917"/>
        <path d="M170 285 V252 Q195 238 220 252 V285 Z" fill="#1C1917"/>
        <path d="M380 285 V252 Q405 238 430 252 V285 Z" fill="#1C1917"/>
        <rect x="195" y="168" width="210" height="50" fill="#991B1B"/>
        <polygon points="165,172 300,122 435,172" fill="#D97706"/>
        <polygon points="215,142 300,104 385,142" fill="#F59E0B"/>
      </svg>`;
    default: // cantho
      return `<svg viewBox="0 0 600 400" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
        <defs>
          <linearGradient id="g-ct-sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#0F4C81"/><stop offset="60%" stop-color="#38BDF8"/><stop offset="100%" stop-color="#FDE68A"/></linearGradient>
          <linearGradient id="g-ct-river" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#0E7490"/><stop offset="100%" stop-color="#164E63"/></linearGradient>
        </defs>
        <rect width="600" height="400" fill="url(#g-ct-sky)"/>
        <circle cx="145" cy="95" r="34" fill="#FEF08A" opacity="0.9"/>
        <path d="M0 218 Q120 190 250 212 T600 202 L600 240 L0 240 Z" fill="#166534"/>
        <rect x="0" y="232" width="600" height="168" fill="url(#g-ct-river)"/>
        <path d="M120 295 Q205 335 290 295 L268 322 Q205 342 142 322 Z" fill="#78350F"/>
        <circle cx="185" cy="292" r="10" fill="#F97316"/><circle cx="205" cy="288" r="11" fill="#EAB308"/><circle cx="225" cy="292" r="10" fill="#22C55E"/>
        <line x1="255" y1="298" x2="270" y2="235" stroke="#FEF3C7" stroke-width="3"/>
        <path d="M345 275 Q415 305 485 275 L466 296 Q415 312 364 296 Z" fill="#92400E"/>
        <circle cx="400" cy="272" r="9" fill="#EF4444"/><circle cx="420" cy="270" r="9" fill="#FACC15"/><circle cx="438" cy="273" r="8" fill="#4ADE80"/>
        <line x1="455" y1="278" x2="468" y2="224" stroke="#FEF3C7" stroke-width="2.5"/>
      </svg>`;
  }
}

// Crisp deterministic QR Code SVG
function renderQrSvg(code, size = 112) {
  let cells = '';
  const seed = (code || 'HC-VN-00000').split('').reduce((a, c) => a + c.charCodeAt(0), 0);
  for (let r = 0; r < 11; r++) {
    for (let c = 0; c < 11; c++) {
      const inCorner = (r < 3 && c < 3) || (r < 3 && c > 7) || (r > 7 && c < 3);
      if (inCorner) continue;
      if (((r * 13 + c * 7 + seed) % 3) === 0 || ((r + c + seed) % 5) === 0) {
        cells += `<rect x="${12 + c * 8}" y="${12 + r * 8}" width="6.5" height="6.5" rx="1" fill="#0F4C81"/>`;
      }
    }
  }
  return `
    <svg width="${size}" height="${size}" viewBox="0 0 112 112" xmlns="http://www.w3.org/2000/svg" style="background:#fff;border-radius:10px;">
      <rect x="10" y="10" width="24" height="24" rx="4" fill="none" stroke="#0F4C81" stroke-width="4"/>
      <rect x="17" y="17" width="10" height="10" rx="2" fill="#0F4C81"/>
      <rect x="78" y="10" width="24" height="24" rx="4" fill="none" stroke="#0F4C81" stroke-width="4"/>
      <rect x="85" y="17" width="10" height="10" rx="2" fill="#0F4C81"/>
      <rect x="10" y="78" width="24" height="24" rx="4" fill="none" stroke="#0F4C81" stroke-width="4"/>
      <rect x="17" y="85" width="10" height="10" rx="2" fill="#0F4C81"/>
      ${cells}
    </svg>
  `;
}

/**
 * Batched Wikipedia Action API helper.
 * Properly resolves both `normalized` and `redirects` chains so every requested
 * title maps reliably to its page thumbnail URL.
 */
async function fetchWikiBatch(lang, titles) {
  if (!titles || !titles.length) return {};
  const url = `https://${lang}.wikipedia.org/w/api.php?action=query&titles=${encodeURIComponent(titles.join('|'))}&prop=pageimages&pithumbsize=800&redirects=1&origin=*&format=json`;
  try {
    const res = await fetch(url, { headers: { Accept: 'application/json' } });
    if (!res.ok) return {};
    const data = await res.json();
    const query = data && data.query;
    if (!query || !query.pages) return {};

    const normMap = {};
    (query.normalized || []).forEach(n => { normMap[n.from] = n.to; });

    const redirMap = {};
    (query.redirects || []).forEach(r => { redirMap[r.from] = r.to; });

    const pageThumbByTitle = {};
    const pageThumbByLower = {};
    Object.values(query.pages).forEach(page => {
      if (page && page.title && page.thumbnail && page.thumbnail.source) {
        pageThumbByTitle[page.title] = page.thumbnail.source;
        pageThumbByLower[page.title.toLowerCase()] = page.thumbnail.source;
      }
    });

    const result = {};
    titles.forEach(orig => {
      let cur = normMap[orig] || orig.replace(/_/g, ' ');
      for (let i = 0; i < 6 && redirMap[cur]; i++) {
        cur = redirMap[cur];
      }
      const found = pageThumbByTitle[cur] || pageThumbByLower[cur.toLowerCase()];
      if (found) result[orig] = found;
    });
    return result;
  } catch (err) {
    return {};
  }
}

/**
 * Fetches all 6 Vietnam destination photos in ONE batched EN Wikipedia query,
 * with batched VI Wikipedia and alternate landmark fallbacks for any missing items.
 */
async function loadDestinationPhotos() {
  // 1. Primary batched query (English Wikipedia)
  const enTitles = POPULAR_ROUTES.map(r => r.enTitle);
  const enMap = await fetchWikiBatch('en', enTitles);
  POPULAR_ROUTES.forEach(r => {
    if (enMap[r.enTitle]) routeImageCache[r.slug] = enMap[r.enTitle];
  });

  // 2. Secondary batched fallback (Vietnamese Wikipedia) for any missing routes
  const missingAfterEn = POPULAR_ROUTES.filter(r => !routeImageCache[r.slug]);
  if (missingAfterEn.length) {
    const viMap = await fetchWikiBatch('vi', missingAfterEn.map(r => r.viTitle));
    missingAfterEn.forEach(r => {
      if (viMap[r.viTitle]) routeImageCache[r.slug] = viMap[r.viTitle];
    });
  }

  // 3. Tertiary batched fallback (Alternate landmark titles) for any still-missing routes
  const missingAfterVi = POPULAR_ROUTES.filter(r => !routeImageCache[r.slug]);
  if (missingAfterVi.length) {
    const altMap = await fetchWikiBatch('en', missingAfterVi.map(r => r.altTitle));
    missingAfterVi.forEach(r => {
      if (altMap[r.altTitle]) routeImageCache[r.slug] = altMap[r.altTitle];
    });
  }

  // Stagger image src assignments slightly (80ms apart) to avoid Wikimedia CDN throttling
  POPULAR_ROUTES.forEach((route, idx) => {
    const imgUrl = routeImageCache[route.slug];
    if (imgUrl) {
      setTimeout(() => {
        injectRoutePhoto(route.slug, imgUrl, `${route.from} to ${route.to}`, 0);
      }, idx * 80);
    }
  });
}

/**
 * On-demand fallback when a resolved <img> still triggers onerror after a retry.
 */
async function fetchSingleRouteFallback(slug) {
  const route = POPULAR_ROUTES.find(r => r.slug === slug);
  if (!route) return;
  const tried = routeFallbackTried[slug] || 0;
  routeFallbackTried[slug] = tried + 1;

  let nextUrl = null;
  if (tried === 0) {
    const viMap = await fetchWikiBatch('vi', [route.viTitle]);
    if (viMap[route.viTitle] && viMap[route.viTitle] !== routeImageCache[slug]) {
      nextUrl = viMap[route.viTitle];
    }
  }
  if (!nextUrl && tried <= 1) {
    const altMap = await fetchWikiBatch('en', [route.altTitle]);
    if (altMap[route.altTitle] && altMap[route.altTitle] !== routeImageCache[slug]) {
      nextUrl = altMap[route.altTitle];
    }
  }

  if (nextUrl) {
    routeImageCache[slug] = nextUrl;
    injectRoutePhoto(slug, nextUrl, `${route.from} to ${route.to}`, 0);
  }
}

function handleRouteImgLoad(imgEl, slug) {
  if (imgEl && imgEl.complete && imgEl.naturalWidth > 0) {
    routeImageLoaded[slug] = true;
    imgEl.classList.add('loaded');
  }
}

function handleRouteImgError(imgEl, slug, attempt) {
  const currentAttempt = Number(attempt || 0);
  const url = routeImageCache[slug];
  if (currentAttempt === 0 && url) {
    setTimeout(() => {
      injectRoutePhoto(slug, url, imgEl ? imgEl.alt : slug, 1);
    }, 700);
  } else {
    if (imgEl) imgEl.remove();
    fetchSingleRouteFallback(slug);
  }
}

function injectRoutePhoto(slug, imgUrl, alt, attempt = 0) {
  const slot = document.getElementById(`route-photo-${slug}`);
  if (!slot) return;
  const img = document.createElement('img');
  img.alt = alt;
  img.referrerPolicy = 'no-referrer';
  img.decoding = 'async';
  img.className = 'route-bg-img';
  img.onload = () => handleRouteImgLoad(img, slug);
  img.onerror = () => handleRouteImgError(img, slug, attempt);
  img.src = imgUrl;
  slot.innerHTML = '';
  slot.appendChild(img);
  if (img.complete && img.naturalWidth > 0) {
    handleRouteImgLoad(img, slug);
  }
}
