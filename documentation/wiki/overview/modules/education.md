---
title: Education
---

<div class="module-detail" markdown>

# Education

<p class="module-detail__lead">The education module determines transitions into and out of student status. Students do not enter the labour-supply module; when they leave education, their education level is evaluated and they may enter employment.</p>

## Student status

Students aged 16-29 are assigned a probability of remaining in education using a probit model (E1a). The current UK equation includes gender, age, lagged continuous-education status, parental education, lagged income quintile, region, ethnicity and time effects. Students below 16 remain in education; students above 29 leave. The student-share alignment adjustment is applied to E1a, not to re-entry.

People who were neither students nor retired in the preceding period may re-enter education through a second probit model (E1b). Its covariates include gender, age, lagged partnership, education and employment status, lagged numbers of children and children aged 0-2, parental education, region, ethnicity and time effects. The implementation does not impose an upper age of 45 for re-entry. A person who returns can leave again in a subsequent year.

## Education level

When a person ceases to be a student, education level is assigned using a generalised ordered logit model (E2), conditional on gender, age, lagged parental education, region, ethnicity and time effects. For people leaving education after returning to study, education may remain unchanged or increase, but cannot decrease.

These specifications follow the [UK education estimation script](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/input/InitialPopulations/compile/RegressionEstimates/01_reg_education.do) and [Java implementation](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/model/Person.java). Record the code revision and parameter files when reporting a simulation; historical releases and diagrams may use earlier specifications.

## Process diagram

The source diagram records the decisions used to update student status and education level.

<details class="module-process-diagram">
  <summary>View the education diagram</summary>
  <div class="module-process-diagram__meta">
    <span>Scroll to follow the complete sequence.</span>
    <a href="/figures/modules/education_module.png" target="_blank" rel="noopener">Open full resolution <span aria-hidden="true">&#8599;</span></a>
  </div>
  <div class="module-process-diagram__viewport" role="region" aria-label="Scrollable education implementation diagram" tabindex="0">
    <img src="/figures/modules/optimized/education-flow.png" alt="Education implementation flow" width="1200" height="3700" loading="lazy" decoding="async">
  </div>
</details>

</div>
