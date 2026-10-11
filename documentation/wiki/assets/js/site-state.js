(() => {
  const managedBodyClasses = [
    "sp-page-home",
    "sp-page-docs-index",
    "sp-page-directory",
    "sp-page-funding",
    "sp-page-roadmap",
    "sp-page-validation",
    "sp-no-section-nav",
    "sp-no-toc",
    "sp-tab-model",
    "sp-tab-documentation"
  ];

  let searchToggle = null;
  let updateSearchState = null;

  const setSearchState = () => {
    document.body.classList.toggle("sp-search-open", Boolean(searchToggle?.checked));
  };

  const bindSearchState = () => {
    if (searchToggle && updateSearchState) {
      searchToggle.removeEventListener("change", updateSearchState);
    }

    searchToggle = document.querySelector("#__search");
    updateSearchState = setSearchState;

    const searchInput = document.querySelector(".md-search__input");
    if (searchInput) {
      searchInput.placeholder = "Search SimPaths";
    }

    if (searchToggle) {
      searchToggle.addEventListener("change", updateSearchState);
    }

    setSearchState();
  };

  const markNavigationState = () => {
    document.querySelectorAll(".sp-nav-container-active").forEach((container) => {
      container.classList.remove("sp-nav-container-active");
    });
    document.querySelectorAll(".sp-modules-branch").forEach((branch) => {
      branch.classList.remove("sp-modules-branch");
    });

    document.querySelectorAll(".md-nav__container").forEach((container) => {
      const directActiveLink = Array.from(container.children).some(
        (child) => child.matches("a.md-nav__link--active")
      );
      container.classList.toggle("sp-nav-container-active", directActiveLink);
    });

    document
      .querySelectorAll('.md-sidebar--primary a[href*="/overview/modules/"]')
      .forEach((link) => {
        link
          .closest("li.md-nav__item--section.md-nav__item--nested")
          ?.classList.add("sp-modules-branch");
      });
  };

  const updateSiteState = () => {
    const body = document.body;
    const pageRoots = [document.documentElement, body];
    const pageState = document.querySelector("[data-sp-page-classes]");
    const classes = new Set((pageState?.dataset.spPageClasses || "").split(/\s+/));
    managedBodyClasses.forEach((className) => {
      pageRoots.forEach((root) => root.classList.toggle(className, classes.has(className)));
    });

    markNavigationState();
    bindSearchState();
  };

  if (typeof document$ !== "undefined") {
    document$.subscribe(updateSiteState);
  } else if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", updateSiteState, { once: true });
  } else {
    updateSiteState();
  }
})();
