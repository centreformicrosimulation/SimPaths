(() => {
  function setup() {
    const bar = document.querySelector('[data-docs-channel]');
    if (!bar) return;
    bar.dataset.versionReady = "true";
    if (new URL(location.href).searchParams.has('version-page-missing')) {
      const notice = bar.querySelector('[data-version-notice]');
      notice.hidden = false;
      notice.textContent = 'That page is not available in this version. Showing its homepage.';
    }
  }
  // Capture before Material's instant navigation; never reuse a channel's
  // search worker or base URL after switching to the other channel.
  window.addEventListener('click', async event => {
    const link = event.target.closest('[data-version-target]');
    if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    event.stopImmediatePropagation();
    const bar = link.closest('[data-docs-channel]');
    const page = bar.dataset.docsPage;
    const target = new URL(link.dataset.versionTarget === 'develop' ? '/develop/' : '/', location.href);
    try {
      const response = await fetch(new URL('version.json', target), {cache:'no-cache'});
      if (!response.ok) throw new Error('Version unavailable');
      const manifest = await response.json();
      const found = manifest.pages.includes(page);
      const destination = new URL(found ? page : './', target);
      if (found) destination.hash = location.hash;
      else destination.searchParams.set('version-page-missing', '1');
      location.assign(destination.href);
    } catch (_) {
      const notice = bar.querySelector('[data-version-notice]');
      notice.hidden = false;
      notice.textContent = 'This version is temporarily unavailable. Please try again later.';
    }
  }, true);
  const connect = () => {
    setup();
    if (typeof document$ !== 'undefined') document$.subscribe(setup);
  };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', connect, {once:true});
  else connect();
})();
