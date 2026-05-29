(() => {
  const HIDE_STYLE_ID = 'xtube-mobile-clean-layout';

  function addStyle() {
    if (document.getElementById(HIDE_STYLE_ID)) return;
    const style = document.createElement('style');
    style.id = HIDE_STYLE_ID;
    style.textContent = `
      ytd-watch-next-secondary-results-renderer,
      ytm-watch-next-secondary-results-renderer,
      ytm-related-items-renderer,
      ytd-reel-shelf-renderer,
      ytm-reel-shelf-renderer,
      ytd-rich-section-renderer,
      ytm-rich-section-renderer,
      ytd-shelf-renderer,
      ytm-shelf-renderer,
      ytd-compact-video-renderer,
      ytm-compact-video-renderer,
      ytd-item-section-renderer[section-identifier='related-items'],
      .ytp-endscreen-content,
      .ytp-ce-element,
      .ytp-pause-overlay,
      .ytp-suggestion-set,
      .ytp-videowall-still,
      .ytp-related-on-error-overlay,
      .ytp-player-content.ytp-iv-player-content,
      [is-shorts],
      a[href*='/shorts/'] {
        display: none !important;
        visibility: hidden !important;
        opacity: 0 !important;
        height: 0 !important;
        max-height: 0 !important;
        overflow: hidden !important;
      }
      ytm-single-column-watch-next-results-renderer,
      ytm-item-section-renderer {
        margin-top: 0 !important;
      }
    `;
    (document.documentElement || document.head || document.body).appendChild(style);
  }

  function keepPlaybackStateFriendly() {
    try {
      Object.defineProperty(document, 'hidden', { get: () => false, configurable: true });
      Object.defineProperty(document, 'visibilityState', { get: () => 'visible', configurable: true });
      document.hasFocus = () => true;
    } catch (e) {}
  }

  function cleanupRecommendations() {
    const selectors = [
      'ytd-watch-next-secondary-results-renderer',
      'ytm-watch-next-secondary-results-renderer',
      'ytm-related-items-renderer',
      'ytd-reel-shelf-renderer',
      'ytm-reel-shelf-renderer',
      'ytd-compact-video-renderer',
      'ytm-compact-video-renderer',
      'ytd-rich-section-renderer',
      'ytm-rich-section-renderer',
      '.ytp-endscreen-content',
      '.ytp-ce-element',
      '.ytp-pause-overlay',
      '.ytp-suggestion-set',
      '.ytp-videowall-still',
      'a[href*="/shorts/"]'
    ];
    for (const selector of selectors) {
      try {
        document.querySelectorAll(selector).forEach(node => node.remove());
      } catch (e) {}
    }
  }

  function compactShortInterstitials() {
    try {
      const buttons = document.querySelectorAll('button, .ytp-ad-skip-button, .ytp-skip-ad-button, .ytp-ad-skip-button-modern');
      buttons.forEach(button => {
        const text = (button.textContent || '').toLowerCase();
        if (text.includes('skip') || text.includes('пропуск') || text.includes('пропусни')) {
          button.click();
        }
      });
      const video = document.querySelector('video');
      const player = document.querySelector('.html5-video-player');
      if (video && player && /ad-showing|ad-interrupting/.test(player.className || '')) {
        if (Number.isFinite(video.duration) && video.duration > 0 && video.duration <= 8) {
          video.currentTime = video.duration;
        }
        video.play().catch(() => {});
      }
    } catch (e) {}
  }

  function run() {
    addStyle();
    keepPlaybackStateFriendly();
    cleanupRecommendations();
    compactShortInterstitials();
  }

  run();
  setInterval(run, 700);
  new MutationObserver(run).observe(document.documentElement, { childList: true, subtree: true });
  document.addEventListener('visibilitychange', () => setTimeout(run, 0), true);
})();
