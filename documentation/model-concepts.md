# Model Concepts

This page explains what SimPaths simulates, how it is structured, and how its core mechanisms work. It is intended as the conceptual companion to the operational guides (getting-started, configuration, cli-reference).

---

## What SimPaths is

SimPaths is a dynamic population microsimulation model. It takes a sample of real households as a starting population and advances them forward in time, year by year, simulating individual life events using a combination of statistical regression models and rule-based processes.

The output is a longitudinal synthetic population whose trajectories can be used to study policy scenarios, distributional outcomes, and the long-run consequences of demographic and economic change.

### Supported countries

| Code | Country |
|------|---------|
| `UK` | United Kingdom |
| `IT` | Italy |

Country selection affects which initial population, regional classifications, EUROMOD policy schedule, and regression coefficients are loaded. The two countries share the same model structure but are fully parameterised separately.

---

## Agent hierarchy

The simulation maintains three nested entity types.

### Person

The individual. Each person carries their own demographic, health, education, labour, and income attributes. Almost all behavioural processes (health, education, fertility, partnership, labour supply) are resolved at the person level.

Key attributes tracked per person include:

- **Demographics**: age, gender, region
- **Education**: highest qualification (Low / Medium / High / InEducation), mother's and father's education
- **Labour market status**: one of `EmployedOrSelfEmployed`, `NotEmployed`, `Student`, `Retired`; weekly hours worked; wage rate; work history in months
- **Health**: physical health (SF-12 PCS), mental health (SF-12 MCS, GHQ-12 psychological distress score, caseness indicator), life satisfaction, EQ-5D utility score, disability / care-need flag
- **Partnership**: partner reference, years in partnership
- **Income**: gross labour income, capital income, pension income, benefit receipt flags (UC and non-UC)
- **Social care**: formal and informal care hours received per week; care provision hours per week
- **Financial wellbeing**: equivalised disposable income, lifetime income trajectory, financial distress flag

### BenefitUnit

The tax-and-benefit assessment unit — typically an adult (or a couple) and their dependent children. Benefits and taxes are computed at this level, mirroring how real-world tax-benefit systems work.

Key attributes include:

- region, homeownership flag
- equivalised disposable income (EDI) and year-on-year change in log-EDI
- poverty flag (< 60 % of median equivalised household disposable income)
- discretionary consumption (when intertemporal optimisation is enabled)

### Household

A grouping of benefit units sharing an address. Used for aggregation and housing-related logic. A household may contain more than one benefit unit (for example, adult children living with parents before leaving home).

---

## Annual simulation cycle

SimPaths uses **discrete annual time steps**. Within each year, processes fire in a fixed order. The table below lists the ordered steps as scheduled in `SimPathsModel.buildSchedule()`.

