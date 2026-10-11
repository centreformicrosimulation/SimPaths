(() => {
  let disposeSidebarViewport = null;
  let mountedRootList = null;
  let mountedPage = null;
  let mountedInput = null;

  const getDirectLabel = (item) => {
    const label = item.querySelector(
      ":scope > a.md-nav__link > .md-ellipsis, " +
      ":scope > .md-nav__container > a.md-nav__link > .md-ellipsis, " +
      ":scope > label.md-nav__link > .md-ellipsis"
    );
    return label ? label.textContent.trim().toLocaleLowerCase() : "";
  };

  const getChildItems = (item) => {
    const childNav = Array.from(item.children).find(
      (child) => child.matches("nav.md-nav:not(.md-nav--secondary)")
    );
    if (!childNav) return [];

    const childList = Array.from(childNav.children).find(
      (child) => child.matches("ul.md-nav__list")
    );
    return childList
      ? Array.from(childList.children).filter((child) => child.matches("li.md-nav__item"))
      : [];
  };

  const showSubtree = (item) => {
    item.classList.remove("sp-sidebar-filter-hidden");
    getChildItems(item).forEach(showSubtree);
  };

  const setBranchOpen = (item, open) => {
    const toggle = Array.from(item.children).find(
      (child) => child.matches("input.md-nav__toggle")
    );
    if (!toggle) return;

    if (open && !toggle.checked) {
      toggle.dataset.spSidebarFilterOpened = "true";
      toggle.checked = true;
    } else if (!open && toggle.dataset.spSidebarFilterOpened) {
      toggle.checked = false;
      delete toggle.dataset.spSidebarFilterOpened;
    }
  };

  const resetTree = (items) => {
    items.forEach((item) => {
      item.classList.remove("sp-sidebar-filter-hidden");
      setBranchOpen(item, false);
      resetTree(getChildItems(item));
    });
  };

  const filterItem = (item, query) => {
    const children = getChildItems(item);
    const ownMatch = getDirectLabel(item).includes(query);

    if (ownMatch) {
      showSubtree(item);
      setBranchOpen(item, children.length > 0);
      return true;
    }

    const childMatches = children.map((child) => filterItem(child, query));
    const childMatch = childMatches.some(Boolean);
    item.classList.toggle("sp-sidebar-filter-hidden", !childMatch);
    setBranchOpen(item, childMatch);
    return childMatch;
  };

  const setupSidebar = () => {
    const activeTab = document.querySelector(".md-tabs__item--active .md-tabs__link");
    const sidebarPanel = document.querySelector(".md-sidebar--primary");
    const sidebar = sidebarPanel?.querySelector(".md-sidebar__inner");
    const primaryNav = sidebar?.querySelector("nav.md-nav--primary");
    const documentationNav = primaryNav?.querySelector(
      ":scope > .md-nav__list > .md-nav__item--active.md-nav__item--section > nav.md-nav"
    );
    const rootList = documentationNav?.querySelector(":scope > .md-nav__list");
    const isDocumentation = activeTab?.textContent.trim() === "Documentation";

    const pageKey = location.pathname + location.search;
    const samePage = mountedPage === pageKey;
    // An anchor visit can replace Material's navigation tree. Preserve the
    // query by page identity even when the filter itself has to be rebuilt.
    if (isDocumentation && samePage && rootList && rootList === mountedRootList) return;
    const previousQuery = samePage ? mountedInput?.value || "" : "";

    disposeSidebarViewport?.();
    disposeSidebarViewport = null;
    mountedRootList = null;
    mountedInput = null;
    mountedPage = pageKey;
    document.body.classList.remove("sp-docs-navigation");
    if (!isDocumentation || !sidebarPanel || !sidebar || !rootList) return;

    mountedRootList = rootList;

    document.body.classList.add("sp-docs-navigation");
    sidebarPanel.querySelector(".sp-sidebar-tools")?.remove();

    const tools = document.createElement("div");
    tools.className = "sp-sidebar-tools";
    tools.innerHTML = `
      <label class="sp-sidebar-filter">
        <svg class="sp-sidebar-filter__icon" viewBox="0 0 24 24" aria-hidden="true">
          <path fill="currentColor" d="M4 5.5h16v1.7H4V5.5Zm3 5.65h10v1.7H7v-1.7Zm3 5.65h4v1.7h-4v-1.7Z"/>
        </svg>
        <input class="sp-sidebar-filter__input" type="search" placeholder="Filter pages" aria-label="Filter documentation pages" autocomplete="off">
        <button class="sp-sidebar-filter__clear" type="button" aria-label="Clear sidebar filter">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="m6.4 5.2 5.6 5.6 5.6-5.6 1.2 1.2-5.6 5.6 5.6 5.6-1.2 1.2-5.6-5.6-5.6 5.6-1.2-1.2 5.6-5.6-5.6-5.6 1.2-1.2Z"/></svg>
        </button>
      </label>
      <span class="sp-sidebar-filter__status" role="status" aria-live="polite">No matching pages</span>
    `;
    // Share the navigation's inner width, including on systems with scrollbars.
    // CSS keeps this filter sticky while the navigation below it scrolls.
    sidebar.prepend(tools);

    const input = tools.querySelector(".sp-sidebar-filter__input");
    mountedInput = input;
    input.value = previousQuery;
    const clear = tools.querySelector(".sp-sidebar-filter__clear");
    const status = tools.querySelector(".sp-sidebar-filter__status");
    const scrollwrap = sidebarPanel.querySelector(":scope > .md-sidebar__scrollwrap");
    const footer = document.querySelector(".md-footer");
    const main = document.querySelector(".md-main");
    const rootItems = Array.from(rootList.children).filter(
      (child) => child.matches("li.md-nav__item")
    );

    const desktop = window.matchMedia("(min-width: 76.25em)");
    let viewportFrame = 0;

    const updateSidebarViewport = () => {
      viewportFrame = 0;
      if (!desktop.matches || !scrollwrap) {
        sidebarPanel.classList.remove("sp-sidebar-viewport-ready");
        sidebarPanel.style.removeProperty("--sp-sidebar-viewport-height");
        return;
      }

      const scrollTop = scrollwrap.getBoundingClientRect().top;
      const footerTop = footer?.getBoundingClientRect().top ?? Number.POSITIVE_INFINITY;
      const lowerBoundary = Math.min(window.innerHeight - 24, footerTop - 24);
      const availableHeight = Math.max(120, Math.floor(lowerBoundary - scrollTop));

      sidebarPanel.style.setProperty(
        "--sp-sidebar-viewport-height",
        `${availableHeight}px`
      );
      sidebarPanel.classList.add("sp-sidebar-viewport-ready");
    };

    const scheduleSidebarViewport = () => {
      if (viewportFrame) return;
      viewportFrame = window.requestAnimationFrame(updateSidebarViewport);
    };

    window.addEventListener("scroll", scheduleSidebarViewport, { passive: true });
    window.addEventListener("resize", scheduleSidebarViewport, { passive: true });
    desktop.addEventListener("change", scheduleSidebarViewport);

    const resizeObserver = typeof ResizeObserver === "undefined"
      ? null
      : new ResizeObserver(scheduleSidebarViewport);
    [tools, main, footer].filter(Boolean).forEach((element) => resizeObserver?.observe(element));

    disposeSidebarViewport = () => {
      window.removeEventListener("scroll", scheduleSidebarViewport);
      window.removeEventListener("resize", scheduleSidebarViewport);
      desktop.removeEventListener("change", scheduleSidebarViewport);
      resizeObserver?.disconnect();
      if (viewportFrame) window.cancelAnimationFrame(viewportFrame);
      sidebarPanel.classList.remove("sp-sidebar-viewport-ready");
      sidebarPanel.style.removeProperty("--sp-sidebar-viewport-height");
    };

    scheduleSidebarViewport();

    const applyFilter = () => {
      const query = input.value.trim().toLocaleLowerCase();
      clear.dataset.visible = String(query.length > 0);

      if (!query) {
        resetTree(rootItems);
        status.dataset.visible = "false";
        scheduleSidebarViewport();
        return;
      }

      const rootMatches = rootItems.map((item) => filterItem(item, query));
      const hasMatch = rootMatches.some(Boolean);
      status.dataset.visible = String(!hasMatch);
      scheduleSidebarViewport();
    };

    input.addEventListener("input", applyFilter);
    clear.addEventListener("click", () => {
      input.value = "";
      applyFilter();
      input.focus();
    });
    applyFilter();
  };

  if (typeof document$ !== "undefined") {
    document$.subscribe(setupSidebar);
  } else if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", setupSidebar, { once: true });
  } else {
    setupSidebar();
  }
})();
