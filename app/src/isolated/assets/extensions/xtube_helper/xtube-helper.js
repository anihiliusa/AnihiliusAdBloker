(() => {
  const STYLE_ID = 'xtube-safe-layout-v3';

  function isWatchPage() {
    return location.pathname === '/watch' || location.href.includes('/watch?');
  }

  function addStyle() {
    if (document.getElementById(STYLE_ID)) return;
    const style = document.createElement('style');
    style.id = STYLE_ID;
    style.textContent = `
      ytm-mobile-topbar-renderer,
      ytm-pivot-bar-renderer,
      ytm-app-header,
      ytm-masthead,
      ytm-promoted-sparkles-web-renderer,
      ytm-companion-slot,
      .ytm-app-promo,
      .ytp-ad-overlay-container,
      .ytp-ad-module,
      .video-ads {
        display: none !important;
        visibility: hidden !important;
        opacity: 0 !important;
        pointer-events: none !important;
      }
      html, body { background: #050508 !important; }
    `;
    (document.documentElement || document.head || document.body).appendChild(style);
  }

  function addWatchStyle() {
    if (!isWatchPage()) return;
    if (document.getElementById(STYLE_ID + '-watch')) return;
    const style = document.createElement('style');
    style.id = STYLE_ID + '-watch';
    style.textContent = `
      ytm-watch-next-secondary-results-renderer,
      ytm-related-items-renderer,
      ytm-reel-shelf-renderer,
      ytm-rich-section-renderer,
      ytm-horizontal-card-list-renderer,
      ytm-bottom-sheet-renderer,
      ytm-engagement-panel,
      ytm-mealbar-promo-renderer,
      .ytm-bottom-sheet-overlay,
      .ytm-popup-overlay,
      .ytp-endscreen-content,
      .ytp-ce-element,
      .ytp-pause-overlay,
      .ytp-suggestion-set,
      .ytp-videowall-still,
      .ytp-related-on-error-overlay,
      .ytp-player-content.ytp-iv-player-content,
      a[href*='/shorts/'] {
        display: none !important;
        visibility: hidden !important;
        opacity: 0 !important;
        max-height: 0 !important;
        overflow: hidden !important;
        pointer-events: none !important;
      }
    `;
    (document.documentElement || document.head || document.body).appendChild(style);
  }

  function keepVisibleState() {
    try {
      Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
      Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
      document.hasFocus = () => true;
    } catch (e) {}
  }

  function removeBaseNodes() {
    const selectors = [
      'ytm-mobile-topbar-renderer',
      'ytm-pivot-bar-renderer',
      'ytm-app-header',
      'ytm-masthead',
      'ytm-promoted-sparkles-web-renderer',
      'ytm-companion-slot',
      '.ytm-app-promo',
      '.ytp-ad-overlay-container',
      '.ytp-ad-module',
      '.video-ads'
    ];
    for (const selector of selectors) {
      try { document.querySelectorAll(selector).forEach(node => node.remove()); } catch (e) {}
    }
  }

  function removeWatchNodes() {
    if (!isWatchPage()) return;
    const selectors = [
      'ytm-watch-next-secondary-results-renderer',
      'ytm-related-items-renderer',
      'ytm-reel-shelf-renderer',
      'ytm-rich-section-renderer',
      'ytm-horizontal-card-list-renderer',
      'ytm-bottom-sheet-renderer',
      'ytm-engagement-panel',
      'ytm-mealbar-promo-renderer',
      '.ytm-bottom-sheet-overlay',
      '.ytm-popup-overlay',
      '.ytp-endscreen-content',
      '.ytp-ce-element',
      '.ytp-pause-overlay',
      '.ytp-suggestion-set',
      '.ytp-videowall-still',
      'a[href*="/shorts/"]'
    ];
    for (const selector of selectors) {
      try { document.querySelectorAll(selector).forEach(node => node.remove()); } catch (e) {}
    }
  }

  function closePrompts() {
    try {
      document.querySelectorAll('button, [role="button"]').forEach(btn => {
        const txt = (btn.textContent || '').trim().toLowerCase();
        if (txt.includes('да не се включва') || txt.includes('not now') || txt.includes('no thanks') || txt === '×' || txt === 'x') {
          btn.click();
        }
      });
    } catch (e) {}
  }

  function shortClipGuard() {
    try {
      document.querySelectorAll('button, .ytp-ad-skip-button, .ytp-skip-ad-button, .ytp-ad-skip-button-modern').forEach(button => {
        const text = (button.textContent || '').toLowerCase();
        if (text.includes('skip') || text.includes('пропуск') || text.includes('пропусни')) button.click();
      });
      const video = document.querySelector('video');
      const player = document.querySelector('.html5-video-player');
      if (video && player && /ad-showing|ad-interrupting/.test(player.className || '')) {
        if (Number.isFinite(video.duration) && video.duration > 0 && video.duration <= 8) video.currentTime = video.duration;
        video.play().catch(() => {});
      }
    } catch (e) {}
  }

  function keepPlayback() {
    try {
      const video = document.querySelector('video');
      if (video && video.paused && !video.ended && isWatchPage()) {
        video.play().catch(() => {});
      }
    } catch (e) {}
  }

  function run() {
    addStyle();
    addWatchStyle();
    keepVisibleState();
    removeBaseNodes();
    removeWatchNodes();
    closePrompts();
    shortClipGuard();
    keepPlayback();
  }

  run();
  setInterval(run, 900);
  new MutationObserver(run).observe(document.documentElement, { childList: true, subtree: true });
  document.addEventListener('visibilitychange', () => setTimeout(run, 0), true);
})();
