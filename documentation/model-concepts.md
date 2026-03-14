# Model Concepts

SimPaths is a dynamic population microsimulation model that advances a starting population of real households forward in time, year by year, simulating individual life events through statistical regression models and rule-based processes. For the full academic description — including the 11 simulated modules — see the [Overview](../documentation/wiki/overview/index.md) section of the website, in particular [Simulated Modules](../documentation/wiki/overview/simulated-modules.md).

This page covers what you need to understand the **code and configuration**: agent structure, the annual process order, alignment flags, and the tax-benefit system.

---

## Agent hierarchy

The simulation maintains three nested entity types.

### Person

The individual. Each person carries their own demographic, health, education, labour, and income attributes. Almost all behavioural processes are resolved at the person level.

Key attributes tracked per person:

- **Demographics**: age, gender, region
- **Education**: highest qualification (`Low` / `Medium` / `High` / `InEducation`), mother's and father's education
- **Labour market status**: `EmployedOrSelfEmployed`, `NotEmployed`, `Student`, or `Retired`; weekly hours worked; wage rate; work history in months
- **Health**: physical health (SF-12 PCS), mental health (SF-12 MCS, GHQ-12 psychological distress, caseness indicator), life satisfaction (0–10), EQ-5D utility score, disability/care-need flag
- **Partnership**: partner reference, years in partnership
- **Income**: gross labour income, capital income, pension income, benefit receipt flags (UC and non-UC)
- **Social care**: formal and informal care hours received per week; informal care hours provided per week
- **Financial wellbeing**: equivalised disposable income, lifetime income trajectory, financial distress flag

### BenefitUnit

The tax-and-benefit assessment unit — typically an adult (or couple) and their dependent children. Taxes and benefits are computed here, mirroring how real-world tax-benefit systems work.

Key attributes:

- Region, homeownership flag, wealth
- Equivalised disposable income (EDI) and year-on-year change in log-EDI
- Poverty flag (< 60% of median equivalised household disposable income)
- Discretionary consumption (when intertemporal optimisation is enabled)

### Household

A grouping of benefit units sharing an address. Used for aggregation and housing-related logic. A household may contain more than one benefit unit (e.g. adult children living with parents before leaving home).

---

## Annual simulation cycle

SimPaths uses **discrete annual time steps**. Within each year, processes fire in a fixed order defined in `SimPathsModel.buildSchedule()`.

