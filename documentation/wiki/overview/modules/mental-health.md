---
title: Mental health
---

<div class="module-detail" markdown>

# Mental health

<p class="module-detail__lead">Mental health is represented through the 12-item General Health Questionnaire, with baseline measures of psychological distress subsequently adjusted for economic transitions and non-economic exposure to the Covid-19 pandemic.</p>

## Baseline psychological distress

For people aged 16 and over, each simulation cycle produces two GHQ-12 measures. A Likert score from 0 to 36 is estimated with a linear regression. A binary indicator of potentially clinically significant common mental disorder is estimated with a logistic regression.

Both specifications condition on the lagged number of dependent children, lagged physical and mental health, gender, age, education, household composition, region and year.

## Economic transitions and the Covid-19 period

The baseline level and caseness measures are adjusted for changes in economic circumstances and for non-economic exposure to the Covid-19 pandemic.

Fixed-effects regressions estimate the direct effects of transitions from employment to non-employment, non-employment to employment, and non-employment to long-term non-employment. They also estimate transitions into and out of poverty, long-term poverty, changes in household-income growth and decreases in household income.

Effects of economic transitions are estimated on pre-pandemic data so that they remain applicable in other periods. Non-economic pandemic effects for 2020 and 2021 are estimated with a multilevel mixed-effects generalised linear model.

## Process diagrams

The first diagram separates the baseline level process from the secondary adjustment implemented by `Person.healthMentalHM2level()`. The second shows how the baseline and secondary caseness processes are combined.

<details class="module-process-diagram">
  <summary>View the psychological-distress level diagram</summary>
  <div class="module-process-diagram__meta">
    <span>Scroll to follow the complete sequence.</span>
    <a href="/figures/modules/mental_health_levels_module.png" target="_blank" rel="noopener">Open full resolution <span aria-hidden="true">&#8599;</span></a>
  </div>
  <div class="module-process-diagram__viewport" role="region" aria-label="Scrollable psychological distress level diagram" tabindex="0">
    <img src="/figures/modules/optimized/mental-health-levels-flow.png" alt="Psychological distress level process" width="1200" height="1915" loading="lazy" decoding="async">
  </div>
</details>

<details class="module-process-diagram">
  <summary>View the psychological-distress caseness diagram</summary>
  <div class="module-process-diagram__meta">
    <span>Scroll to follow the complete sequence.</span>
    <a href="/figures/modules/mental_health_cases_module.png" target="_blank" rel="noopener">Open full resolution <span aria-hidden="true">&#8599;</span></a>
  </div>
  <div class="module-process-diagram__viewport" role="region" aria-label="Scrollable psychological distress caseness diagram" tabindex="0">
    <img src="/figures/modules/optimized/mental-health-cases-flow.png" alt="Psychological distress caseness process" width="1200" height="1923" loading="lazy" decoding="async">
  </div>
</details>

</div>
