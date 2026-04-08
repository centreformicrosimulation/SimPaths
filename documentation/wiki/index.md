---
hide:
  - navigation
  - toc
---

<style>
  .simpaths-home-hero {
    width: 100vw;
    margin: -0.18rem calc(50% - 50vw) 2.2rem;
    padding: 1.82rem 1.2rem 1.8rem;
    background:
      linear-gradient(90deg, rgba(255, 220, 226, 0.92) 0%, rgba(243, 223, 255, 0.9) 46%, rgba(217, 236, 255, 0.94) 100%);
    border-top: none;
    border-bottom: 1px solid rgba(195, 198, 213, 0.46);
  }

  .simpaths-home-hero__inner {
    max-width: 43rem;
    margin: 0 auto;
    text-align: center;
  }

  .simpaths-home-hero__brand {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0;
    margin-bottom: 0.8rem;
  }

  .simpaths-home-hero__logo {
    display: block;
    width: clamp(5.1rem, 8.6vw, 5.7rem);
    height: auto;
    margin: 0 auto 0.08rem;
  }

  .md-typeset .simpaths-home-hero h1.simpaths-home-hero__title {
    margin: 0 !important;
    font-family: var(--sp-heading-font), Georgia, serif;
    color: var(--sp-midnight);
    font-size: clamp(2.05rem, 3.9vw, 2.55rem) !important;
    font-weight: 500;
    letter-spacing: 0.01em;
    line-height: 0.94;
    padding-bottom: 0 !important;
    margin-bottom: 0 !important;
    border-bottom: none !important;
    position: static;
  }

  .md-typeset .simpaths-home-hero h1.simpaths-home-hero__title::after {
    display: none !important;
  }

  .md-typeset .simpaths-home-hero p.simpaths-home-hero__strap {
    margin: 0.18rem 0 0 !important;
    color: #1f2731;
    font-size: 0.58rem;
    font-weight: 560;
    letter-spacing: 0.14em;
    line-height: 1.2;
    text-transform: uppercase;
    text-align: center !important;
    padding-bottom: 0 !important;
  }

  .md-typeset .simpaths-home-hero p.simpaths-home-hero__copy {
    margin: 0 auto;
    max-width: 42rem;
    color: #2f3844;
    font-size: 0.83rem;
    line-height: 1.52;
    text-align: center !important;
    word-spacing: normal;
    letter-spacing: normal;
    hyphens: none !important;
    -webkit-hyphens: none !important;
    text-wrap: unset;
  }

  .md-typeset .simpaths-home-hero p.simpaths-home-hero__copy + p.simpaths-home-hero__copy {
    margin-top: 0.34rem;
  }

  .simpaths-home-hero__copy strong {
    color: var(--sp-midnight);
  }

  .simpaths-home-hero__countries {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.42rem;
    margin: 0.82rem 0 0.82rem;
  }

  .simpaths-home-hero__country {
    display: inline-flex;
    align-items: center;
    gap: 0.3rem;
    background: rgba(250, 249, 245, 0.66);
    color: #4a5463;
    border: 1px solid rgba(195, 198, 213, 0.52);
    border-radius: 4px;
    padding: 0.16rem 0.5rem;
    font-size: 0.68rem;
    font-weight: 500;
    backdrop-filter: blur(6px);
  }

  .simpaths-home-hero__cta {
    display: flex;
    justify-content: center;
    gap: 0.65rem;
    flex-wrap: wrap;
    margin-top: 0.12rem;
  }

  .simpaths-home-hero__cta a {
    display: inline-flex;
    align-items: center;
    border-radius: 4px;
    padding: 0.46rem 0.98rem;
    font-size: 0.74rem;
    font-weight: 600;
    text-decoration: none !important;
    border-bottom: none !important;
    transition: var(--sp-transition);
  }

  .simpaths-home-hero__cta .simpaths-home-hero__btn--primary {
    background: var(--sp-midnight);
    color: var(--sp-paper) !important;
    box-shadow: 0 2px 8px rgba(42, 56, 72, 0.14);
  }

  .simpaths-home-hero__cta .simpaths-home-hero__btn--primary:hover {
    background: var(--sp-midnight-deep);
    transform: translateY(-1px);
  }

  .simpaths-home-hero__cta .simpaths-home-hero__btn--secondary {
    background: rgba(250, 249, 245, 0.52);
    color: var(--sp-midnight) !important;
    border: 1px solid rgba(195, 198, 213, 0.9) !important;
  }

  .simpaths-home-hero__cta .simpaths-home-hero__btn--secondary:hover {
    background: rgba(250, 249, 245, 0.82);
    transform: translateY(-1px);
  }

  @media (max-width: 700px) {
    .simpaths-home-hero {
      margin-top: -0.12rem;
      padding: 1.65rem 1rem 1.65rem;
    }

    .simpaths-home-hero__logo {
      width: 4.45rem;
    }

    .md-typeset .simpaths-home-hero h1.simpaths-home-hero__title {
      font-size: clamp(1.95rem, 7vw, 2.35rem) !important;
    }

    .md-typeset .simpaths-home-hero p.simpaths-home-hero__strap {
      font-size: 0.55rem;
      letter-spacing: 0.1em;
    }

    .md-typeset .simpaths-home-hero p.simpaths-home-hero__copy {
      max-width: 34rem;
      font-size: 0.8rem;
    }
  }

  .simpaths-home-explore {
    margin: 0.5rem 0 2.4rem;
  }

  .simpaths-home-explore__intro {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    gap: 1rem;
    margin-bottom: 1.15rem;
  }

  .md-typeset .simpaths-home-explore .section-heading {
    margin-bottom: 0.22rem;
  }

  .md-typeset .simpaths-home-explore .section-heading::after {
    background: linear-gradient(90deg, rgba(42, 56, 72, 0.18) 0%, rgba(93, 129, 158, 0.28) 26%, transparent 100%);
  }

  .md-typeset .simpaths-home-explore__lede {
    max-width: 34rem;
    margin: 0;
    color: rgba(42, 56, 72, 0.74);
    font-size: 0.82rem;
    line-height: 1.6;
  }

  .md-typeset .simpaths-home-explore .card-grid {
    display: grid;
    grid-template-columns: repeat(12, minmax(0, 1fr));
    gap: 1.08rem;
    margin: 0;
  }

  .md-typeset .simpaths-home-explore .card-grid--primary {
    margin-bottom: 1.08rem;
  }

  .md-typeset .simpaths-home-explore .card-grid--primary .feature-card {
    grid-column: span 6;
  }

  .md-typeset .simpaths-home-explore .card-grid--secondary .feature-card {
    grid-column: span 4;
  }

  .md-typeset .simpaths-home-explore a.feature-card {
    --card-bg: linear-gradient(145deg, #1e2c3a 0%, #31465b 56%, #40586d 100%);
    --card-bloom: rgba(255, 255, 255, 0.18);
    --card-copy: rgba(250, 249, 245, 0.82);
    --card-kicker: rgba(250, 249, 245, 0.72);
    --card-pill: rgba(250, 249, 245, 0.12);
    --card-pill-border: rgba(250, 249, 245, 0.18);
    position: relative;
    display: flex;
    flex-direction: column;
    min-height: 15.2rem;
    padding: 1.38rem 1.42rem 1.3rem;
    border-radius: 24px;
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, 0.18);
    background: var(--card-bg);
    box-shadow: 0 22px 54px rgba(18, 24, 36, 0.18);
    text-decoration: none !important;
    border-bottom: none !important;
    color: var(--sp-paper) !important;
    isolation: isolate;
    transition:
      transform 0.24s cubic-bezier(0.22, 1, 0.36, 1),
      box-shadow 0.24s cubic-bezier(0.22, 1, 0.36, 1),
      border-color 0.24s ease;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary {
    min-height: 17rem;
    padding: 1.54rem 1.6rem 1.44rem;
  }

  .md-typeset .simpaths-home-explore a.feature-card > * {
    position: relative;
    z-index: 1;
  }

  .md-typeset .simpaths-home-explore a.feature-card::before {
    content: "";
    position: absolute;
    top: -5.6rem;
    right: -3.2rem;
    width: 15rem;
    height: 15rem;
    border-radius: 999px;
    background: radial-gradient(circle, var(--card-bloom) 0%, rgba(255, 255, 255, 0.03) 58%, rgba(255, 255, 255, 0) 72%);
    pointer-events: none;
    transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  }

  .md-typeset .simpaths-home-explore a.feature-card::after {
    content: "";
    position: absolute;
    inset: 0;
    background:
      linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0) 36%),
      repeating-linear-gradient(135deg, rgba(255, 255, 255, 0.07) 0 1px, transparent 1px 13px);
    opacity: 0.3;
    mix-blend-mode: screen;
    pointer-events: none;
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover {
    transform: translateY(-7px);
    border-color: rgba(255, 255, 255, 0.28);
    box-shadow: 0 28px 68px rgba(18, 24, 36, 0.24);
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover::before {
    transform: translate3d(-12px, 10px, 0) scale(1.08);
  }

  .md-typeset .simpaths-home-explore .card-topline {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 0.7rem;
    margin-bottom: 0.95rem;
  }

  .md-typeset .simpaths-home-explore .card-kicker {
    display: inline-block;
    margin: 0;
    color: var(--card-kicker);
    font-size: 0.62rem;
    font-weight: 760;
    letter-spacing: 0.16em;
    text-transform: uppercase;
  }

  .md-typeset .simpaths-home-explore .card-badge {
    display: inline-flex;
    align-items: center;
    min-height: 1.55rem;
    padding: 0.2rem 0.6rem;
    border-radius: 999px;
    background: var(--card-pill);
    border: 1px solid var(--card-pill-border);
    color: rgba(250, 249, 245, 0.96);
    font-size: 0.64rem;
    font-weight: 650;
    white-space: nowrap;
    backdrop-filter: blur(12px);
  }

  .md-typeset .simpaths-home-explore .card-icon-wrap {
    width: 50px;
    height: 50px;
    border-radius: 15px;
    margin-bottom: 0.95rem;
    background: linear-gradient(160deg, rgba(255, 255, 255, 0.22) 0%, rgba(255, 255, 255, 0.08) 100%);
    border: 1px solid rgba(255, 255, 255, 0.22);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16);
    color: var(--sp-paper);
    backdrop-filter: blur(14px);
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover .card-icon-wrap {
    transform: translateY(-2px) scale(1.04);
  }

  .md-typeset .simpaths-home-explore .card-icon-wrap svg {
    width: 20px;
    height: 20px;
  }

  .md-typeset .simpaths-home-explore .feature-card--model {
    --card-bg: linear-gradient(145deg, #16283b 0%, #1f4761 54%, #23809f 100%);
    --card-bloom: rgba(126, 215, 255, 0.38);
  }

  .md-typeset .simpaths-home-explore .feature-card--documentation {
    --card-bg: linear-gradient(145deg, #162923 0%, #1e5147 52%, #2f8a79 100%);
    --card-bloom: rgba(118, 239, 214, 0.34);
  }

  .md-typeset .simpaths-home-explore .feature-card--validation {
    --card-bg: linear-gradient(145deg, #1b2433 0%, #384b66 54%, #6b8097 100%);
    --card-bloom: rgba(168, 196, 232, 0.3);
  }

  .md-typeset .simpaths-home-explore .feature-card--research {
    --card-bg: linear-gradient(145deg, #2b1520 0%, #6a2533 54%, #be5055 100%);
    --card-bloom: rgba(255, 173, 176, 0.32);
  }

  .md-typeset .simpaths-home-explore .feature-card--funding {
    --card-bg: linear-gradient(145deg, #2f2210 0%, #6d5120 54%, #ae8531 100%);
    --card-bloom: rgba(255, 225, 150, 0.28);
  }

  .md-typeset .simpaths-home-explore a.feature-card h3 {
    margin-bottom: 0.42rem !important;
    color: var(--sp-paper) !important;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary h3 {
    font-size: 1.36rem !important;
  }

  .md-typeset .simpaths-home-explore a.feature-card p {
    margin-bottom: 0;
    color: var(--card-copy);
    font-size: 0.79rem;
    line-height: 1.62;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary p {
    font-size: 0.82rem;
  }

  .md-typeset .simpaths-home-explore .card-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 0.45rem;
    margin: 0.96rem 0 0;
  }

  .md-typeset .simpaths-home-explore .card-tags span {
    display: inline-flex;
    align-items: center;
    min-height: 1.55rem;
    padding: 0.22rem 0.58rem;
    border-radius: 999px;
    background: rgba(250, 249, 245, 0.1);
    border: 1px solid rgba(250, 249, 245, 0.14);
    color: rgba(250, 249, 245, 0.9);
    font-size: 0.64rem;
    font-weight: 620;
    letter-spacing: 0.02em;
  }

  .md-typeset .simpaths-home-explore a.feature-card .card-link {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    margin-top: auto;
    padding-top: 1rem;
    border-top: 1px solid rgba(250, 249, 245, 0.16);
    color: var(--sp-paper);
    font-size: 0.76rem;
    font-weight: 680;
    letter-spacing: 0.01em;
    gap: 0.5rem;
  }

  .md-typeset .simpaths-home-explore a.feature-card .card-link::before {
    content: "Jump in";
    display: block;
    margin-bottom: 0.08rem;
    font-size: 0.54rem;
    font-weight: 760;
    letter-spacing: 0.15em;
    text-transform: uppercase;
    color: var(--card-kicker);
  }

  .md-typeset .simpaths-home-explore a.feature-card .card-link::after {
    content: "\2192";
    display: inline-grid;
    place-items: center;
    width: 2.1rem;
    height: 2.1rem;
    border-radius: 999px;
    background: rgba(250, 249, 245, 0.14);
    color: var(--sp-paper);
    flex: 0 0 auto;
    transform: translateX(0);
    transition: transform 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover .card-link::after {
    transform: translateX(4px);
    background: rgba(250, 249, 245, 0.2);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16);
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover .card-link {
    color: var(--sp-paper);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore__lede {
    color: rgba(250, 249, 245, 0.72);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore .section-heading::after {
    background: linear-gradient(90deg, rgba(92, 184, 240, 0.26) 0%, rgba(195, 198, 213, 0.32) 35%, transparent 100%);
  }

  @media (max-width: 900px) {
    .simpaths-home-explore__intro {
      flex-direction: column;
      align-items: flex-start;
      margin-bottom: 1rem;
    }

    .md-typeset .simpaths-home-explore .card-grid--secondary .feature-card {
      grid-column: span 6;
    }
  }

  @media (max-width: 760px) {
    .md-typeset .simpaths-home-explore .card-grid {
      grid-template-columns: 1fr;
    }

    .md-typeset .simpaths-home-explore .card-grid--primary .feature-card,
    .md-typeset .simpaths-home-explore .card-grid--secondary .feature-card {
      grid-column: auto;
    }

    .md-typeset .simpaths-home-explore a.feature-card,
    .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary {
      min-height: auto;
    }

    .md-typeset .simpaths-home-explore a.feature-card .card-link::after {
      width: 1.95rem;
      height: 1.95rem;
    }
  }
</style>

<div class="simpaths-home-hero">
<div class="simpaths-home-hero__inner">
  <div class="simpaths-home-hero__brand">
    <img src="assets/images/homepage-hero-logo.svg?v=3" alt="SimPaths logo showing people progressing across a rising path." class="simpaths-home-hero__logo">
    <h1 class="simpaths-home-hero__title">SimPaths</h1>
    <p class="simpaths-home-hero__strap">Life Course Microsimulation</p>
  </div>

  <p class="simpaths-home-hero__copy">SimPaths is a family of open-source models for individual and household life course events, all sharing common components. The framework is designed to project life histories through time, building up a detailed picture of career paths, family (inter)relations, health, and financial circumstances.</p>

  <p class="simpaths-home-hero__copy">The broader SimPaths family spans <strong>the UK and other European countries</strong>.</p>

  <div class="simpaths-home-hero__countries">
    <span class="simpaths-home-hero__country">🇬🇧 United Kingdom</span>
    <span class="simpaths-home-hero__country">🇬🇷 Greece</span>
    <span class="simpaths-home-hero__country">🇭🇺 Hungary</span>
    <span class="simpaths-home-hero__country">🇮🇹 Italy</span>
    <span class="simpaths-home-hero__country">🇵🇱 Poland</span>
  </div>

  <div class="simpaths-home-hero__cta">
    <a href="documentation/" class="simpaths-home-hero__btn--primary">Explore Documentation →</a>
    <a href="overview/" class="simpaths-home-hero__btn--secondary">Explore the Model</a>
    <a href="https://github.com/simpaths/SimPaths" class="simpaths-home-hero__btn--secondary">View on GitHub</a>
  </div>
</div>
</div>

<section class="simpaths-home-explore">
<div class="simpaths-home-explore__intro">
  <div>
    <h2 class="section-heading">Explore SimPaths</h2>
    <p class="simpaths-home-explore__lede">Choose a route into the project. The first row gets you into the model and documentation fast; the second row points to evidence, outputs, and programme context.</p>
  </div>
</div>

<div class="card-grid card-grid--primary">

<a href="overview/" class="feature-card feature-card--primary feature-card--model">
<div class="card-topline">
  <span class="card-kicker">Core model</span>
  <span class="card-badge">Modules + structure</span>
</div>
<div class="card-icon-wrap">
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
</div>
<h3>Model</h3>
<p>Understand what SimPaths simulates, how the major components fit together, and how country-specific logic is organised across the framework.</p>
<div class="card-tags">
  <span>Life-course events</span>
  <span>Parameterisation</span>
  <span>Country models</span>
</div>
<span class="card-link">Explore the model</span>
</a>

<a href="documentation/" class="feature-card feature-card--primary feature-card--documentation">
<div class="card-topline">
  <span class="card-kicker">Guides + setup</span>
  <span class="card-badge">Run SimPaths</span>
</div>
<div class="card-icon-wrap">
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
</div>
<h3>Documentation</h3>
<p>Get from environment setup to first run, then move through user guides, developer internals, and the reference material that powers the model.</p>
<div class="card-tags">
  <span>Getting started</span>
  <span>User guide</span>
  <span>Developer internals</span>
</div>
<span class="card-link">Open documentation</span>
</a>

</div>

<div class="card-grid card-grid--secondary">

<a href="validation/" class="feature-card feature-card--validation">
<div class="card-topline">
  <span class="card-kicker">Diagnostics</span>
  <span class="card-badge">Targets + checks</span>
</div>
<div class="card-icon-wrap">
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
</div>
<h3>Validation</h3>
<p>Validation procedures, alignment diagnostics, and comparison against external targets.</p>
<div class="card-tags">
  <span>Alignment</span>
  <span>Benchmarks</span>
</div>
<span class="card-link">Validation</span>
</a>

<a href="research/" class="feature-card feature-card--research">
<div class="card-topline">
  <span class="card-kicker">Publications</span>
  <span class="card-badge">Applied work</span>
</div>
<div class="card-icon-wrap">
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 20V10"/><path d="M12 20V4"/><path d="M6 20v-6"/></svg>
</div>
<h3>Research</h3>
<p>Published papers, working papers, and ongoing research using SimPaths.</p>
<div class="card-tags">
  <span>Journal articles</span>
  <span>Policy analysis</span>
</div>
<span class="card-link">Research</span>
</a>

<a href="funding/" class="feature-card feature-card--funding">
<div class="card-topline">
  <span class="card-kicker">Programme support</span>
  <span class="card-badge">Partners + grants</span>
</div>
<div class="card-icon-wrap">
  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10h18"/><path d="M5 10v8"/><path d="M9 10v8"/><path d="M15 10v8"/><path d="M19 10v8"/><path d="M2 18h20"/><path d="M12 2l10 5H2l10-5z"/></svg>
</div>
<h3>Funding</h3>
<p>See the grants and programmes that have supported the development and application of SimPaths.</p>
<div class="card-tags">
  <span>Grant history</span>
  <span>Institutional support</span>
</div>
<span class="card-link">Funding</span>
</a>

</div>
</section>

---

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
