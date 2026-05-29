(() => {
  const HIDE_STYLE_ID = 'xtube-mobile-clean-layout-v2';

  function addStyle() {
    if (document.getElementById(HIDE_STYLE_ID)) return;
    const style = document.createElement('style');
    style.id = HIDE_STYLE_ID;
    style.textContent = `
      ytm-mobile-topbar-renderer,
      ytm-pivot-bar-renderer,
      ytm-app-header,
      ytm-masthead,
      ytm-promoted-sparkles-web-renderer,
      ytm-companion-slot,
      ytm-watch-next-secondary-results-renderer,
      ytm-single-column-watch-next-results-renderer ytm-item-section-renderer:not(:first-child),
      ytm-related-items-renderer,
      ytm-reel-shelf-renderer,
      ytm-rich-section-renderer,
      ytm-shelf-renderer,
      ytm-compact-video-renderer,
      ytm-compact-playlist-renderer,
      ytm-horizontal-card-list-renderer,
      ytm-bottom-sheet-renderer,
      ytm-engagement-panel,
      ytm-mealbar-promo-renderer,
      ytm-consent-bump-renderer,
      ytm-popup-container,
      ytm-dialog,
      ytm-modal,
      ytd-watch-next-secondary-results-renderer,
      ytd-rich-section-renderer,
      ytd-reel-shelf-renderer,
      ytd-compact-video-renderer,
      ytd-item-section-renderer[section-identifier='related-items'],
      .ytm-app-promo,
      .ytm-bottom-sheet-overlay,
      .ytm-popup-overlay,
      .ytm-mealbar-promo-renderer,
      .ytp-endscreen-content,
      .ytp-ce-element,
      .ytp-pause-overlay,
      .ytp-suggestion-set,
      .ytp-videowall-still,
      .ytp-related-on-error-overlay,
      .ytp-player-content.ytp-iv-player-content,
      .ytp-ad-overlay-container,
      .ytp-ad-module,
      .video-ads,
      [is-shorts],
      a[href*='/shorts/'] {
        display: none !important;
        visibility: hidden !important;
        opacity: 0 !important;
        height: 0 !important;
        max-height: 0 !important;
        overflow: hidden !important;
        pointer-events: none !important;
      }
      html, body { background: #050508 !important; }
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

  function removeNodes() {
    const selectors = [
      'ytm-mobile-topbar-renderer',
      'ytm-pivot-bar-renderer',
      'ytm-app-header',
      'ytm-masthead',
      'ytm-promoted-sparkles-web-renderer',
      'ytm-companion-slot',
      'ytm-watch-next-secondary-results-renderer',
      'ytm-related-items-renderer',
      'ytm-reel-shelf-renderer',
      'ytm-rich-section-renderer',
      'ytm-shelf-renderer',
      'ytm-compact-video-renderer',
      'ytm-compact-playlist-renderer',
      'ytm-horizontal-card-list-renderer',
      'ytm-bottom-sheet-renderer',
      'ytm-engagement-panel',
      'ytm-mealbar-promo-renderer',
      'ytm-consent-bump-renderer',
      'ytm-popup-container',
      'ytm-dialog',
      'ytm-modal',
      '.ytm-app-promo',
      '.ytm-bottom-sheet-overlay',
      '.ytm-popup-overlay',
      '.ytp-endscreen-content',
      '.ytp-ce-element',
      '.ytp-pause-overlay',
      '.ytp-suggestion-set',
      '.ytp-videowall-still',
      '.ytp-ad-overlay-container',
      '.ytp-ad-module',
      '.video-ads',
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
        if (txt.includes('да не се включва') || txt.includes('not now') || txt.includes('no thanks') || txt.includes('close') || txt === '×' || txt === 'x') {
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
      if (video && video.paused && !video.ended && document.location.href.includes('/watch')) {
        video.play().catch(() => {});
      }
    } catch (e) {}
  }

  function run() {
    addStyle();
    keepVisibleState();
    removeNodes();
    closePrompts();
    shortClipGuard();
    keepPlayback();
  }

  run();
  setInterval(run, 650);
  new MutationObserver(run).observe(document.documentElement, { childList: true, subtree: true });
  document.addEventListener('visibilitychange', () => setTimeout(run, 0), true);
})();
