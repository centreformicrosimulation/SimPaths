---
title: Model overview
hide:
  - footer
---

<div class="model-overview" id="model" markdown>

# Model overview {#model-description}

SimPaths is an open-source dynamic microsimulation model for studying how living standards and inequalities develop over the life course. Starting from a population derived from survey data, it projects changes in education, family composition, health, care, employment and financial circumstances in annual steps. It represents individuals within families and households, preserving links between partners and between parents and dependent children as their circumstances change. Births, deaths and migration also alter the population. The resulting simulated histories allow researchers to examine the distribution of outcomes in each year and how those outcomes develop over individual lives.

Demographic and health transitions are largely described by **reduced-form equations** estimated from survey data. These relate outcomes such as partnership formation or a change in health to individual and family characteristics, including previous states. Labour supply is represented by a **structural behavioural model**: eligible adults are assumed to choose working hours to maximise utility from disposable income and leisure, with preferences estimated from observed choices. Tax–benefit rules enter this decision through the disposable income associated with each alternative. A reform can therefore change both a family's income at given hours of work and its incentives to work. An alternative, forward-looking specification extends the decision problem to consumption and saving over the remaining lifetime.

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

The [consumption module](modules/consumption.md) represents expenditure, including formal childcare and social-care costs. The default specification derives consumption from disposable income using an assumed saving rate. It expresses consumption in equivalised terms, adjusting for family size and composition. With forward-looking economic decisions, discretionary consumption is chosen explicitly and saving follows from income less expenditure. Net wealth is carried between periods, affecting future resources through investment returns and the financing of consumption. Homeownership is also represented as a state used in mental-health projections.

</section>

## How the simulation operates {#how-the-model-works}

### The annual cycle

The modules operate in a defined [annual sequence](../developer-guide/internals/simpaths-model.md). The diagram shows the module cycle used in the published model description.

<figure class="module-diagram">
  <a href="https://github.com/simpaths/SimPaths/assets/56582427/d4c773a2-b720-4546-bca6-c76d07282dc4" target="_blank" rel="noopener" aria-label="Open the annual sequence diagram at full size">
    <img src="https://github.com/simpaths/SimPaths/assets/56582427/d4c773a2-b720-4546-bca6-c76d07282dc4" alt="Published annual module cycle: ageing, education, health, family composition, social care, investment income, linked labour-income and disposable-income calculations, consumption and wealth, and mental-health adjustments, returning to the next year." width="2770" height="1810" loading="lazy" decoding="async">
  </a>
  <figcaption>Published module cycle. In the current schedule, mental-health processes follow the income calculations and population alignment runs at year end. <a href="https://github.com/simpaths/SimPaths/assets/56582427/d4c773a2-b720-4546-bca6-c76d07282dc4" target="_blank" rel="noopener">Open the diagram at full size.</a></figcaption>
</figure>

<details class="annual-explorer" data-annual-explorer markdown="0">
<summary>Inspect the modules</summary>
<div class="annual-explorer__body">
<p class="annual-explorer__intro">Select a module to see the population it acts on, examples of its inputs and the states it updates. This guide follows the modules in the published diagram; the current event schedule is described below.</p>
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

Many demographic, family and health outcomes use equations estimated from survey data. For a discrete outcome, an equation supplies probabilities conditional on the person's characteristics and family circumstances; random draws determine individual realisations. Continuous outcomes can include a draw from an estimated residual distribution. Eligibility rules restrict the population to which a process applies, and procedures such as partner matching resolve relationships between individuals.

Dependence on past states gives these transitions persistence. Their dependence on characteristics projected by other modules transmits changes between domains: leaving education can alter employment eligibility and wage potential; a partnership or birth can alter income needs and benefit entitlements.

### Economic decisions and policy

The default labour-supply specification uses a **discrete-choice random utility model** to evaluate income and leisure in the current year. Eligible adults face working-hours alternatives, including non-employment; for couples with two eligible adults, choices are combinations of both partners' hours. Estimated preferences over each alternative's disposable income and leisure define choice probabilities, allowing for unobserved variation in preferences.

To construct these alternatives, SimPaths combines potential wages, other income and family circumstances with precomputed tax–benefit results from UKMOD. Results are imputed by matching to a donor database containing the relevant policy calculations. Changing the policy system changes the income available at different working hours, allowing a reform to affect both disposable income and labour supply. UKMOD supplies the tax–benefit calculations in advance; SimPaths evaluates their consequences as people and families change.

The alternative **forward-looking specification** solves a joint employment and consumption problem based on expected utility over the remaining lifetime. Current choices depend on expectations of future earnings, family circumstances and needs, as well as on current resources. Saving and borrowing transfer resources between periods, subject to a net-wealth constraint. Both specifications produce dynamic life histories; the distinction is whether future consequences enter the economic decision problem explicitly.

## Defining and interpreting a projection

A projection depends on the initial population, estimated parameters, policy settings and external demographic and economic assumptions. Population projections, price and wage indices, returns on assets and borrowing costs supply trajectories for the environment in which individual lives develop. The chosen economic specification and the processes enabled for a run also determine which relationships operate. [Parameterisation](parameterisation.md) describes these inputs and their roles.

Selected outcomes are **aligned** to external targets. Population alignment adjusts simulated counts by age, gender and region to demographic projections. Other alignment procedures adjust transition probabilities or decision parameters, for example to reproduce observed employment rates. These targets are part of the specification: their use needs to be distinguished from comparisons with observations used to assess model performance, as described in [Validation](../validation/index.md).

Policy analysis compares projections under explicitly defined alternatives, such as different tax–benefit systems applied to the same starting population. The resulting differences reflect the reform's direct effects and the responses transmitted through the model's specified relationships. Their interpretation depends on the empirical and behavioural assumptions underlying those relationships. [Uncertainty analysis](../user-guide/uncertainty-analysis.md) addresses simulation variation, uncertainty in estimated parameters and sensitivity to alternative assumptions.

For the model's design and methodological foundations, see [Bronka et al. (2025), *SimPaths: an open-source microsimulation model for life course analysis*](https://microsimulation.pub/articles/00318).

<div class="model-overview__related" markdown>

[Development roadmap](roadmap.md) · [How to cite](how-to-cite.md)

</div>

</div>
