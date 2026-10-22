(() => {
  let mounted = null;
  let dispose = () => {};

  const setup = () => {
    const panel = document.querySelector('.md-sidebar--primary');
    const header = document.querySelector('.md-header');
    const search = document.querySelector('[data-md-component="search"]');
    if (!header || !search) return;
    if (mounted?.panel === panel && mounted.header === header && mounted.search === search) return;
    dispose();
    mounted = { panel, header, search };

    const events = new AbortController();
    const listen = (element, type, action, options = {}) =>
      element?.addEventListener(type, action, { ...options, signal: events.signal });
    const drawer = document.querySelector('#__drawer');
    const searchToggle = document.querySelector('#__search');
    const mobile = matchMedia('(max-width: 76.24em)');
    const primaryNav = panel?.querySelector('.md-nav--primary');
    const drawerButton = header.querySelector('[data-sp-toggle="drawer"]');
    const searchInput = search.querySelector('.md-search__input');
    let focusFrame = 0;
    let searchOpener = null;

    if (panel) panel.id = 'sp-site-navigation';
    search.id = 'sp-site-search';
    search.setAttribute('aria-label', 'Search SimPaths');

    // Keep Material's checkbox state and events as the single source of truth.
    const setToggle = (toggle, open) => {
      if (toggle && toggle.checked !== open) toggle.click();
    };
    const focusAfterUpdate = (element) => {
      cancelAnimationFrame(focusFrame);
      focusFrame = requestAnimationFrame(() => {
        if (element?.isConnected) element.focus({ preventScroll: true });
      });
    };

    // The desktop search pill uses the same native control as the mobile icon.
    const searchLabel = search.querySelector('label.md-search__icon[for="__search"]');
    if (searchLabel) {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = searchLabel.className;
      button.dataset.spToggle = 'search';
      button.setAttribute('aria-controls', search.id);
      button.replaceChildren(...searchLabel.childNodes);
      searchLabel.replaceWith(button);
    }
    const searchButtons = [...header.querySelectorAll('[data-sp-toggle="search"]')];

    panel?.querySelectorAll('label.md-nav__title[for]').forEach((label) => {
      if (label.htmlFor === '__drawer') return;
      const button = document.createElement('button');
      button.type = 'button';
      button.className = label.className;
      button.dataset.spNavBack = label.htmlFor;
      button.setAttribute('aria-label', `Back from ${label.textContent.trim()}`);
      button.replaceChildren(...label.childNodes);
      label.replaceWith(button);
    });

    let closeButton = panel?.querySelector('.sp-drawer-close');
    if (panel && !closeButton) {
      closeButton = document.createElement('button');
      closeButton.type = 'button';
      closeButton.className = 'sp-drawer-close';
      closeButton.setAttribute('aria-label', 'Close site navigation');
      closeButton.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="m6.4 5.2 5.6 5.6 5.6-5.6 1.2 1.2-5.6 5.6 5.6 5.6-1.2 1.2-5.6-5.6-5.6 5.6-1.2-1.2 5.6-5.6-5.6-5.6z"/></svg>';
      panel.append(closeButton);
    }

    const currentPanel = () => {
      let current = primaryNav;
      let child;
      while ((child = current?.querySelector(':scope > .md-nav__list > .md-nav__item > .md-nav__toggle:checked ~ .md-nav'))) {
        current = child;
      }
      return current;
    };
    const visible = (element) => element.getClientRects().length > 0 &&
      getComputedStyle(element).visibility === 'visible';
    const drawerTargets = () => {
      const current = currentPanel();
      if (!current) return [];
      return [...current.querySelectorAll('a[href], button, label[tabindex="0"]')]
        .filter((element) => element.closest('.md-nav') === current && visible(element))
        .concat(closeButton);
    };
    const syncBranchState = () => {
      panel?.querySelectorAll('label.md-nav__link[for]').forEach((label) => {
        const toggle = document.getElementById(label.htmlFor);
        const title = label.textContent.trim() || label.previousElementSibling?.textContent.trim();
        label.setAttribute('role', 'button');
        label.setAttribute('aria-label', `Toggle ${title || 'section'}`);
        label.setAttribute('aria-expanded', String(Boolean(toggle?.checked)));
        if (mobile.matches) label.tabIndex = 0;
      });
    };
    const syncDrawer = () => {
      if (!panel) {
        if (drawerButton) drawerButton.hidden = true;
        return;
      }
      if (drawerButton) drawerButton.hidden = false;
      drawerButton?.setAttribute('aria-expanded', String(Boolean(drawer?.checked)));
      drawerButton?.setAttribute('aria-label', drawer?.checked ? 'Close site navigation' : 'Open site navigation');
      panel.inert = mobile.matches && !drawer?.checked;
      if (mobile.matches && drawer?.checked) {
        panel.setAttribute('role', 'dialog');
        panel.setAttribute('aria-modal', 'true');
        panel.setAttribute('aria-label', 'Site navigation');
      } else {
        panel.removeAttribute('role');
        panel.removeAttribute('aria-modal');
        panel.removeAttribute('aria-label');
      }
      syncBranchState();
    };
    const syncSearch = () => {
      searchButtons.forEach((button) => {
        button.setAttribute('aria-expanded', String(Boolean(searchToggle?.checked)));
        button.setAttribute('aria-label', searchToggle?.checked ? 'Close search' : 'Search SimPaths');
      });
    };
    const closeDrawer = () => {
      setToggle(drawer, false);
      focusAfterUpdate(drawerButton);
    };
    const closeSearch = () => {
      setToggle(searchToggle, false);
      focusAfterUpdate(searchOpener && visible(searchOpener) ? searchOpener : searchButtons.find(visible));
    };

    listen(drawerButton, 'click', () => {
      if (drawer.checked) closeDrawer();
      else {
        setToggle(searchToggle, false);
        // Standalone records have singleton sections in MkDocs navigation.
        // Their active subpanel contains no useful links: open the site level.
        if (document.documentElement.classList.contains('sp-no-section-nav')) {
          primaryNav?.querySelectorAll(':scope > .md-nav__list > .md-nav__item > .md-nav__toggle:checked')
            .forEach((toggle) => setToggle(toggle, false));
        }
        setToggle(drawer, true);
        focusAfterUpdate(drawerTargets()[0]);
      }
    });
    searchButtons.forEach((button) => listen(button, 'click', () => {
      if (searchToggle.checked) closeSearch();
      else {
        searchOpener = button;
        setToggle(drawer, false);
        setToggle(searchToggle, true);
        focusAfterUpdate(searchInput);
      }
    }));
    listen(closeButton, 'click', closeDrawer);
    listen(document.querySelector('.md-overlay[for="__drawer"]'), 'click', () => focusAfterUpdate(drawerButton));
    listen(search.querySelector('.md-search__overlay'), 'click', () => focusAfterUpdate(searchOpener));
    listen(drawer, 'change', syncDrawer);
    listen(searchToggle, 'change', syncSearch);
    listen(panel, 'click', (event) => {
      const button = event.target.closest('[data-sp-nav-back]');
      if (!button) return;
      setToggle(document.getElementById(button.dataset.spNavBack), false);
      focusAfterUpdate(drawerTargets()[0]);
    });
    listen(panel, 'change', (event) => {
      if (!event.target.matches('.md-nav__toggle')) return;
      syncBranchState();
      if (mobile.matches && drawer.checked) focusAfterUpdate(drawerTargets()[0]);
    });
    listen(document, 'keydown', (event) => {
      if (event.key === 'Escape' && (drawer.checked || searchToggle.checked)) {
        event.preventDefault();
        event.stopPropagation();
        if (searchToggle.checked) closeSearch();
        else closeDrawer();
      } else if (event.key === 'Tab' && mobile.matches && drawer.checked) {
        const targets = drawerTargets();
        const index = targets.indexOf(document.activeElement);
        const next = index < 0 ? (event.shiftKey ? targets.length - 1 : 0)
          : (index + targets.length + (event.shiftKey ? -1 : 1)) % targets.length;
        event.preventDefault();
        targets[next]?.focus();
      } else if (event.key === ' ' && event.target.matches('label.md-nav__link[role="button"]')) {
        event.preventDefault();
        event.target.click();
      }
    }, { capture: true });
    listen(mobile, 'change', () => {
      if (!mobile.matches) setToggle(drawer, false);
      syncDrawer();
    });
    syncDrawer();
    syncSearch();
    dispose = () => {
      events.abort();
      cancelAnimationFrame(focusFrame);
      if (panel) panel.inert = false;
    };
  };

  if (typeof document$ !== 'undefined') document$.subscribe(setup);
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', setup, { once: true });
  else setup();
})();
