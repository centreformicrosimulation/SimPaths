---
title: Model overview
hide:
  - footer
---

<div class="model-overview" id="model" markdown>

# Model overview {#model-description}

SimPaths is an open-source dynamic microsimulation model for studying how living standards and inequalities develop over the life course. Starting from a population derived from survey data, it projects changes in education, family composition, health, care, employment and financial circumstances in annual steps. It represents individuals within families and households, preserving links between partners and between parents and dependent children as their circumstances change. Births, deaths and migration also alter the population. The resulting simulated histories allow researchers to examine the distribution of outcomes in each year and how those outcomes develop over individual lives.

Demographic and health transitions largely use **reduced-form equations** estimated from survey data, relating outcomes to individual and family characteristics, including previous states. Economic decisions use a **structural behavioural model**, representing choices through preferences and budget constraints. Tax–benefit rules determine the disposable income available under different working-hours alternatives, so a reform can change both income at given hours and the incentives to work. Decisions can be evaluated within the current period or over the remaining lifetime.

## Population and model components {#simulated-modules}

The [initial population](../getting-started/data/initial-population-uk.md) is derived from Understanding Society, the UK Household Longitudinal Study. Each simulated person has a changing set of characteristics, including age, education, health, employment and income, and links to other family members. Previous values of selected characteristics are retained so that later outcomes can depend on earlier circumstances.

People are organised into **benefit units** within households. A benefit unit consists of a single adult or couple and any dependent children; a household can contain more than one such unit. This structure matters for the calculations: partners' work choices can be modelled jointly, tax–benefit entitlements depend on family composition, and income must be assessed against the needs of the people it supports. Partnership formation, separation and children becoming independent therefore change the units within which economic decisions and resources are evaluated.

<section class="model-overview__components" aria-label="Model components" markdown>

### Demography, families and education

[Ageing](modules/ageing.md) and mortality operate at individual level, while migration is represented through population alignment to demographic projections. The [family-composition module](modules/family-composition.md) projects partnership formation and dissolution, matches prospective partners within the simulated population, and projects fertility, adding newborn children to their families. The [education module](modules/education.md) projects continuation in education, exit and re-entry, and attainment on leaving. Student status affects eligibility for employment decisions, while attained education enters wage and health projections.

### Health and care

The [health module](modules/health.md) projects self-rated health and disability, with additional measures of physical and mental functioning and life satisfaction. [Mental-health modelling](modules/mental-health.md) includes psychological distress measured by the General Health Questionnaire (GHQ-12), with adjustments for changes in employment and income. Health also enters other processes, including potential wages and care needs. The [social-care module](modules/social-care.md) projects care needs, receipt and provision, distinguishing formal and informal care. Purchased care creates expenditure requirements; providing informal care uses time that enters the economic decision problem.

### Employment and income

The [labour-income module](modules/labour-income.md) projects potential hourly wages, including for people currently out of work, and evaluates working-hours choices. Wage potential depends on characteristics such as education, health and employment history; realised earnings also depend on hours worked. [Investment income](modules/investment-income.md) covers capital income and private pensions. These are projected from estimated equations when wealth is not tracked explicitly; with explicit wealth, asset returns and conversion of wealth into pension annuities connect income to accumulated resources. The [disposable-income module](modules/disposable-income.md) combines private income with taxes and benefits under the applicable policy system. Together, these calculations establish the financial consequences of work alternatives for each benefit unit.

### Consumption and wealth

The [consumption module](modules/consumption.md) represents discretionary expenditure and formal childcare and social-care costs. Consumption is expressed in equivalised terms to account for family size and composition. When wealth is tracked explicitly, it connects current expenditure to future resources through saving, borrowing and investment returns. Homeownership is also represented as a state used in mental-health projections.

</section>

## How the simulation operates {#how-the-model-works}

### The annual cycle

The modules operate in a defined [annual sequence](../developer-guide/internals/simpaths-model.md). The diagram maps their connections and distinguishes processes implemented in the UK model from proposed extensions. It shows the conceptual structure; the execution order is described below.

<figure class="module-diagram">
  <a href="../assets/images/model-structure-uk.svg" target="_blank" rel="noopener" aria-label="Open the model structure diagram at full size">
    <img src="../assets/images/model-structure-uk.svg" alt="SimPaths UK model structure. Charcoal outlines mark implemented processes, with separate asset projections and bequests shown as development extensions. Amber marks population alignment, external wage-growth assumptions and childcare costs. Purple dashed outlines mark extensions not implemented: pension-rule imputation, inter-vivos transfers and additional well-being outcomes." width="1400" height="1078" loading="lazy" decoding="async">
  </a>

