(function () {
  const COLLAPSE_OFFSET = 160;
  const SCROLL_DELTA = 10;

  let tabs = null;
  let lastY = 0;
  let ticking = false;

  function getTabs() {
    if (!tabs || !tabs.isConnected) {
      tabs = document.querySelector(".md-tabs");
    }
    return tabs;
  }

  function setCollapsed(collapsed) {
    const currentTabs = getTabs();
    if (!currentTabs) return;
    currentTabs.classList.toggle("sp-tabs-collapsed", collapsed);
  }

  function updateTabs() {
    const currentY = window.scrollY || document.documentElement.scrollTop || 0;

    if (currentY <= COLLAPSE_OFFSET) {
      setCollapsed(false);
      lastY = currentY;
      return;
    }

    if (currentY > lastY + SCROLL_DELTA) {
      setCollapsed(true);
    } else if (currentY < lastY - SCROLL_DELTA) {
      setCollapsed(false);
    }

    lastY = currentY;
  }

  function onScroll() {
    if (ticking) return;
    ticking = true;
    requestAnimationFrame(() => {
      ticking = false;
      updateTabs();
    });
  }

  function initTabsAutohide() {
    tabs = document.querySelector(".md-tabs");
    lastY = window.scrollY || document.documentElement.scrollTop || 0;
    updateTabs();
  }

  window.addEventListener("scroll", onScroll, { passive: true });
  window.addEventListener("resize", onScroll, { passive: true });

  if (typeof document$ !== "undefined" && document$.subscribe) {
    document$.subscribe(() => {
      requestAnimationFrame(initTabsAutohide);
    });
  } else {
    document.addEventListener("DOMContentLoaded", initTabsAutohide, { once: true });
  }
})();
