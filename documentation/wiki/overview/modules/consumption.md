---
title: Consumption
---

<div class="module-detail" markdown>

# Consumption

<p class="module-detail__lead">Given disposable income and household composition, the consumption module projects benefit-unit expenditure. When wealth is modelled explicitly, it also tracks net assets over time and projects homeownership.</p>

## Non-discretionary expenditure

The model projects formal social-care and formal childcare costs. Social-care costs combine projected hours of formal care with assumed hourly wage rates for social-care workers.

Childcare costs are estimated with a double-hurdle model. A probit equation describes whether formal childcare costs are incurred, and a linear least-squares equation describes their amount when they occur. Both equations include the number and ages of dependent children, adults' partnership and employment status, whether any adult has higher education, region and year.

## Discretionary consumption

Employment and discretionary consumption can be projected jointly. The approach depends on whether forward-looking expectations are represented implicitly or explicitly.

By default, annual disposable income is equivalised using the modified OECD scale. Equivalised consumption is set equal to equivalised disposable income for retired people. For everyone else, disposable income is adjusted by a fixed discount factor representing an implicit saving rate, which in turn affects simulated capital income.

With explicit expectations, the model solves the lifetime decision problem and stores solutions in a look-up table. For discretionary consumption, the table records the ratio of consumption to cash on hand, where cash on hand is net wealth plus disposable income and available credit. The ratio is bounded between zero and one, which assists numerical solution and the evaluation of policy counterfactuals.

## Asset accumulation

Net wealth connects decisions across time when forward-looking expectations are explicit. In most periods it follows the accounting identity

$$
w_{i,t} = w_{i,t-1} + y_{i,t} - c_{i,t} - \bar{c}_{i,t},
$$

where $w_{i,t}$ is the net wealth of benefit unit $i$ in period $t$, $y_{i,t}$ disposable income, $c_{i,t}$ discretionary consumption and $\bar{c}_{i,t}$ non-discretionary expenditure. At retirement, if $w_{i,t} > 0$, a fixed share of wealth is converted into a life annuity.

## Homeownership

Although net wealth is not disaggregated into asset classes, homeownership is represented because it enters the projection of psychological distress. A benefit unit owns its home if at least one responsible adult is classified as a homeowner.

At individual level, homeownership is described by a probit model conditional on gender, age, lagged employment status, education, lagged self-rated health, lagged benefit-unit income quintile, lagged gross personal non-employment non-benefit income, region, lagged household composition, lagged spouse employment and a time trend.

</div>