</figure>

<section class="diagram-key" aria-label="Diagram notes">
<details class="diagram-key__notes">
<summary>Implementation notes</summary>
<dl>
<dt>Wealth and transfers</dt>
<dd>The model includes aggregate wealth accounting under intertemporal optimisation: wealth changes with disposable income less discretionary and non-discretionary consumption. Separate housing, financial and private-pension projections, and bequest settlement, are not yet included in the published model. Inter-vivos transfers remain proposed.</dd>
<dt>Alignment, assumptions and costs</dt>
<dd>Internal mobility and external migration use population alignment to adjust the population to demographic targets, rather than modelling migration decisions. Wage growth comes from external time-series assumptions. Childcare calculations represent formal use and expenditure, not general childcare time allocation.</dd>
<dt>Well-being and pensions</dt>
<dd>Life satisfaction and public pension income are already represented. Additional well-being outcomes and dedicated pension-rule imputation remain proposed extensions; the latter is a separate method of projecting entitlements.</dd>
</dl>
<p>This diagram describes the UK model. See the <a href="../developer-guide/internals/simpaths-model/">event schedule</a> for execution order and the <a href="roadmap/">development roadmap</a> for planned work.</p>
</details>
<a class="diagram-key__full-size" href="../assets/images/model-structure-uk.svg" target="_blank" rel="noopener">Open full size ↗</a>
</section>

