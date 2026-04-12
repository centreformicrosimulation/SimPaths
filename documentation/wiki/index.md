---
hide:
  - navigation
  - toc
---

<style>
  .md-main__inner:has(.simpaths-home-hero) {
    margin-top: 0;
  }

  .md-content:has(.simpaths-home-hero) .md-content__inner {
    padding-top: 0;
  }

  .md-content:has(.simpaths-home-hero) .md-content__inner::before {
    display: none;
  }

  .md-main:has(.simpaths-home-hero) .md-top {
    display: none !important;
  }

  .simpaths-home-hero {
    position: relative;
    isolation: isolate;
    overflow: hidden;
    width: 100vw;
    margin: 0 calc(50% - 50vw) 2.45rem;
    padding: 2.48rem 1.2rem 2.48rem;
    background:
      linear-gradient(180deg, rgba(250, 249, 245, 0.04), rgba(250, 249, 245, 0.012) 42%, rgba(4, 10, 16, 0.16)),
      linear-gradient(90deg, rgba(8, 14, 22, 0.98) 0%, rgba(13, 20, 30, 0.92) 48%, rgba(23, 22, 34, 0.96) 100%),
      linear-gradient(116deg, #080d13 0%, #111b27 52%, #241f2c 100%);
    border-top: 1px solid rgba(250, 249, 245, 0.1);
    border-bottom: 1px solid rgba(250, 249, 245, 0.1);
    box-shadow:
      inset 0 1px 0 rgba(250, 249, 245, 0.1),
      inset 0 -1px 0 rgba(4, 10, 16, 0.5);
  }

  .simpaths-home-hero::before {
    content: "";
    position: absolute;
    inset: 0;
    z-index: 0;
    pointer-events: none;
    background: linear-gradient(180deg, rgba(250, 249, 245, 0.035) 0%, transparent 34%, rgba(4, 10, 16, 0.18) 100%);
  }

  .simpaths-home-hero__inner {
    position: relative;
    z-index: 1;
    max-width: 43rem;
    margin: 0 auto;
    padding: 0.3rem 0;
    text-align: center;
  }

  .simpaths-home-hero__brand {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0;
    margin-bottom: 1rem;
  }

  .simpaths-home-hero__logo {
    display: block;
    width: clamp(6.2rem, 10.8vw, 7.35rem);
    height: auto;
    margin: 0 auto 0.68rem;
    filter: saturate(1.16) brightness(1.08) contrast(1.06) drop-shadow(0 16px 26px rgba(14, 22, 34, 0.24));
  }

  .md-typeset .simpaths-home-hero h1.simpaths-home-hero__title {
    margin: 0 !important;
    font-family: var(--md-text-font), -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    color: #ffffff;
    font-size: clamp(1.98rem, 3.95vw, 2.74rem) !important;
    font-weight: 640;
    letter-spacing: -0.045em;
    line-height: 0.95;
    padding-bottom: 0 !important;
    margin-bottom: 0 !important;
    border-bottom: none !important;
    position: relative;
    text-shadow: 0 12px 28px rgba(0, 0, 0, 0.3);
  }

  .md-typeset .simpaths-home-hero h1.simpaths-home-hero__title::after {
    content: "";
    display: block !important;
    width: 3.8rem;
    height: 2px;
    margin: 0.42rem auto 0;
    border-radius: 999px;
    background: linear-gradient(90deg, rgba(250, 249, 245, 0.18), rgba(250, 249, 245, 0.72), rgba(250, 249, 245, 0.18));
  }

  .md-typeset .simpaths-home-hero p.simpaths-home-hero__strap {
    margin: 0.58rem 0 0 !important;
    color: rgba(255, 255, 255, 0.9);
    font-size: 0.62rem;
    font-weight: 650;
    letter-spacing: 0.195em;
    line-height: 1.2;
    text-transform: uppercase;
    text-align: center !important;
    text-shadow:
      0 0 16px rgba(151, 190, 255, 0.22),
      0 1px 10px rgba(0, 0, 0, 0.32);
    padding-bottom: 0 !important;
  }

  .md-typeset .simpaths-home-hero p.simpaths-home-hero__copy {
    margin: 0 auto;
    max-width: 42rem;
    color: rgba(255, 255, 255, 0.93);
    font-size: 0.83rem;
    line-height: 1.52;
    text-align: center !important;
    text-shadow: 0 1px 18px rgba(0, 0, 0, 0.28);
    word-spacing: normal;
    letter-spacing: normal;
    hyphens: none !important;
    -webkit-hyphens: none !important;
    text-wrap: unset;
  }

  .md-typeset .simpaths-home-hero p.simpaths-home-hero__copy + p.simpaths-home-hero__copy {
    margin-top: 0.34rem;
  }

  .simpaths-home-hero__countries {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.42rem;
    margin: 0.82rem 0 0.82rem;
  }

  .simpaths-home-hero__country {
    position: relative;
    display: inline-flex;
    align-items: center;
    gap: 0.34rem;
    overflow: hidden;
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.07)),
      rgba(10, 18, 29, 0.62);
    color: rgba(255, 255, 255, 0.98);
    border: 1px solid rgba(255, 255, 255, 0.28);
    border-radius: 4px;
    padding: 0.2rem 0.62rem;
    font-size: 0.67rem;
    font-weight: 640;
    letter-spacing: 0.005em;
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.2),
      inset 0 -1px 0 rgba(4, 10, 16, 0.24),
      0 10px 22px rgba(4, 10, 16, 0.2);
    text-shadow: 0 1px 10px rgba(0, 0, 0, 0.24);
    backdrop-filter: blur(14px) saturate(1.12);
    -webkit-backdrop-filter: blur(14px) saturate(1.12);
  }

  .simpaths-home-hero__country::after {
    position: absolute;
    right: 0.38rem;
    bottom: 0.18rem;
    left: 0.38rem;
    height: 1px;
    content: "";
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.32), transparent);
    opacity: 0.65;
  }

  @media (max-width: 700px) {
    .simpaths-home-hero {
      padding: 1.65rem 1rem 1.65rem;
    }

    .simpaths-home-hero__logo {
      width: 5rem;
    }

    .md-typeset .simpaths-home-hero h1.simpaths-home-hero__title {
      font-size: clamp(1.86rem, 6.7vw, 2.25rem) !important;
    }

    .md-typeset .simpaths-home-hero p.simpaths-home-hero__strap {
      font-size: 0.55rem;
      letter-spacing: 0.11em;
    }

    .md-typeset .simpaths-home-hero p.simpaths-home-hero__copy {
      max-width: 34rem;
      font-size: 0.8rem;
    }
  }

  .simpaths-home-explore {
    margin: 0.4rem 0 0;
    padding-bottom: 3.1rem;
  }

  .simpaths-home-explore__intro {
    margin-bottom: 1.5rem;
  }

  .md-typeset .simpaths-home-explore .section-heading {
    margin-bottom: 0.18rem;
    font-size: 1.7rem;
  }

  .md-typeset .simpaths-home-explore .section-heading::after {
    display: none !important;
  }

  .md-typeset .simpaths-home-explore .card-grid {
    display: grid;
    gap: 1.28rem;
    margin: 0;
  }

  .md-typeset .simpaths-home-explore .card-grid--primary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin-bottom: 1.28rem;
  }

  .md-typeset .simpaths-home-explore .card-grid--secondary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .md-typeset .simpaths-home-explore .card-grid--primary .feature-card--documentation {
    grid-column: span 2;
  }

  .md-typeset .simpaths-home-explore a.feature-card {
    --card-accent: #3d5d82;
    --card-accent-soft: rgba(61, 93, 130, 0.12);
    --card-surface: #F2F6FF;
    --card-border: rgba(42, 56, 72, 0.12);
    position: relative;
    display: grid;
    grid-template-rows: auto 1fr auto;
    gap: 0.96rem;
    min-height: 14.1rem;
    padding: 1.58rem 1.54rem 1.42rem;
    border-radius: 12px;
    overflow: hidden;
    border: 1px solid var(--card-border);
    background: var(--card-surface);
    box-shadow: 0 12px 24px rgba(19, 27, 38, 0.045);
    text-decoration: none !important;
    border-bottom: none !important;
    color: inherit !important;
    transition:
      transform 0.24s ease,
      box-shadow 0.24s ease,
      border-color 0.24s ease,
      background-color 0.24s ease;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary {
    grid-template-columns: minmax(10rem, 0.9fr) minmax(0, 1.12fr);
    grid-template-areas:
      "header copy"
      "footer footer";
    gap: 1.02rem 1.42rem;
    min-height: 13.8rem;
    padding: 1.68rem 1.74rem 1.54rem;
    background: linear-gradient(145deg, #1d2a37 0%, #26384a 100%);
    border-color: rgba(19, 27, 38, 0.26);
    box-shadow: 0 18px 32px rgba(17, 25, 36, 0.15);
  }

  .md-typeset .simpaths-home-explore a.feature-card::before,
  .md-typeset .simpaths-home-explore a.feature-card::after {
    content: none !important;
    display: none !important;
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover {
    transform: translateY(-3px);
    border-color: rgba(42, 56, 72, 0.2);
    box-shadow: 0 18px 30px rgba(19, 27, 38, 0.085);
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary:hover {
    border-color: rgba(19, 27, 38, 0.34);
    box-shadow: 0 24px 40px rgba(17, 25, 36, 0.2);
  }

  .md-typeset .simpaths-home-explore .card-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 0.96rem;
    margin: 0;
  }

  .md-typeset .simpaths-home-explore a.feature-card:not(.feature-card--primary) .card-header {
    flex-direction: column;
    justify-content: flex-start;
    align-items: flex-start;
    gap: 0.7rem;
  }

  .md-typeset .simpaths-home-explore .card-label {
    min-width: 0;
    order: 1;
  }

  .md-typeset .simpaths-home-explore a.feature-card:not(.feature-card--primary) .card-label,
  .md-typeset .simpaths-home-explore a.feature-card:not(.feature-card--primary) .card-icon-wrap {
    order: 0;
  }

  .md-typeset .simpaths-home-explore .card-icon-wrap {
    order: 2;
    display: flex;
    align-items: center;
    justify-content: center;
    width: auto;
    height: auto;
    flex: 0 0 auto;
    margin-bottom: 0;
    color: var(--card-accent);
    line-height: 1;
    transition: color 0.22s ease, transform 0.22s ease;
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover .card-icon-wrap {
    transform: translateY(-1px);
  }

  .md-typeset .simpaths-home-explore .card-icon-wrap svg {
    width: 20px;
    height: 20px;
  }

  .md-typeset .simpaths-home-explore .feature-card--model {
    --card-accent: #3d5d82;
    --card-accent-soft: rgba(61, 93, 130, 0.12);
    --card-surface: #F2F6FF;
    --card-border: rgba(61, 93, 130, 0.16);
  }

  .md-typeset .simpaths-home-explore .feature-card--documentation {
    --card-accent: rgba(250, 249, 245, 0.96);
    --card-accent-soft: rgba(250, 249, 245, 0.08);
  }

  .md-typeset .simpaths-home-explore .feature-card--validation {
    --card-accent: #4a697b;
    --card-accent-soft: rgba(74, 105, 123, 0.12);
    --card-surface: #F2F6FF;
    --card-border: rgba(74, 105, 123, 0.16);
  }

  .md-typeset .simpaths-home-explore .feature-card--research {
    --card-accent: #495b79;
    --card-accent-soft: rgba(73, 91, 121, 0.12);
    --card-surface: #F2F6FF;
    --card-border: rgba(73, 91, 121, 0.16);
  }

  .md-typeset .simpaths-home-explore .feature-card--funding {
    --card-accent: #7a6645;
    --card-accent-soft: rgba(122, 102, 69, 0.12);
    --card-surface: #F2F6FF;
    --card-border: rgba(122, 102, 69, 0.16);
  }

  .md-typeset .simpaths-home-explore a.feature-card h3 {
    margin: 0 !important;
    font-family: var(--sp-heading-font), Georgia, serif !important;
    font-size: 1.22rem !important;
    font-weight: 700 !important;
    color: #1e2c3a !important;
    line-height: 1;
    letter-spacing: -0.015em;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary h3 {
    font-size: 1.52rem !important;
    color: #ffffff !important;
  }

  .md-typeset .simpaths-home-explore a.feature-card p {
    margin: 0;
    color: rgba(42, 56, 72, 0.76);
    font-size: 0.77rem;
    line-height: 1.58;
    max-width: 29ch;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary p {
    grid-area: copy;
    align-self: start;
    max-width: none;
    color: rgba(250, 249, 245, 0.8);
    font-size: 0.77rem;
    line-height: 1.6;
  }

  .md-typeset .simpaths-home-explore a.feature-card .card-link {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    gap: 0.5rem;
    margin-top: 0.12rem;
    padding-top: 0.96rem;
    border-top: 1px solid rgba(42, 56, 72, 0.1);
    color: var(--card-accent);
    font-size: 0.7rem;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: none;
    transition: color 0.22s ease, letter-spacing 0.22s ease;
  }

  .md-typeset .simpaths-home-explore a.feature-card .card-link::after {
    content: "\2192";
    display: inline-grid;
    place-items: center;
    color: currentColor;
    transform: translateX(0);
    transition: transform 0.22s ease;
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover .card-link::after {
    transform: translateX(5px);
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover .card-link {
    color: var(--sp-midnight-deep);
    letter-spacing: 0.065em;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary .card-header {
    grid-area: header;
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
    gap: 0;
    padding-right: 0.92rem;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary .card-label,
  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary .card-icon-wrap {
    order: 0;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary p {
    padding-left: 1.42rem;
    border-left: 1px solid rgba(250, 249, 245, 0.14);
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary .card-link {
    grid-area: footer;
    align-self: end;
    border-top-color: rgba(250, 249, 245, 0.14);
    color: #ffffff;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary:hover .card-link {
    color: #ffffff;
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore .section-heading::after {
    display: none !important;
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card {
    background: linear-gradient(180deg, rgba(22, 32, 44, 0.96) 0%, rgba(18, 27, 38, 0.96) 100%);
    border-color: rgba(250, 249, 245, 0.08);
    box-shadow: 0 14px 28px rgba(0, 0, 0, 0.18);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card:hover {
    border-color: rgba(250, 249, 245, 0.16);
    box-shadow: 0 18px 34px rgba(0, 0, 0, 0.24);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary {
    background: linear-gradient(145deg, #253546 0%, #304559 100%);
    border-color: rgba(250, 249, 245, 0.12);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore .card-icon-wrap {
    border-color: rgba(250, 249, 245, 0.1);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card h3 {
    color: #edf3f8 !important;
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card p {
    color: rgba(250, 249, 245, 0.72);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card .card-link {
    border-top-color: rgba(250, 249, 245, 0.1);
    color: rgba(250, 249, 245, 0.94);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card:hover .card-link {
    color: #ffffff;
  }

  @media (max-width: 900px) {
    .md-typeset .simpaths-home-explore .card-grid--primary {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .md-typeset .simpaths-home-explore .card-grid--primary .feature-card--documentation {
      grid-column: span 2;
    }

    .md-typeset .simpaths-home-explore .card-grid--secondary {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary {
      grid-template-columns: minmax(9.4rem, 0.9fr) minmax(0, 1.08fr);
    }
  }

  @media (max-width: 760px) {
    .md-typeset .simpaths-home-explore .card-grid--primary,
    .md-typeset .simpaths-home-explore .card-grid--secondary {
      grid-template-columns: 1fr;
    }

    .md-typeset .simpaths-home-explore .card-grid--primary .feature-card--documentation {
      grid-column: auto;
    }

    .md-typeset .simpaths-home-explore a.feature-card,
    .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary {
      min-height: auto;
    }

    .md-typeset .simpaths-home-explore a.feature-card {
      padding: 1.34rem 1.28rem 1.18rem;
      gap: 0.86rem;
    }

    .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary {
      grid-template-columns: 1fr;
      grid-template-areas:
        "header"
        "copy"
        "footer";
      gap: 0.94rem;
      padding: 1.5rem 1.42rem 1.28rem;
    }

    .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary .card-header {
      gap: 0.74rem;
      padding-right: 0;
      border-right: none;
    }

    .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary p {
      padding-left: 0;
      border-left: none;
    }

    .md-typeset .simpaths-home-explore a.feature-card h3 {
      font-size: 1.06rem !important;
    }

    .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary h3 {
      font-size: 1.22rem !important;
    }

    .md-typeset .simpaths-home-explore a.feature-card p {
      max-width: none;
      font-size: 0.76rem;
    }
  }

  .simpaths-home-research-band {
    margin: 0 calc(50% - 50vw);
    padding: 3.35rem 1.2rem 3.5rem;
    background: rgba(255, 255, 255, 0.98);
    border-top: 1px solid rgba(42, 56, 72, 0.06);
  }

  .md-typeset .simpaths-home-research-band .research-section {
    max-width: 60rem;
    margin: 0 auto;
  }

  .md-typeset .simpaths-home-research-band .research-header {
    align-items: flex-end;
    margin-bottom: 1.1rem;
  }

  .md-typeset .simpaths-home-research-band .research-header .section-heading {
    margin-bottom: 0;
    font-size: 1.62rem;
  }

  .md-typeset .simpaths-home-research-band .research-header .section-heading::after {
    background: linear-gradient(90deg, rgba(42, 56, 72, 0.12) 0%, rgba(42, 56, 72, 0.06) 28%, transparent 100%);
  }

  .md-typeset .simpaths-home-research-band .archive-link {
    opacity: 0.48;
  }

  .md-typeset .simpaths-home-research-band .research-list {
    margin-top: 0.1rem;
  }

  .md-typeset .simpaths-home-research-band a.research-entry {
    gap: 0 1.4rem;
    padding: 1.3rem 0;
    border-bottom-color: rgba(42, 56, 72, 0.08);
    transition: transform 0.18s ease, border-color 0.18s ease;
  }

  .md-typeset .simpaths-home-research-band a.research-entry:first-child {
    border-top-color: rgba(42, 56, 72, 0.08);
  }

  .md-typeset .simpaths-home-research-band a.research-entry:hover {
    padding-left: 0;
    transform: translateX(0.14rem);
    border-bottom-color: rgba(42, 56, 72, 0.14);
  }

  .md-typeset .simpaths-home-research-band .research-entry__meta {
    gap: 0.9rem;
    margin-bottom: 0.38rem;
  }

  .md-typeset .simpaths-home-research-band .research-label {
    letter-spacing: 0.14em;
    color: rgba(96, 56, 71, 0.88);
  }

  .md-typeset .simpaths-home-research-band .research-journal {
    font-size: 0.64rem;
    color: rgba(42, 56, 72, 0.56);
  }

  .md-typeset .simpaths-home-research-band a.research-entry h3 {
    font-size: 1.05rem;
    line-height: 1.38;
  }

  .md-typeset .simpaths-home-research-band .research-authors {
    margin-top: 0.26rem !important;
    font-size: 0.72rem;
  }

  .md-typeset .simpaths-home-research-band .research-summary {
    max-width: 43rem;
    margin-top: 0.4rem !important;
    font-size: 0.78rem;
    line-height: 1.64;
  }

  .md-typeset .simpaths-home-research-band .research-arrow {
    font-size: 1rem;
    color: rgba(42, 56, 72, 0.18);
  }

  @media (max-width: 700px) {
    .simpaths-home-research-band {
      padding: 2.5rem 1rem 2.7rem;
    }
  }
</style>

<div class="simpaths-home-hero">
<div class="simpaths-home-hero__inner">
  <div class="simpaths-home-hero__brand">
    <img src="assets/images/homepage-hero-logo-dark.svg?v=1" alt="SimPaths logo showing people progressing across a rising path." class="simpaths-home-hero__logo">
    <h1 class="simpaths-home-hero__title">SimPaths</h1>
    <p class="simpaths-home-hero__strap">Life Course Microsimulation</p>
  </div>

  <p class="simpaths-home-hero__copy">SimPaths is a family of open-source models for individual and household life course events, designed to project life histories through time, building up a detailed picture of career paths, family (inter)relations, health, and financial circumstances.</p>

  <div class="simpaths-home-hero__countries">
    <span class="simpaths-home-hero__country">🇬🇧 United Kingdom</span>
    <span class="simpaths-home-hero__country">🇬🇷 Greece</span>
    <span class="simpaths-home-hero__country">🇭🇺 Hungary</span>
    <span class="simpaths-home-hero__country">🇮🇹 Italy</span>
    <span class="simpaths-home-hero__country">🇵🇱 Poland</span>
  </div>

</div>
</div>

<section class="simpaths-home-explore">
<div class="simpaths-home-explore__intro">
  <div>
    <h2 class="section-heading">Explore SimPaths</h2>
  </div>
</div>

<div class="card-grid card-grid--primary">

<a href="overview/" class="feature-card feature-card--model">
<div class="card-header">
  <div class="card-icon-wrap">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
  </div>
  <div class="card-label">
    <h3>Model</h3>
  </div>
</div>
<p>See what SimPaths simulates, the core entities, and how the system fits together.</p>
<span class="card-link">Explore model</span>
</a>

<a href="documentation/" class="feature-card feature-card--primary feature-card--documentation">
<div class="card-header">
  <div class="card-label">
    <h3>Documentation</h3>
  </div>
</div>
<p>Go from setup and your first run to user guides, developer internals, and the core reference material for the model.</p>
<span class="card-link">Open documentation</span>
</a>

</div>

<div class="card-grid card-grid--secondary">

<a href="validation/" class="feature-card feature-card--validation">
<div class="card-header">
  <div class="card-icon-wrap">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
  </div>
  <div class="card-label">
    <h3>Validation</h3>
  </div>
</div>
<p>Inspect how outputs are checked, benchmarked, and compared against external evidence.</p>
<span class="card-link">View validation</span>
</a>

<a href="research/" class="feature-card feature-card--research">
<div class="card-header">
  <div class="card-icon-wrap">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 20V10"/><path d="M12 20V4"/><path d="M6 20v-6"/></svg>
  </div>
  <div class="card-label">
    <h3>Research</h3>
  </div>
</div>
<p>Browse papers, applications, and active projects that use SimPaths.</p>
<span class="card-link">Browse research</span>
</a>

<a href="funding/" class="feature-card feature-card--funding">
<div class="card-header">
  <div class="card-icon-wrap">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10h18"/><path d="M5 10v8"/><path d="M9 10v8"/><path d="M15 10v8"/><path d="M19 10v8"/><path d="M2 18h20"/><path d="M12 2l10 5H2l10-5z"/></svg>
  </div>
  <div class="card-label">
    <h3>Funding</h3>
  </div>
</div>
<p>See the grants, programmes, and institutions that supported development.</p>
<span class="card-link">See funding</span>
</a>

</div>
</section>

<section class="simpaths-home-research-band">
<div class="research-section">
<div class="research-header">
  <h2 class="section-heading">Recent Research Highlights</h2>
  <a href="research/" class="archive-link">ALL PUBLICATIONS →</a>
</div>

<div class="research-list">

<a href="https://doi.org/10.1093/eurpub/ckaf161.076" class="research-entry">
  <div class="research-entry__meta">
    <span class="research-label">MENTAL HEALTH · 2025</span>
    <span class="research-journal">European Journal of Public Health</span>
  </div>
  <h3>Tax reforms vs benefit enhancement to address mental health inequalities: a microsimulation study</h3>
  <p class="research-authors">Igelstrom E, Kopasker D, Richiardi MG, Katikireddi SV</p>
  <p class="research-summary">Compares alternative fiscal policy responses and their implications for mental health inequalities.</p>
  <span class="research-arrow">→</span>
</a>

<a href="https://www.sciencedirect.com/science/article/pii/S0167268125000319" class="research-entry">
  <div class="research-entry__meta">
    <span class="research-label">LIFE COURSE · 2025</span>
    <span class="research-journal">Journal of Economic Behavior &amp; Organization</span>
  </div>
  <h3>Attenuation and reinforcement mechanisms over the life course</h3>
  <p class="research-authors">Richiardi M, Bronka P, van de Ven J</p>
  <p class="research-summary">Examines how early advantage and disadvantage can compound or soften across the life course.</p>
  <span class="research-arrow">→</span>
</a>

<a href="https://journals.plos.org/plosmedicine/article?id=10.1371/journal.pmed.1004358" class="research-entry">
  <div class="research-entry__meta">
    <span class="research-label">HEALTH POLICY · 2024</span>
    <span class="research-journal">PLOS Medicine</span>
  </div>
  <h3>Short-term impacts of Universal Basic Income on population mental health inequalities in the UK</h3>
  <p class="research-authors">Thomson RM, Kopasker D, Bronka P, et al.</p>
  <p class="research-summary">Uses microsimulation to estimate how Universal Basic Income could affect mental health across the UK population.</p>
  <span class="research-arrow">→</span>
</a>

</div>
</div>
</section>
