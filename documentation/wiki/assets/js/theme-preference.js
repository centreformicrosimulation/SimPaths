(() => {
  // Delegate to Material's palette radios: they own persistence, OS changes,
  // first-paint restoration and the colour-scheme/theme-color metadata.
  const media = {
    system: "(prefers-color-scheme)",
    light: "(prefers-color-scheme: light)",
    dark: "(prefers-color-scheme: dark)"
  };
  const mounted = new WeakSet();

  const setup = () => {
    const control = document.querySelector(".sp-theme-control");
    const select = control?.querySelector("select");
    if (!select) return;
    const activeMedia = document.body.dataset.mdColorMedia;
    select.value = Object.keys(media).find(key => media[key] === activeMedia) || "system";
    control.hidden = false;
    if (mounted.has(select)) return;
    mounted.add(select);
    select.addEventListener("change", () => {
      const input = document.querySelector(`[data-md-component="palette"] input[data-md-color-media="${media[select.value]}"]`);
      input?.click();
    });
  };

  if (typeof document$ !== "undefined") document$.subscribe(setup);
  else if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", setup, { once: true });
  else setup();
})();
