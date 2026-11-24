/* =========================================================================
 * 13. SCREEN 5: PAYMENT
 * ========================================================================= */
function calcTotals() {
  const base = seatsSubtotal(state.selectedTrip, state.selectedSeats);
  let discount = 0;
  if (state.appliedPromo && PROMO_CODES[state.appliedPromo]) {
    const p = PROMO_CODES[state.appliedPromo];
    discount = p.type === 'percent' ? Math.round(base * (p.value / 100)) : Math.min(base, p.value);
  }
  const after = Math.max(0, base - discount);
  const tax = Math.round(after * 0.08);
  return { base, discount, tax, total: after + tax };
}

function renderPaymentScreen() {
  const trip = state.selectedTrip;
  if (!trip) return '';
  const { base, discount, tax, total } = calcTotals();
  const methods = [
    { id: 'card', lbl: 'Credit / Debit card', sub: 'Visa, Mastercard, Napas', ic: 'creditCard' },
    { id: 'ewallet', lbl: 'MoMo / ZaloPay', sub: 'Instant 1-tap e-wallet', ic: 'wallet' },
    { id: 'vietqr', lbl: 'VietQR Bank Transfer', sub: 'Scan with any VN bank app', ic: 'qr' },
    { id: 'digital', lbl: 'Apple Pay / Google Pay', sub: 'Touch ID or Face ID', ic: 'smartphone' }
  ];

  return `
    <div style="background:#fff;border-bottom:1px solid var(--border);padding:14px 0;">
      <div class="container-md flex items-center gap-4">
        <button type="button" style="padding:8px;color:var(--muted);" onclick="navigate('passenger')">${icon('arrowLeft', 18)}</button>
        ${renderStepper(2)}
      </div>
    </div>

    <div class="container-md" style="padding-top:28px;padding-bottom:64px;display:flex;flex-direction:column;gap:20px;">
      <div class="card p-6">
        <h2 class="font-extrabold mb-4" style="font-size:1.15rem;">Payment method</h2>
        <div class="pay-method-grid mb-4">
          ${methods.map(m => {
            const act = state.paymentMethod === m.id;
            return `
              <button type="button" class="pay-method-card ${act ? 'active' : ''}" onclick="state.paymentMethod='${m.id}'; render();">
                <span style="width:38px;height:38px;border-radius:10px;display:flex;align-items:center;justify-content:center;background:${act ? 'var(--navy)' : 'var(--bg)'};color:${act ? '#fff' : 'var(--muted)'};">${icon(m.ic, 16)}</span>
                <div style="flex:1;min-width:0;">
                  <div class="font-bold text-sm truncate ${act ? 'text-navy' : ''}">${m.lbl}</div>
                  <div class="text-xs text-muted truncate">${m.sub}</div>
                </div>
              </button>
            `;
          }).join('')}
        </div>

        ${state.paymentMethod === 'card' ? `
          <div class="grid-2" style="padding-top:14px;border-top:1px solid var(--border);">
            <div class="col-span-2">
              <label class="field-label-muted">Cardholder name</label>
              <input class="input" type="text" placeholder="MINH ANH NGUYEN" value="${escapeHtml(state.cardForm.name)}" oninput="state.cardForm.name=this.value"/>
            </div>
            <div class="col-span-2">
              <label class="field-label-muted">Card number</label>
              <input class="input" type="text" maxlength="19" placeholder="4242 4242 4242 4242" value="${escapeHtml(state.cardForm.number)}" oninput="state.cardForm.number=this.value"/>
            </div>
            <div>
              <label class="field-label-muted">Expiry</label>
              <input class="input" type="text" maxlength="7" placeholder="MM / YY" value="${escapeHtml(state.cardForm.expiry)}" oninput="state.cardForm.expiry=this.value"/>
            </div>
            <div>
              <label class="field-label-muted">CVV</label>
              <input class="input" type="password" maxlength="4" placeholder="123" value="${escapeHtml(state.cardForm.cvv)}" oninput="state.cardForm.cvv=this.value"/>
            </div>
          </div>
        ` : state.paymentMethod === 'vietqr' ? `
          <div class="flex items-center gap-4" style="background:var(--bg);padding:16px;border-radius:12px;border:1px solid var(--border);">
            ${renderQrSvg('VIETQR-' + total, 88)}
            <div>
              <div class="font-bold text-sm mb-2">Scan VietQR for instant bank transfer</div>
              <div class="text-xs text-muted">Amount: <strong class="text-orange">${formatVND(total)}</strong> · Supported by 40+ Vietnamese banks.</div>
            </div>
          </div>
        ` : `
          <div class="text-sm text-muted" style="background:var(--bg);padding:14px;border-radius:12px;">
            Click the button below to confirm your payment securely with <strong>${methods.find(x => x.id === state.paymentMethod).lbl}</strong>.
          </div>
        `}
      </div>

      <div class="card p-6">
        <h3 class="font-bold mb-4">Price breakdown</h3>
        <div class="flex flex-col gap-3 text-sm">
          <div class="flex justify-between"><span class="text-muted">Base fare × ${state.selectedSeats.length} seat${state.selectedSeats.length > 1 ? 's' : ''} (${state.selectedSeats.join(', ')})</span><strong>${formatVND(base)}</strong></div>
          ${discount > 0 ? `<div class="flex justify-between text-success font-bold"><span>Promo (${state.appliedPromo})</span><span>-${formatVND(discount)}</span></div>` : ''}
          <div class="flex justify-between"><span class="text-muted">VAT & service fees (8%)</span><strong>${formatVND(tax)}</strong></div>
          <div class="flex justify-between" style="padding-top:12px;border-top:1px solid var(--border);font-size:1.1rem;font-weight:800;">
            <span>Total</span>
            <span class="text-orange">${formatVND(total)}</span>
          </div>
        </div>
      </div>

      <div class="card p-5">
        <div class="check-row" style="align-items:flex-start;" onclick="state.agreedTerms=!state.agreedTerms; render();">
          <span class="check-box ${state.agreedTerms ? 'checked' : ''}" style="margin-top:2px;">${state.agreedTerms ? icon('check', 11) : ''}</span>
          <span class="text-sm text-muted">I agree to H Coach Vietnam's <strong class="text-navy">Terms of Service</strong> & <strong class="text-navy">Privacy Policy</strong> and confirm the passenger details are accurate.</span>
        </div>
      </div>

      <div>
        <button type="button" class="btn btn-primary btn-lg btn-full" ${!state.agreedTerms || state.isProcessingPayment ? 'disabled' : ''} onclick="processPayment()">
          ${state.isProcessingPayment ? `<span class="spinner"></span> Processing payment…` : `${icon('lock', 16)} Pay ${formatVND(total)} securely`}
        </button>
        <p class="text-xs text-muted mt-2" style="text-align:center;">${icon('shield', 12)} 256-bit SSL encryption · PCI DSS compliant</p>
      </div>
    </div>
  `;
}
