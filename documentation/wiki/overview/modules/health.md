---
title: Health
---

<div class="module-detail" markdown>

# Health

<p class="module-detail__lead">The health module projects self-rated physical health and determines whether a person is long-term sick or disabled. Psychological distress is documented separately because it combines a baseline health process with later economic transitions.</p>

## Physical health

Self-rated health is projected on a five-point scale from poor to excellent. The current UK implementation uses a generalised ordered logit model (process H1), which allows relationships with the explanatory variables to differ across health thresholds.

The equation includes student status, gender, age, education, lagged SF-12 physical and mental health scores, lagged employment, income quintile, household composition and disability, as well as region, ethnicity and time effects. It is one model with these covariates, not separate equations for students and other adults.

## Long-term sickness and disability

For people aged 16 or over who have left continuous education, a probit model (H2) determines long-term sickness or disability. Its covariates include lagged disability, lagged SF-12 physical and mental health scores, gender, age, education, lagged income quintile and household composition, region, ethnicity and time effects. Under intertemporal optimisation, this process also depends on the disability setting in the decision model.

The disability decision is integrated into the physical-health implementation rather than run as a separate module.

## Process diagram

The diagram shows the combined physical-health and disability process.

<details class="module-process-diagram">
  <summary>View the health diagram</summary>
  <div class="module-process-diagram__meta">
    <span>Scroll to follow the complete sequence.</span>
    <a href="/figures/modules/health_module.png" target="_blank" rel="noopener">Open full resolution <span aria-hidden="true">&#8599;</span></a>
  </div>
  <div class="module-process-diagram__viewport" role="region" aria-label="Scrollable physical health implementation diagram" tabindex="0">
    <img src="/figures/modules/optimized/health-flow.png" alt="Physical health and disability implementation flow" width="1200" height="3588" loading="lazy" decoding="async">
  </div>
</details>

## Psychological distress

SimPaths also projects psychological distress using the 12-item General Health Questionnaire. Its baseline estimates and subsequent adjustments for economic transitions and the Covid-19 period are described on the [Mental health](mental-health.md) page.

## Other health and wellbeing measures

Self-rated health is distinct from the continuous SF-12 physical and mental component scores (PCS and MCS). The implementation also projects these scores and life satisfaction using the equations in `reg_health_wellbeing.xlsx`. These outcomes should not be described as the five-category H1 process.

The descriptions above follow the [UK parameter-loading code](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/data/Parameters.java) and [health estimation script](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/input/InitialPopulations/compile/RegressionEstimates/05_reg_health.do). For reproducible analysis, record the code revision and parameter files used; historical releases and diagrams may describe earlier specifications.

</div>