| # | Process | Level | Description |
|---|---------|-------|-------------|
| 1 | StartYear | model | Housekeeping, year logging |
| 2 | RationalOptimisation | model | *First year only.* Pre-computes intertemporal decision grids (if enabled) |
| 3 | UpdateParameters | model | Loads year-specific parameters and time-series factors |
| 4 | GarbageCollection | model | Removes stale entity references |
| 5 | UpdateWealth | benefit unit | Updates savings/wealth stocks (if intertemporal enabled) |
| 6 | Update | benefit unit | Refreshes household composition counts, clears state flags |
| 7 | Update | person | Refreshes individual-level state variables and lags |
| 8 | **Aging** | person | Increments age; checks whether individuals age out of the population |
| 9 | ConsiderRetirement | person | Stochastic retirement decision |
| 10 | **InSchool** | person | Whether person remains in / enters education (age 16–29) |
| 11 | InSchoolAlignment | model | Aligns school participation rates to targets (if enabled) |
| 12 | LeavingSchool | person | Transition out of education |
| 13 | EducationLevelAlignment | model | Aligns completed education distribution (if enabled) |
| 14 | Homeownership | benefit unit | Homeownership transition |
| 15 | **Health** | person | Updates physical health and disability status |
| 16 | UpdatePotentialHourlyEarnings | person | Refreshes wage potential for labour supply decisions |
| 17 | CohabitationAlignment | model | Aligns cohabitation share to targets (if enabled) |
| 18 | **Cohabitation** | person | Entry into partnership |
| 19 | PartnershipDissolution | person | Exit from partnership (separation / bereavement) |
| 20 | **UnionMatching** | model | Matches unpartnered individuals into new couples |
| 21 | FertilityAlignment | model | Aligns birth rates to projected fertility (if enabled) |
| 22 | **Fertility** | person | Fertility decision for women of childbearing age |
| 23 | GiveBirth | person | Adds newborn children to simulation |
| 24 | SocialCareReceipt | person | Care receipt (formal and informal) for those with care need |
| 25 | SocialCareProvision | person | Provision of informal care by eligible individuals |
| 26 | **Unemployment** | person | Unemployment transitions |
| 27 | UpdateStates | benefit unit | Refreshes joint labour states for IO decision (if enabled) |
| 28 | **LabourMarketAndIncomeUpdate** | model | Resolves labour supply, imputes taxes and benefits via EUROMOD donor matching |
| 29 | ReceivesBenefits | benefit unit | Assigns benefit receipt flags from donor match |
| 30 | ProjectDiscretionaryConsumption | benefit unit | Consumption/savings decision (if intertemporal enabled) |
| 31 | ProjectEquivConsumption | person | Computes individual equivalised consumption share |
| 32 | CalculateChangeInEDI | benefit unit | Updates equivalised disposable income and year-on-year change |
| 33 | ReviseLifetimeIncome | person | Updates lifetime income trajectory (if intertemporal enabled) |
| 34 | **FinancialDistress** | person | Financial distress indicator |
| 35–40 | **Mental health and wellbeing** | person | GHQ-12 psychological distress (levels and caseness, two-step); SF-12 MCS and PCS; life satisfaction (all two-step) |
| 41 | **ConsiderMortality** | person | Stochastic mortality |
| 42 | HealthEQ5D | person | EQ-5D utility score update |
| 43 | PopulationAlignment | model | Re-weights or resamples population to match demographic projections |
| 44 | EndYear / UpdateYear | model | Year-end housekeeping |

---

## Modules in depth

### Education

Individuals aged 16–29 are assessed each year for whether they remain in education (`InSchool`) and whether they have left (`LeavingSchool`). Upon leaving, their highest completed qualification (Low / Medium / High) is determined. Parent education levels are tracked as covariates in child education models.

Optional alignment (`alignInSchool`, `alignEducation`) can anchor simulated shares to empirical targets.

### Health

Physical health is updated annually using regression models. The result feeds into disability and care-need flags, which then govern social care processes.

Mental health and wellbeing are resolved later in the cycle (after income is determined), reflecting the evidence that material conditions affect mental health outcomes. Multiple constructs are tracked:

- **GHQ-12 psychological distress** — continuous score (0–12 Likert) and caseness indicator, each resolved in two steps (baseline prediction + exposure adjustment)
- **SF-12 MCS/PCS** — mental and physical component summary scores, two-step
- **Life satisfaction** — 0–10 score, two-step
- **EQ-5D** — health utility index updated at year end

### Partnership

Cohabitation entry is modelled at the person level; union matching is handled at the model level via a matching algorithm that pairs eligible singles. Partnership dissolution (separation or death of partner) is also modelled stochastically. Alignment of cohabitation shares to targets is available via `alignCohabitation`.

### Fertility

Women of childbearing age receive a fertility draw each year. A separate alignment step (`FertilityAlignment`) can scale individual probabilities to match aggregate fertility projections from population statistics.

### Social care

When `projectSocialCare` is enabled, individuals with a care need draw formal and informal care hours. Separate provision processes model informal care given by family members and others. A market-clearing step can reconcile supply and demand.

