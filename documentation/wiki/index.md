---
hide:
  - toc
---

<section class="simpaths-home-hero" aria-labelledby="home-title">
<div class="simpaths-home-hero__inner">
  <div class="simpaths-home-hero__brand">
--8<-- "documentation/wiki/assets/images/homepage-hero-logo-dark.svg"
    <h1 id="home-title" class="simpaths-home-hero__title">SimPaths</h1>
    <p class="simpaths-home-hero__strap">Life Course Microsimulation</p>
  </div>
</div>
</section>

<section class="simpaths-home-intro-band">
<div class="simpaths-home-intro-band__inner">
  <p class="simpaths-home-intro-band__lede">SimPaths is an open-source framework for modelling individual and household life courses. It projects how people’s careers, family relationships, health and financial circumstances evolve over time and interact with one another. Its modular design supports analysis of alternative assumptions about tax and benefit systems, sensitivity to parameter estimates, and different approaches to projecting labour and leisure, consumption and saving decisions.</p>
  <p class="simpaths-home-intro-band__body">Standardised assumptions and data sources facilitate adaptation across countries. Models based on the framework currently exist for the UK, Greece, Hungary, Italy and Poland, with Germany, Spain and Sweden under development.</p>
  <p class="simpaths-home-intro-band__body">All source code is freely available for download under a European Free/Open Source Software (F/OSS) EUPL-1.2 license, alongside evolving, increasingly detailed documentation.</p>
  <h2 id="model-features" class="simpaths-home-features__heading">Model features</h2>
  <div class="simpaths-home-feature-map">
  <dl class="simpaths-home-features" aria-labelledby="model-features">
    <div>
      <dt><a href="overview/#transitions-and-individual-variation">Connected life domains</a></dt>
      <dd>Education, work, family life, health and finances evolve together, with earlier circumstances influencing later outcomes.</dd>
    </div>
    <div>
      <dt><a href="overview/modules/labour-income/#employment-decisions">Behavioural responses</a></dt>
      <dd>Employment responds to financial incentives and family circumstances. A forward-looking option also models consumption and saving.</dd>
    </div>
    <div>
      <dt><a href="overview/modules/disposable-income/">Tax–benefit detail</a></dt>
      <dd>Results from tax–benefit models link policy rules to disposable income for different working hours and family circumstances.</dd>
    </div>
    <div>
      <dt><a href="user-guide/uncertainty-analysis/">Alternative assumptions</a></dt>
      <dd>Vary economic and demographic assumptions, compare behavioural specifications, and test sensitivity to estimated parameters.</dd>
    </div>
  </dl>
  <div class="simpaths-home-features__purpose">
    <h3>Policy analysis over the life course</h3>
    <p>Together, these features allow policies to be compared by their effects on incomes, behaviour and inequalities over time.</p>
  </div>
  </div>
  <div class="simpaths-home-intro-band__access-row" aria-label="SimPaths source code">
    <a href="https://github.com/simpaths/SimPaths" class="simpaths-home-intro-band__access">UK source code</a>
    <a href="https://github.com/simpaths/SimPathsEU" class="simpaths-home-intro-band__access">EU source code</a>
  </div>
</div>
</section>

<section id="use-simpaths" class="simpaths-home-paths" aria-labelledby="use-simpaths-title">
<div class="simpaths-home-paths__inner">
  <header class="simpaths-home-paths__header">
    <h2 id="use-simpaths-title">Use SimPaths</h2>
  </header>

  <div class="simpaths-first-run">
      <p class="simpaths-first-run__intro">Try a first simulation using the training population and tax–benefit data included in the repository. This lets you check the installation before preparing inputs for your own research.</p>
      <ol class="simpaths-first-run__steps" aria-label="Steps to a first simulation">
        <li><a href="getting-started/environment-setup/">
          <span class="simpaths-first-run__number" aria-hidden="true">01</span>
          <span><strong>Set up the software</strong><span class="simpaths-first-run__description">Install Java and Maven, then build SimPaths.</span></span>
          <span class="simpaths-first-run__arrow" aria-hidden="true">→</span>
        </a></li>
        <li><a href="getting-started/data/">
          <span class="simpaths-first-run__number" aria-hidden="true">02</span>
          <span><strong>Check the input data</strong><span class="simpaths-first-run__description">Locate the training population, tax–benefit donors and parameter files.</span></span>
          <span class="simpaths-first-run__arrow" aria-hidden="true">→</span>
        </a></li>
        <li><a href="getting-started/first-simulation/">
          <span class="simpaths-first-run__number" aria-hidden="true">03</span>
          <span><strong>Run your first simulation</strong><span class="simpaths-first-run__description">Prepare the database, launch the model and check the run.</span></span>
          <span class="simpaths-first-run__arrow" aria-hidden="true">→</span>
        </a></li>
      </ol>
      <footer class="simpaths-first-run__footer">
        <p class="simpaths-first-run__note">Training data are for learning and testing, not substantive analysis.</p>
        <a class="simpaths-home-paths__browse" href="documentation/">Browse the full documentation <span aria-hidden="true">→</span></a>
      </footer>
  </div>
</div>
</section>

<section id="selected-research" class="simpaths-home-research-band">
<div class="research-section">
<div class="research-header">
  <h2 class="section-heading">Selected Research</h2>
  <a href="research/" class="archive-link">All research</a>
</div>

<div class="research-list">

<a href="https://doi.org/10.1093/eurpub/ckaf161.076" class="research-entry research-entry--mental-health">
  <div class="research-entry__meta">
    <span class="research-label">MENTAL HEALTH · 2025</span>
    <span class="research-journal">European Journal of Public Health</span>
  </div>
  <p class="research-title">Tax reforms vs benefit enhancement to address mental health inequalities: a microsimulation study</p>
  <p class="research-authors">Igelstrom E, Kopasker D, Richiardi MG, Katikireddi SV</p>
</a>

<a href="https://www.sciencedirect.com/science/article/pii/S0167268125000319" class="research-entry research-entry--life-course">
  <div class="research-entry__meta">
    <span class="research-label">LIFE COURSE · 2025</span>
    <span class="research-journal">Journal of Economic Behavior &amp; Organization</span>
  </div>
  <p class="research-title">Attenuation and reinforcement mechanisms over the life course</p>
  <p class="research-authors">Richiardi M, Bronka P, van de Ven J</p>
</a>

<a href="https://journals.plos.org/plosmedicine/article?id=10.1371/journal.pmed.1004358" class="research-entry research-entry--health-policy">
  <div class="research-entry__meta">
    <span class="research-label">HEALTH POLICY · 2024</span>
    <span class="research-journal">PLOS Medicine</span>
  </div>
  <p class="research-title">Short-term impacts of Universal Basic Income on population mental health inequalities in the UK</p>
  <p class="research-authors">Thomson RM, Kopasker D, Bronka P, et al.</p>
</a>

</div>
</div>
</section>