<details class="annual-explorer" data-annual-explorer markdown="0">
<summary>Inspect the modules</summary>
<div class="annual-explorer__body">
<p class="annual-explorer__intro">Select a module to see the population it acts on, examples of its inputs and the states it updates. This guide covers the documented UK processes; the diagram above also includes extensions. The current event schedule is described below.</p>
<div class="annual-explorer__stages">
<details class="annual-explorer__stage" id="cycle-ageing">
<summary>Ageing</summary>
<div class="annual-explorer__content">
<dl>
<dt>Population</dt><dd>Individuals; benefit units and households when their membership changes.</dd>
<dt>Inputs include</dt><dd>Age, family membership, mortality risks and demographic projection targets.</dd>
<dt>Updates</dt><dd>Age, independence from the parental benefit unit, survival and population composition. In the current schedule, mortality and population alignment occur towards year end.</dd>
</dl>
<p class="annual-explorer__links"><a href="modules/ageing/">Ageing and population alignment</a></p>
</div>
</details>
<details class="annual-explorer__stage" id="cycle-education">
<summary>Education</summary>
<div class="annual-explorer__content">
<dl>
<dt>Population</dt><dd>Students and eligible people who may re-enter education.</dd>
<dt>Inputs include</dt><dd>Age, previous student status, parental education and previous family and economic circumstances.</dd>
<dt>Updates</dt><dd>Student status and educational attainment on leaving education. Student status determines whether a person can enter labour-supply decisions; attainment enters later wage and health projections.</dd>
</dl>
<p class="annual-explorer__links"><a href="modules/education/">Education transitions and attainment</a></p>
</div>
</details>
<details class="annual-explorer__stage" id="cycle-health">
<summary>Health</summary>
<div class="annual-explorer__content">
<dl>
<dt>Population</dt><dd>Individuals, with eligibility rules for each health process.</dd>
<dt>Inputs include</dt><dd>Age, education, previous health, employment, income and household composition.</dd>
<dt>Updates</dt><dd>Self-rated health, long-term sickness or disability, and additional health measures. Health and disability enter wage, care and employment calculations. Psychological distress is documented separately.</dd>
</dl>
<p class="annual-explorer__links"><a href="modules/health/">Health processes and eligibility</a></p>
</div>
</details>
<details class="annual-explorer__stage" id="cycle-family">
<summary>Family composition</summary>
<div class="annual-explorer__content">
<dl>
<dt>Population</dt><dd>Eligible individuals, couples and their dependent children.</dd>
<dt>Inputs include</dt><dd>Partnership status, age, education, earnings potential and previous family circumstances.</dd>
<dt>Updates</dt><dd>Partnership formation and dissolution, partner matching and births. These processes change benefit-unit and household membership, establishing the family circumstances used in subsequent economic calculations.</dd>
</dl>
<p class="annual-explorer__links"><a href="modules/family-composition/">Partnerships, matching and fertility</a></p>
</div>
</details>
<details class="annual-explorer__stage" id="cycle-care">
<summary>Social care</summary>
<div class="annual-explorer__content">
<dl>
<dt>Population</dt><dd>People who may need or receive care, and potential informal carers.</dd>
<dt>Inputs include</dt><dd>Age, health, disability, partnership status and previous care needs.</dd>
<dt>Updates</dt><dd>Care receipt and provision, including hours and sources of care. Informal care uses time in the labour-supply decision; formal care creates expenditure needs.</dd>
</dl>
<p class="annual-explorer__links"><a href="modules/social-care/">Care receipt and provision</a></p>
</div>
</details>
<details class="annual-explorer__stage" id="cycle-investment">
<summary>Investment income</summary>
<div class="annual-explorer__content">
<dl>
<dt>Population</dt><dd>Individuals receiving private pensions or capital income; benefit units with explicitly modelled wealth.</dd>
<dt>Inputs include</dt><dd>Retirement status, previous income and personal circumstances, or net assets and assumed returns when wealth is tracked.</dd>
<dt>Updates</dt><dd>Private pension and capital income. These are projected using estimated equations when wealth is not tracked explicitly, or linked to asset returns and pension annuities when it is.</dd>
</dl>
<p class="annual-explorer__links"><a href="modules/investment-income/">Pensions and capital income</a></p>
</div>
</details>
<details class="annual-explorer__stage" id="cycle-employment">
<summary>Labour and disposable income</summary>
<div class="annual-explorer__content">
<dl>
<dt>Population</dt><dd>Eligible adults, with joint choices for couples where both partners are eligible; benefit units for tax–benefit calculations.</dd>
<dt>Inputs include</dt><dd>Potential wages, family composition, other income, care commitments and precomputed tax–benefit results.</dd>
<dt>Updates</dt><dd>Working hours, employment income and disposable income. The linked modules evaluate the resources available under work alternatives, then apply the labour-supply specification to select an outcome.</dd>
</dl>
<p class="annual-explorer__links"><a href="modules/labour-income/">Labour income</a><a href="modules/disposable-income/">Disposable income</a></p>
</div>
</details>
<details class="annual-explorer__stage" id="cycle-consumption">
<summary>Consumption and wealth</summary>
<div class="annual-explorer__content">
<dl>
<dt>Population</dt><dd>Benefit units and the people whose needs they support.</dd>
<dt>Inputs include</dt><dd>Disposable income, family composition and care costs; net wealth and available credit under the forward-looking specification.</dd>
<dt>Updates</dt><dd>Consumption and, when explicitly tracked, net wealth. The default uses an assumed saving rate; the forward-looking specification chooses discretionary consumption jointly with employment.</dd>
</dl>
<p class="annual-explorer__links"><a href="modules/consumption/">Consumption and asset accumulation</a></p>
</div>
</details>
<details class="annual-explorer__stage" id="cycle-mental-health">
<summary>Mental-health adjustments</summary>
<div class="annual-explorer__content">
<dl>
<dt>Population</dt><dd>People aged 16 and over in the psychological-distress processes.</dd>
<dt>Inputs include</dt><dd>Baseline psychological distress, employment transitions, poverty and income changes, and pandemic-period adjustments where applicable.</dd>
<dt>Updates</dt><dd>GHQ-12 distress scores and an indicator of potentially clinically significant common mental disorder. The adjustments incorporate changes in economic circumstances into the mental-health projections.</dd>
</dl>
<p class="annual-explorer__links"><a href="modules/mental-health/">Psychological distress and economic transitions</a></p>
</div>
</details>
</div>
<p class="annual-explorer__note">The module order describes when calculations occur. Dependencies also depend on each equation’s use of current or lagged values; the sequence alone does not establish a causal relationship.</p>
</div>
</details>

In the current schedule, each cycle stores previous-year states and updates ages, education, homeownership, general health and potential wages. Partnership and fertility processes establish family circumstances before care, labour supply and income are evaluated. Consumption and income changes are then calculated, followed by financial distress, mental-health and wellbeing outcomes. Mortality and population alignment are applied towards the end of the year. The initial year uses a reduced schedule because many starting characteristics are supplied by the input data.

The sequence determines when updated information becomes available. Each equation specifies whether it uses a current characteristic or a **lagged** value from an earlier year. Together, the schedule and these lag specifications determine whether a change can influence another outcome within the same year or in a later cycle. The [simulation outputs](modules/statistical-display.md) include annual summary statistics and records for people, benefit units and households, from which distributions and life histories can be analysed.

