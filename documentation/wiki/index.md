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
    margin: 0.36rem 0 2.35rem;
  }

  .simpaths-home-explore__intro {
    margin-bottom: 1.05rem;
  }

  .md-typeset .simpaths-home-explore .section-heading {
    margin-bottom: 0.28rem;
  }

  .md-typeset .simpaths-home-explore .section-heading::after {
    background: linear-gradient(90deg, rgba(42, 56, 72, 0.18) 0%, rgba(93, 129, 158, 0.24) 24%, transparent 100%);
  }

  .md-typeset .simpaths-home-explore .card-grid {
    display: grid;
    gap: 1.08rem;
    margin: 0;
  }

  .md-typeset .simpaths-home-explore .card-grid--primary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin-bottom: 1.08rem;
  }

  .md-typeset .simpaths-home-explore .card-grid--secondary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .md-typeset .simpaths-home-explore .card-grid--primary .feature-card--documentation {
    grid-column: span 2;
  }

  .md-typeset .simpaths-home-explore a.feature-card {
    --card-accent: #2f5f7d;
    --card-accent-deep: #22384b;
    --card-accent-soft: rgba(47, 95, 125, 0.1);
    position: relative;
    display: flex;
    flex-direction: column;
    min-height: 13.6rem;
    padding: 1.42rem 1.45rem 1.28rem;
    border-radius: 10px;
    overflow: hidden;
    border: 1px solid rgba(42, 56, 72, 0.12);
    background: rgba(255, 255, 255, 0.98);
    box-shadow:
      inset 0 3px 0 0 var(--card-accent),
      0 10px 24px rgba(18, 24, 36, 0.06);
    text-decoration: none !important;
    border-bottom: none !important;
    color: inherit !important;
    transition:
      transform 0.24s cubic-bezier(0.22, 1, 0.36, 1),
      box-shadow 0.24s cubic-bezier(0.22, 1, 0.36, 1),
      border-color 0.24s ease;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary {
    min-height: 14.8rem;
    padding: 1.56rem 1.62rem 1.4rem;
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover {
    transform: translateY(-3px);
    border-color: rgba(42, 56, 72, 0.18);
    box-shadow:
      inset 0 3px 0 0 var(--card-accent),
      0 14px 30px rgba(18, 24, 36, 0.1);
  }

  .md-typeset .simpaths-home-explore .card-header {
    display: flex;
    align-items: center;
    gap: 0.82rem;
    margin-bottom: 0.9rem;
  }

  .md-typeset .simpaths-home-explore .card-label {
    min-width: 0;
  }

  .md-typeset .simpaths-home-explore .card-icon-wrap {
    width: 42px;
    height: 42px;
    flex: 0 0 42px;
    border-radius: 8px;
    background: var(--card-accent-soft);
    border: 1px solid rgba(42, 56, 72, 0.08);
    color: var(--card-accent-deep);
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover .card-icon-wrap {
    transform: translateY(-1px);
  }

  .md-typeset .simpaths-home-explore .card-icon-wrap svg {
    width: 20px;
    height: 20px;
  }

  .md-typeset .simpaths-home-explore .feature-card--model {
    --card-accent: #2b6f96;
    --card-accent-deep: #1e3d54;
    --card-accent-soft: rgba(43, 111, 150, 0.14);
  }

  .md-typeset .simpaths-home-explore .feature-card--documentation {
    --card-accent: #2a7866;
    --card-accent-deep: #21493f;
    --card-accent-soft: rgba(42, 120, 102, 0.14);
  }

  .md-typeset .simpaths-home-explore .feature-card--validation {
    --card-accent: #58718d;
    --card-accent-deep: #31465c;
    --card-accent-soft: rgba(88, 113, 141, 0.15);
  }

  .md-typeset .simpaths-home-explore .feature-card--research {
    --card-accent: #4e6986;
    --card-accent-deep: #30485e;
    --card-accent-soft: rgba(78, 105, 134, 0.14);
  }

  .md-typeset .simpaths-home-explore .feature-card--funding {
    --card-accent: #9b7427;
    --card-accent-deep: #5f4720;
    --card-accent-soft: rgba(155, 116, 39, 0.14);
  }

  .md-typeset .simpaths-home-explore a.feature-card h3 {
    margin: 0 !important;
    color: #223142 !important;
    line-height: 1.12;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary h3 {
    font-size: 1.22rem !important;
  }

  .md-typeset .simpaths-home-explore a.feature-card p {
    margin-bottom: 0;
    color: rgba(42, 56, 72, 0.78);
    font-size: 0.79rem;
    line-height: 1.62;
  }

  .md-typeset .simpaths-home-explore a.feature-card.feature-card--primary p {
    font-size: 0.82rem;
  }

  .md-typeset .simpaths-home-explore a.feature-card .card-link {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    gap: 0.5rem;
    margin-top: auto;
    padding-top: 1rem;
    border-top: 1px solid rgba(42, 56, 72, 0.08);
    color: var(--card-accent-deep);
    font-size: 0.74rem;
    font-weight: 690;
    letter-spacing: 0.01em;
  }

  .md-typeset .simpaths-home-explore a.feature-card .card-link::after {
    content: "\2192";
    display: inline-grid;
    place-items: center;
    color: var(--card-accent);
    transform: translateX(0);
    transition: transform 0.18s ease;
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover .card-link::after {
    transform: translateX(3px);
  }

  .md-typeset .simpaths-home-explore a.feature-card:hover .card-link {
    color: var(--card-accent-deep);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore .section-heading::after {
    background: linear-gradient(90deg, rgba(92, 184, 240, 0.26) 0%, rgba(195, 198, 213, 0.32) 35%, transparent 100%);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card {
    background: rgba(23, 34, 46, 0.98);
    border-color: rgba(250, 249, 245, 0.08);
    box-shadow:
      inset 0 3px 0 0 var(--card-accent),
      0 16px 32px rgba(0, 0, 0, 0.24);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card:hover {
    border-color: rgba(250, 249, 245, 0.16);
    box-shadow:
      inset 0 3px 0 0 var(--card-accent),
      0 20px 38px rgba(0, 0, 0, 0.3);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore .card-icon-wrap {
    border-color: rgba(250, 249, 245, 0.08);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card h3 {
    color: #edf3f8 !important;
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card p {
    color: rgba(250, 249, 245, 0.72);
  }

  [data-md-color-scheme="slate"] .md-typeset .simpaths-home-explore a.feature-card .card-link {
    border-top-color: rgba(250, 249, 245, 0.1);
    color: rgba(250, 249, 245, 0.92);
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
  </div>
</div>

<div class="card-grid card-grid--primary">

<a href="documentation/" class="feature-card feature-card--primary feature-card--documentation">
<div class="card-header">
  <div class="card-icon-wrap">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>
  </div>
  <div class="card-label">
    <h3>Documentation</h3>
  </div>
</div>
<p>Get from environment setup to first run, then move through user guides, developer internals, and the reference material that powers the model.</p>
<span class="card-link">Open documentation</span>
</a>

<a href="overview/" class="feature-card feature-card--model">
<div class="card-header">
  <div class="card-icon-wrap">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>
  </div>
  <div class="card-label">
    <h3>Model</h3>
  </div>
</div>
<p>Understand what SimPaths simulates, how the major components fit together, and how country-specific logic is organised across the framework.</p>
<span class="card-link">Explore the model</span>
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
<p>Validation procedures, alignment diagnostics, and comparison against external targets.</p>
<span class="card-link">Validation</span>
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
<p>Published papers, working papers, and ongoing research using SimPaths.</p>
<span class="card-link">Research</span>
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
<p>See the grants and programmes that have supported the development and application of SimPaths.</p>
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
