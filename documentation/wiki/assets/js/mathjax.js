(() => {
  let loading = null;
  let queue = Promise.resolve();
  let renderedRoot = null;

  const loadMathJax = () => {
    if (window.MathJax?.typesetPromise) return Promise.resolve(window.MathJax);
    if (loading) return loading;

    window.MathJax = {
      loader: { load: ["[tex]/boldsymbol"] },
      tex: {
        packages: { "[+]": ["boldsymbol"] },
        inlineMath: [["\\(", "\\)"]],
        displayMath: [["\\[", "\\]"]],
        processEscapes: true
      },
      svg: { fontCache: "local" },
      options: { ignoreHtmlClass: ".*|", processHtmlClass: "arithmatex" },
      // This loader owns typesetting so a slow request cannot process a page
      // that has already been replaced by Material's instant navigation.
      startup: { typeset: false }
    };

    const script = document.createElement("script");
    script.src = "https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-svg.js";
    script.async = true;
    loading = new Promise((resolve, reject) => {
      script.onload = () => {
        Promise.resolve(window.MathJax?.startup?.promise).then(() => {
          if (window.MathJax?.typesetPromise) resolve(window.MathJax);
          else reject(new Error("MathJax did not initialise"));
        }, reject);
      };
      script.onerror = () => reject(new Error("MathJax could not be loaded"));
    }).catch((error) => {
      loading = null;
      script.remove();
      throw error;
    });
    document.head.append(script);
    return loading;
  };

  const typesetPage = () => {
    const root = document.querySelector(".md-content__inner");
    if (!root) return;
    // Clear detached math items even when the destination has no equations.
    queue = queue.then(async () => {
      if (!root.isConnected || root === renderedRoot) return;
      if (renderedRoot) {
        window.MathJax?.typesetClear?.([renderedRoot]);
        renderedRoot = null;
      }
      if (!root.querySelector(".arithmatex")) return;
      const math = await loadMathJax();
      if (!root.isConnected) return;
      await math.typesetPromise([root]);
      renderedRoot = root;
    }).catch((error) => {
      // The original TeX remains readable; a later navigation can retry loading.
      console.warn("SimPaths equation rendering:", error);
    });
  };

  if (typeof document$ !== "undefined") document$.subscribe(typesetPage);
  else if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", typesetPage, { once: true });
  } else typesetPage();
})();