| # | Process | Level | Description |
|---|---------|-------|-------------|
| 1 | StartYear | model | Year logging and housekeeping |
| 2 | RationalOptimisation | model | *First year only.* Pre-computes intertemporal decision grids (if enabled) |
| 3 | UpdateParameters | model | Loads year-specific parameters and time-series factors |
| 4 | GarbageCollection | model | Removes stale entity references |
| 5 | UpdateWealth | benefit unit | Updates savings/wealth stocks (if intertemporal enabled) |
| 6 | Update | benefit unit | Refreshes composition counts, clears state flags |
| 7 | Update | person | Refreshes state variables and lag values |
| 8 | Aging | person | Increments age; dependent children reaching independence are split into their own benefit unit |
| 9 | ConsiderRetirement | person | Stochastic retirement decision |
| 10 | InSchool | person | Whether person remains in / enters education (age 16–29) |
| 11 | InSchoolAlignment | model | Aligns school participation rate to targets (if enabled) |
| 12 | LeavingSchool | person | Transition out of education; assigns completed qualification |
| 13 | EducationLevelAlignment | model | Aligns completed education distribution (if enabled) |
| 14 | Homeownership | benefit unit | Homeownership transition |
| 15 | Health | person | Updates physical health and disability status |
| 16 | UpdatePotentialHourlyEarnings | person | Refreshes wage potential prior to labour supply decisions |
| 17 | CohabitationAlignment | model | Aligns cohabitation share to targets (if enabled) |
| 18 | Cohabitation | person | Entry into partnership |
| 19 | PartnershipDissolution | person | Exit from partnership (separation or bereavement) |
| 20 | UnionMatching | model | Matches unpartnered individuals into new couples |
| 21 | FertilityAlignment | model | Scales birth probabilities to projected fertility rates (if enabled) |
| 22 | Fertility | person | Fertility decision for women of childbearing age |
| 23 | GiveBirth | person | Adds newborn children to the simulation |
| 24 | SocialCareReceipt | person | Formal and informal care receipt for those with a care need |
| 25 | SocialCareProvision | person | Informal care provision by eligible individuals |
| 26 | Unemployment | person | Unemployment transitions |
| 27 | UpdateStates | benefit unit | Refreshes joint labour states for IO decisions (if enabled) |
| 28 | LabourMarketAndIncomeUpdate | model | Resolves labour supply; imputes taxes and benefits via EUROMOD donor matching |
| 29 | ReceivesBenefits | benefit unit | Assigns benefit receipt flags from the donor match |
| 30 | ProjectDiscretionaryConsumption | benefit unit | Consumption/savings decision (if intertemporal enabled) |
| 31 | ProjectEquivConsumption | person | Computes individual equivalised consumption share |
| 32 | CalculateChangeInEDI | benefit unit | Updates equivalised disposable income and year-on-year change |
| 33 | ReviseLifetimeIncome | person | Updates lifetime income trajectory (if intertemporal enabled) |
| 34 | FinancialDistress | person | Financial distress indicator |
| 35–40 | Mental health and wellbeing | person | GHQ-12 distress (levels + caseness, two steps each); SF-12 MCS and PCS (two steps each); life satisfaction (two steps) |
| 41 | ConsiderMortality | person | Stochastic mortality |
| 42 | HealthEQ5D | person | EQ-5D utility score update |
| 43 | PopulationAlignment | model | Re-weights/resamples population to match demographic projections |
| 44 | EndYear / UpdateYear | model | Year-end housekeeping |

The first simulation year runs a subset of these (some states are inherited directly from input data). All subsequent years run the full schedule.

---

## Alignment

Alignment prevents simulated aggregate rates from drifting away from known targets. Rather than discarding individual-level stochastic variation, it rescales or resamples agents' outcomes so the population total matches a target share or count.

Each dimension is controlled by a boolean flag in `model_args`:

| Flag | What it aligns | Default |
|------|----------------|---------|
| `alignPopulation` | Age-sex-region population totals to demographic projections | `true` |
| `alignCohabitation` | Share of individuals in partnerships | `true` |
| `alignFertility` | Birth rates to projected fertility rates | `false` |
| `alignInSchool` | School participation rate (age 16–29) | `false` |
| `alignEducation` | Completed education level distribution | `false` |
| `alignEmployment` | Employment share | `false` |

---

## Tax-benefit system (EUROMOD donor matching)

SimPaths does not compute taxes and benefits from first principles. It uses **donor matching**:

1. A database of tax-benefit outcomes is pre-computed by running EUROMOD/UKMOD over a population of "donor" households for each policy year.
2. Each simulated benefit unit selects a donor whose characteristics (labour hours, earnings, household composition, region, year) closely match its own.
3. The donor's computed disposable income, tax, and benefit amounts are imputed to the simulated unit.

This gives SimPaths annually updated policy rules without re-implementing the full tax-benefit schedule. See [Tax-Benefit Donors (UK)](../documentation/wiki/getting-started/data/tax-benefit-donors-uk.md) for how to generate the donor database.

---

## Intertemporal optimisation

When `enableIntertemporalOptimisations: true`, SimPaths solves a life-cycle consumption and labour supply problem. Decision grids are pre-computed in year 0 (`RationalOptimisation`) by solving backwards over the remaining horizon. In each subsequent year agents look up their optimal choice from the grid given their current state.

This is computationally intensive and disabled by default. When enabled, `saveBehaviour` and `useSavedBehaviour` allow a baseline grid to be reused in counterfactual runs without recomputing it — see [Scenario Cookbook](scenario-cookbook.md) for an example.
