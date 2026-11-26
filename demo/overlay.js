// Drawn into the page while the demo is recorded, by record.mjs, on every document.
//
// A headless browser records no pointer and no narration, so a viewer sees fields fill and
// panels open with nothing causing it. This adds the two things the recording is missing: a
// cursor that follows the real mouse events Playwright sends, and a caption the walk writes.
//
// It lives on the documentElement rather than the body, because Vaadin writes the application
// into the body when it boots and takes anything already there with it.
(() => {
  if (window.__demo) {
    return;
  }

  const css = document.createElement('style');
  css.textContent = `
    #demo-cursor { position: fixed; top: 0; left: 0; z-index: 2147483647; width: 24px; height: 24px;
      pointer-events: none; filter: drop-shadow(0 1px 3px rgba(0,0,0,.5)); }
    #demo-ring { position: fixed; z-index: 2147483646; width: 16px; height: 16px;
      margin: -8px 0 0 -8px; border-radius: 50%; pointer-events: none; opacity: 0;
      background: rgba(37,99,235,.3); border: 2px solid rgba(37,99,235,.95); }
    #demo-ring.hit { animation: demo-hit 500ms ease-out; }
    @keyframes demo-hit {
      from { opacity: 1; transform: scale(.5); }
      to { opacity: 0; transform: scale(2.8); }
    }
    #demo-caption { position: fixed; z-index: 2147483647; left: 32px; bottom: 32px;
      max-width: 62%; padding: 12px 20px; border-radius: 12px; pointer-events: none;
      background: rgba(17,24,39,.92); color: #fff; opacity: 0;
      font: 500 21px/1.35 Inter, -apple-system, system-ui, sans-serif;
      transition: opacity 240ms ease; }
    #demo-caption.on { opacity: 1; }
  `;

  const cursor = document.createElement('div');
  cursor.id = 'demo-cursor';
  cursor.innerHTML = '<svg viewBox="0 0 24 24" width="24" height="24">'
    + '<path d="M5 2 L19 13 L12.5 13.6 L16 21 L13.2 22 L9.8 14.7 L5 18 Z"'
    + ' fill="#111827" stroke="#ffffff" stroke-width="1.5" stroke-linejoin="round"/></svg>';

  const ring = document.createElement('div');
  ring.id = 'demo-ring';

  const caption = document.createElement('div');
  caption.id = 'demo-caption';

  /** Puts the four back whenever something has taken them off the page. */
  function ensure() {
    const root = document.documentElement;
    if (!root) {
      return false;
    }
    for (const element of [css, cursor, ring, caption]) {
      if (!element.isConnected) {
        root.appendChild(element);
      }
    }
    return true;
  }

  function moveTo(x, y) {
    cursor.style.transform = 'translate(' + x + 'px,' + y + 'px)';
    ring.style.left = x + 'px';
    ring.style.top = y + 'px';
  }

  addEventListener('mousemove', (event) => {
    ensure();
    moveTo(event.clientX, event.clientY);
  }, true);

  addEventListener('mousedown', () => {
    if (!ensure()) {
      return;
    }
    ring.classList.remove('hit');
    void ring.offsetWidth;
    ring.classList.add('hit');
  }, true);

  window.__demo = {
    say(text) {
      ensure();
      caption.textContent = text;
      caption.classList.toggle('on', Boolean(text));
    },
    ensure,
  };

  // The document may not exist yet when this runs, which is the whole reason for ensure().
  if (!ensure()) {
    document.addEventListener('DOMContentLoaded', ensure);
  }
})();
