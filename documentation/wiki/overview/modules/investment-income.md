---
title: Investment income
---

<div class="module-detail" markdown>

# Investment income

<p class="module-detail__lead">The investment-income module projects private pension income and returns on assets. Its treatment depends on whether a model variant projects wealth explicitly or uses regression-based proxies for non-labour income.</p>

## Retirement

Retirement can occur for adults above an assumed threshold, set to age 50 in the UK parameterisation. Its treatment differs according to whether forward-looking expectations are implicit or explicit.

With implicit expectations, entry to retirement is described by a probit model conditional on gender, age, education, lagged employment status, lagged benefit-unit income quintile, lagged disability, whether the person has reached state pension age, region and year. For couples, the spouse's employment status and proximity to retirement age are also included.

With explicit forward-looking expectations, retirement is treated as a control variable. Retired people may receive pension income under either specification.

## Private pension income

When wealth is not projected explicitly, private pension income for people continuing in retirement is described by a linear regression conditional on age, education, lagged household composition, lagged health, lagged pension income, region and year.

For people entering retirement, a logit model first determines whether private pension income is received. It conditions on state pension age, education, lagged employment status, lagged household composition, lagged health, lagged hourly wage potential, region and year. A linear regression using the same observed characteristics then projects the amount.

When wealth is projected explicitly, an assumed share of benefit-unit wealth is converted at retirement into a life annuity, or a joint-life annuity for couples. Annuity rates are actuarially fair given cohort-specific mortality rates and an assumed internal rate of return.

## Capital income

When wealth is not projected, the incidence of capital income among people aged 16 and over is described by a logit equation that varies with age, lagged health, lagged gross employment and capital income, region and year. For people who are not in continuous education, education, lagged employment and lagged household composition are also included.

For people projected to receive capital income, the amount is described by linear regressions. For students, these condition on gender, age, lagged health, lagged gross employment income, lagged capital income, region and year. For non-students, education, lagged employment and lagged household composition are added.

When wealth is projected explicitly, capital income is the product of net assets and an assumed return. The return varies by year and by benefit-unit net wealth, $w_{i,t}$:

$$
r_{i,t} =
\begin{cases}
r_{a,t}, & w_{i,t} \geq 0, \\
r_{dl,t} + \left(r_{du,t} - r_{dl,t}\right)\phi_{i,t}, & w_{i,t} < 0.
\end{cases}
$$

Here, $i$ denotes the benefit unit and $t$ time. The bounded ratio $0 \leq \phi_{i,t} \leq 1$ represents benefit-unit debt relative to full-time potential earnings. Assuming $r_{du,t} \geq r_{dl,t}$ imposes a soft constraint under which interest rates rise with indebtedness.

</div>