### Labour market and income

Labour supply is resolved for each benefit unit by choosing over a discrete set of hours options for each adult. The model supports:

- **Intertemporal optimisation** (`enableIntertemporalOptimisations`): decision grids are pre-computed in the first year; each subsequent year agents select hours to maximise inter-period utility given expected future income.
- **Static labour supply** (default): hours are drawn from regression models without forward-looking optimisation.

After hours are chosen, taxes and benefits are imputed using **EUROMOD donor matching** (see below).

### Homeownership

Homeownership transitions are modelled at the benefit unit level using a regression model, updating the homeownership flag each year.

### Population alignment

At the end of each year, `PopulationAlignment` re-weights or resamples the population to keep aggregate age-sex distributions consistent with external demographic projections. This ensures the simulated population does not drift away from official forecasts over long horizons.

---

## Alignment

Alignment is a technique used in microsimulation to prevent simulated aggregate rates from drifting away from known targets. Rather than discarding individual-level stochastic variation, alignment rescales or resamples agents' outcomes so that the population total matches a target share or count.

SimPaths uses alignment for several dimensions, each controlled by a boolean flag in the config:

| Flag | What it aligns | Default |
|------|---------------|---------|
| `alignPopulation` | Age-sex population totals to demographic projections | `true` |
| `alignCohabitation` | Share of individuals in partnerships | `true` |
| `alignFertility` | Birth rates to projected fertility rates | `false` |
| `alignInSchool` | School participation rate (age 16–29) | `false` |
| `alignEducation` | Completed education level distribution | `false` |
| `alignEmployment` | Employment share | `false` |

---

## EUROMOD integration and tax-benefit imputation

SimPaths does not compute taxes and benefits directly from first principles. Instead it uses a **donor matching** approach:

1. Before or at the start of a run, a database of tax-benefit outcomes is generated by running EUROMOD (a static tax-benefit microsimulation model) over a population of "donor" households across each policy year.
2. During simulation, each benefit unit selects a donor whose characteristics (labour supply hours, earnings, household composition, region, year) closely match its own.
3. The donor's EUROMOD-calculated disposable income, tax liability, and benefit amounts are imputed to the simulated benefit unit.

This means simulated households benefit from EUROMOD's detailed and annually updated policy rules without requiring SimPaths to re-implement the full tax-benefit schedule. The policy schedule loaded per simulation year is controlled by `input/EUROMODpolicySchedule.xlsx`.

---

## Intertemporal optimisation

When `enableIntertemporalOptimisations: true`, SimPaths solves a life-cycle consumption and labour supply problem. Decision grids are pre-computed in year 0 (`RationalOptimisation`) by solving backwards over the remaining simulation horizon. At each subsequent year, agents look up their optimal choice from the pre-computed grid given their current state.

This is computationally intensive. It is disabled by default. The sensitivity of behaviour to assumed interest rates and labour income can be explored using the `interestRateInnov` and `disposableIncomeFromLabourInnov` parameters.

---

## Key input files

Most files in `input/` are regression coefficient tables (`reg_*.xlsx`), alignment targets (`align_*.xlsx`), and scenario overrides (`scenario_*.xlsx`). The most important ones to understand:

| File | Purpose |
|------|---------|
| `input/EUROMODpolicySchedule.xlsx` | Maps simulation years to EUROMOD policy systems; generated by setup |
| `input/DatabaseCountryYear.xlsx` | Country- and year-specific macro parameters (wages, prices, etc.) |
| `input/input.mv.db` | H2 database containing the donor tax-benefit unit pool; generated by setup |
| `input/InitialPopulations/…/population_initial_UK_2019.csv` | Starting population cross-section |
| `input/reg_labourSupplyUtility.xlsx` | Labour supply utility function coefficients |
| `input/reg_lifetime_incomes.xlsx` | Lifetime income projection coefficients (used by IO module) |
| `input/align_popProjections.xlsx` | Official population projections used for demographic alignment |
