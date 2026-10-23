(() => {
  let dispose = () => {};
  let mountedNav = null;

  const setup = () => {
    const sidebar = document.querySelector('.md-sidebar--secondary:not([hidden])');
    const nav = sidebar?.querySelector('.md-nav--secondary');
    // Material can emit again for a same-page anchor; keep its layout hooks stable.
    if (nav && nav === mountedNav) return;
    dispose();
    dispose = () => {};
    const list = nav?.querySelector(':scope > .md-nav__list');
    const header = document.querySelector('.md-header');
    const main = document.querySelector('.md-main__inner');
    if (!list || !header || !main) return;

    const links = [...list.querySelectorAll('a.md-nav__link[href]')];
    if (!links.length) return;
    const desktop = matchMedia('(min-width: 60em)');
    let frame = 0;
    let readyFrame = 0;
    let disposed = false;
    let current = null;
    nav.classList.add('sp-toc');
    mountedNav = nav;

    const update = () => {
      frame = 0;
      if (disposed || !desktop.matches || !nav.getClientRects().length) {
        sidebar.classList.remove('sp-toc-sidebar');
        return;
      }
      // Preserve the initial gap below the header instead of creeping up on scroll.
      const headerBottom = header.getBoundingClientRect().bottom;
      const top = headerBottom + parseFloat(getComputedStyle(main).marginTop);
      main.style.setProperty('--sp-toc-header-height', `${headerBottom}px`);
      sidebar.style.setProperty('--sp-toc-top', `${top}px`);
      sidebar.classList.add('sp-toc-sidebar');

      // Material handles scrolling, nested headings, hash links and the last section.
      // Before its first heading is reached, the first contents entry is the useful default.
      const active = links.find(link => link.classList.contains('md-nav__link--active')) || links[0];
      if (current !== active) {
        current?.removeAttribute('aria-current');
        active.setAttribute('aria-current', 'location');
        current = active;
      }
      const box = active.getBoundingClientRect();
      // A background marker leaves anchor offset parents intact for Material's toc.follow.
      list.style.setProperty('--sp-toc-offset', `${box.top - list.getBoundingClientRect().top}px`);
      list.style.setProperty('--sp-toc-height', `${box.height}px`);
      if (!nav.classList.contains('sp-toc--ready') && !readyFrame) {
        readyFrame = requestAnimationFrame(() => {
          readyFrame = 0;
          nav.classList.add('sp-toc--ready');
        });
      }
    };
    const schedule = () => {
      if (!disposed && !frame) frame = requestAnimationFrame(update);
    };
    const activeObserver = new MutationObserver(schedule);
    activeObserver.observe(list, { subtree: true, attributes: true, attributeFilter: ['class'] });
    const sizeObserver = new ResizeObserver(schedule);
    [nav, header, main].forEach(element => sizeObserver.observe(element));
    desktop.addEventListener('change', schedule);
    window.addEventListener('resize', schedule);
    document.fonts.ready.then(schedule);
    schedule();

    // Instant navigation reuses the document: never retain the previous page's observers.
    dispose = () => {
      disposed = true;
      mountedNav = null;
      cancelAnimationFrame(frame);
      cancelAnimationFrame(readyFrame);
      activeObserver.disconnect();
      sizeObserver.disconnect();
      desktop.removeEventListener('change', schedule);
      window.removeEventListener('resize', schedule);
      current?.removeAttribute('aria-current');
      nav.classList.remove('sp-toc', 'sp-toc--ready');
      sidebar.classList.remove('sp-toc-sidebar');
      sidebar.style.removeProperty('--sp-toc-top');
      main.style.removeProperty('--sp-toc-header-height');
      list.style.removeProperty('--sp-toc-offset');
      list.style.removeProperty('--sp-toc-height');
    };
  };

  if (typeof document$ !== 'undefined') document$.subscribe(setup);
  else if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', setup, { once: true });
  else setup();
})();
