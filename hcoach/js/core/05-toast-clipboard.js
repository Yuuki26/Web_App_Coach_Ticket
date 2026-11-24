/* =========================================================================
 * 5. TOAST & CLIPBOARD HELPERS
 * ========================================================================= */
function showToast(message, type = 'info') {
  const box = document.getElementById('toast-container');
  if (!box) return;
  const cls = type === 'success' ? 'toast-success' : type === 'error' ? 'toast-error' : 'toast-info';
  const ic = type === 'success' ? 'checkCircle' : type === 'error' ? 'alertCircle' : 'ticket';

  const el = document.createElement('div');
  el.className = `toast-item ${cls}`;
  el.innerHTML = `
    <span>${icon(ic, 18)}</span>
    <span style="flex:1;line-height:1.35;">${escapeHtml(message)}</span>
    <button type="button" style="opacity:0.6;padding:2px;" onclick="this.parentElement.remove()">${icon('x', 14)}</button>
  `;
  box.appendChild(el);

  setTimeout(() => {
    el.style.transition = 'all 0.25s ease';
    el.style.opacity = '0';
    el.style.transform = 'translateX(20px)';
    setTimeout(() => el.remove(), 260);
  }, 3400);
}

function copyText(text) {
  try {
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.opacity = '0';
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    document.body.removeChild(ta);
  } catch (e) {}
}
