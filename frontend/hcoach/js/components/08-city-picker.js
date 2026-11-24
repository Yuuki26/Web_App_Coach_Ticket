/* =========================================================================
 * 8. ALL 63 VIETNAM PROVINCES DROPDOWN PICKER
 * ========================================================================= */
function renderCityOptions(field, searchText) {
  const exclude = field === 'from' ? state.query.to : state.query.from;
  const current = state.query[field];
  const kw = normalizeSearch(searchText);
  const avail = ALL_PROVINCES_VN.filter(c => c !== exclude);

  const renderRow = (city) => `
    <button type="button" class="city-opt ${city === current ? 'active' : ''}" onclick="selectCity('${field}', '${escapeHtml(city)}')">
      <span class="truncate">${escapeHtml(city)}</span>
      ${city === current ? icon('check', 14) : ''}
    </button>
  `;

  if (kw) {
    const list = avail.filter(c => {
      const hay = normalizeSearch(c + ' ' + (CITY_SEARCH_ALIASES[c] || ''));
      return hay.includes(kw);
    });
    if (!list.length) {
      return `<div style="padding:20px;text-align:center;" class="text-xs text-muted">No matching Vietnam province or city</div>`;
    }
    return list.map(renderRow).join('');
  }

  const featured = FEATURED_CITIES.filter(c => c !== exclude);
  const others = avail.filter(c => !FEATURED_CITIES.includes(c));
  return `
    <div class="city-group-hdr">Popular Destinations</div>
    ${featured.map(renderRow).join('')}
    <div class="city-group-hdr">All 63 Provinces & Cities (A–Z)</div>
    ${others.map(renderRow).join('')}
  `;
}

function renderCityPicker(field, label) {
  const val = state.query[field];
  const isOpen = state.openCityDropdown === field;
  const pinColor = field === 'from' ? 'var(--navy)' : 'var(--orange)';

  return `
    <div class="city-picker">
      <label class="field-label-muted">${label}</label>
      <button type="button" class="search-box-btn" onclick="toggleCityDropdown('${field}', event)">
        <span class="flex items-center gap-2" style="min-width:0;">
          <span style="color:${pinColor};display:flex;">${icon('mapPin', 15)}</span>
          <span class="search-box-val">${escapeHtml(val || 'Select city')}</span>
        </span>
        <span class="text-muted" style="transform:${isOpen ? 'rotate(180deg)' : 'none'};transition:transform 0.15s;">${icon('chevronDown', 14)}</span>
      </button>

      ${isOpen ? `
        <div class="city-dropdown" onclick="event.stopPropagation()">
          <div class="city-dropdown-search">
            <div class="input-wrap">
              <span class="input-icon">${icon('search', 14)}</span>
              <input
                id="city-search-${field}"
                class="input input-with-icon"
                style="padding-top:8px;padding-bottom:8px;font-size:0.84rem;"
                type="text"
                autocomplete="off"
                value="${escapeHtml(state.citySearchText)}"
                placeholder="Search 63 Vietnam provinces..."
                oninput="filterCityDropdown('${field}', this.value)"
              />
            </div>
          </div>
          <div id="city-list-${field}" class="city-dropdown-list">
            ${renderCityOptions(field, state.citySearchText)}
          </div>
        </div>
      ` : ''}
    </div>
  `;
}