### Transitions and individual variation

Consider the education equation E1a. For someone who was a student in the preceding year and is within the eligible age range, the probability of remaining a student is

$$
p_{it}=\Phi\!\left(\mathbf{x}_{it}'\widehat{\boldsymbol\beta}+a_t\right).
$$

Here, $\mathbf{x}_{it}$ contains the person's individual and family characteristics, $\widehat{\boldsymbol\beta}$ the estimated coefficients, and $\Phi$ the standard normal cumulative distribution function. The adjustment $a_t$ aligns student participation to an aggregate target when enabled. A uniform random draw between zero and one determines the event: the person remains a student if the draw is below $p_{it}$; otherwise, they leave and their attainment is evaluated. [Education](modules/education.md) specifies the covariates, age rules and separate re-entry equation.

This calculation illustrates how an estimated relationship becomes an individual event. Other processes use ordered or multinomial probabilities, or continuous predictions with residual draws. Their covariates can include previous states and outcomes from other modules, carrying individual histories and changes in circumstances into subsequent projections.

### Economic decisions and policy

Two specifications describe economic choices as utility maximisation at benefit-unit level:

| Aspect | Default within-period specification | Forward-looking specification |
| --- | --- | --- |
| Criterion | Utility from current disposable income and leisure. | Expected utility from consumption and leisure over the remaining lifetime. |
| Work | Discrete hours alternatives, including non-employment; joint choices for two eligible partners. | Employment chosen jointly with discretionary consumption. |
| Consumption | Derived from disposable income, with an assumed saving adjustment for non-retired people. | Chosen using current resources and expectations of future circumstances. |
| Saving and wealth | Saving represented implicitly through the consumption rule. | Income less expenditure updates net wealth; borrowing is subject to a net-wealth constraint. |

In the default **discrete-choice random utility model**, potential wages and other income establish resources under each hours alternative. Matching family circumstances and earnings to precomputed UKMOD donor records supplies disposable income. Estimated utility scores for income and leisure are converted into choice probabilities using a conditional-logit formula; a benefit-unit random draw selects the realised alternative. A reform changes the incomes associated with those alternatives and can therefore change work choices.

Both specifications produce dynamic life histories. The forward-looking model also makes future consequences explicit in current decisions: anticipated care costs, for example, can influence saving before those costs arise. [Labour income](modules/labour-income.md) and [Consumption](modules/consumption.md) describe the decision problems in detail.

## Defining and interpreting a projection

A projection is defined by its initial population, estimated parameters, policy settings, economic specification and enabled processes. External trajectories for population, prices, wages, asset returns and borrowing costs describe the environment. [Parameterisation](parameterisation.md) explains these inputs.

**Alignment** brings selected outcomes towards external targets by adjusting different parts of the calculation. Examples include:

| Process | What alignment adjusts |
| --- | --- |
| Student participation | The probit adjustment $a_t$ changes probabilities of remaining in education. |
| Default employment | Utility adjustments bring subgroup employment rates towards their targets when employment alignment is enabled. |
| Population | Simulated counts, or weights in the weighted implementation, are adjusted to demographic totals by age, gender and region. |

Targets, the years in which they apply, and adjustments retained in later years are part of the specification. Agreement with an imposed target is not independent evidence of predictive accuracy; [Validation](../validation/index.md) explains how model performance is assessed.

Policy analysis compares scenarios with explicitly defined differences in policy or other assumptions, holding the remaining settings fixed. Differences in outcomes reflect both the direct changes and responses transmitted through the model's relationships; their interpretation depends on those relationships and each scenario's alignment settings.

Varying random draws while holding coefficients and other inputs fixed assesses **simulation variation**. Resampling estimated coefficients examines **parameter uncertainty**; changing equations or external assumptions examines sensitivity to the specification or scenario. Changing a seed can affect both simulation draws and coefficient resampling. [Uncertainty analysis](../user-guide/uncertainty-analysis.md) explains how to distinguish these comparisons in practice.

For the model's design and methodological foundations, see [Bronka et al. (2025), *SimPaths: an open-source microsimulation model for life course analysis*](https://microsimulation.pub/articles/00318).

<div class="model-overview__related" markdown>

[Development roadmap](roadmap.md) · [How to cite](how-to-cite.md)

</div>

</div>
