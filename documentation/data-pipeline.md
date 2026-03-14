# Data Pipeline

This page explains how the simulation-ready input files in `input/` are generated from raw survey data, and what to do if you need to update or extend them.

The pipeline has three independent parts: (1) initial populations, (2) regression coefficients, (3) alignment targets. Each can be re-run separately.

---

## Data sources

| Source | Description | Access |
|--------|-------------|--------|
| **UKHLS** (Understanding Society) | Main household panel survey; waves 1 to O (UKDA-6614-stata) | Requires EUL licence from UK Data Service |
| **BHPS** (British Household Panel Survey) | Historical predecessor to UKHLS; used for pre-2009 employment history | Bundled with UKHLS EUL |
| **WAS** (Wealth and Assets Survey) | Biennial survey of household wealth; waves 1 to 7 (UKDA-7215-stata) | Requires EUL licence from UK Data Service |
| **EUROMOD / UKMOD** | Tax-benefit microsimulation system | See [Tax-Benefit Donors (UK)](../documentation/wiki/getting-started/data/tax-benefit-donors-uk.md) on the website |

---

## Part 1 — Initial populations (`input/InitialPopulations/compile/`)

**What it produces:** Annual CSV files `population_initial_UK_<year>.csv` used as the starting population for each simulation run.

**Master script:** `input/InitialPopulations/compile/00_master.do`

The pipeline runs in numbered stages:

| Script | What it does |
|--------|-------------|
| `01_prepare_UKHLS_pooled_data.do` | Pools and standardises UKHLS waves |
| `02_create_UKHLS_variables.do` | Constructs all required variables (demographics, labour, health, income, wealth flags) and applies simulation-consistency rules (retirement as absorbing state, education age bounds, work/hours consistency) |
| `02_01_checks.do` | Data quality checks |
| `03_social_care_received.do` | Social care receipt variables |
| `04_social_care_provided.do` | Informal care provision variables |
| `05_create_benefit_units.do` | Groups individuals into benefit units (tax units) following UK tax-benefit rules |
| `06_reweight_and_slice.do` | Reweighting and year-specific slicing |
| `07_was_wealth_data.do` | Prepares Wealth and Assets Survey data |
| `08_wealth_to_ukhls.do` | Merges WAS wealth into UKHLS records |
| `09_finalise_input_data.do` | Final cleaning and formatting |
| `10_check_yearly_data.do` | Per-year consistency checks |
| `99_training_data.do` | Extracts the small training subset committed to the repo |

### Employment history sub-pipeline (`compile/do_emphist/`)

Reconstructs each respondent's monthly employment history from January 2007 onwards by combining UKHLS and BHPS interview records. The output variable `liwwh` (months employed since Jan 2007) feeds into the labour supply models.

| Script | Purpose |
|--------|---------|
| `00_Master_emphist.do` | Master; sets parameters and calls sub-scripts |
| `01_Intdate.do` – `07_Empcal1a.do` | Sequential stages: interview dating, BHPS linkage, employment spell reconstruction, new-entrant identification |

---

## Part 2 — Regression coefficients (`input/InitialPopulations/compile/RegressionEstimates/`)

**What it produces:** The `reg_*.xlsx` coefficient tables read by `Parameters.java` at simulation startup.

**Master script:** `input/InitialPopulations/compile/RegressionEstimates/master.do`

> **Note:** Income and union-formation regressions depend on predicted wages, so `reg_wages.do` must complete before `reg_income.do` and `reg_partnership.do`. All other scripts can run in any order.

**Required Stata packages:** `fre`, `tsspell`, `carryforward`, `outreg2`, `oparallel`, `gologit2`, `winsor`, `reghdfe`, `ftools`, `require`

| Script | Module | Method |
|--------|--------|--------|
| `reg_wages.do` | Hourly wages | Heckman selection model (males and females separately) |
| `reg_income.do` | Non-labour income | Hurdle model (selection + amount); requires predicted wages |
| `reg_partnership.do` | Partnership formation/dissolution | Probit; requires predicted wages |
| `reg_education.do` | Education transitions | Generalised ordered logit |
| `reg_fertility.do` | Fertility | Probit |
| `reg_health.do` | Physical health (SF-12 PCS) | Linear regression |
| `reg_health_mental.do` | Mental health (GHQ-12, SF-12 MCS) | Linear regression |
| `reg_health_wellbeing.do` | Life satisfaction | Linear regression |
| `reg_home_ownership.do` | Homeownership transitions | Probit |
| `reg_retirement.do` | Retirement | Probit |
| `reg_leave_parental_home.do` | Leaving parental home | Probit |
| `reg_socialcare.do` | Social care receipt and provision | Probit / ordered logit |
| `reg_unemployment.do` | Unemployment transitions | Probit |
| `reg_financial_distress.do` | Financial distress | Probit |

After running, output Excel files are placed in `input/` (overwriting the existing `reg_*.xlsx` files).

---

## Part 3 — Alignment targets (`input/DoFilesTarget/`)

**What it produces:** The `align_*.xlsx` and `*_targets.xlsx` files that the alignment modules use to rescale simulated rates.

| Script | Output file |
|--------|------------|
| `01_employment_shares_initpopdata.do` | `input/employment_targets.xlsx` — employment shares by benefit-unit subgroup and year |
| `01_inSchool_targets_initpopdata.do` | `input/inSchool_targets.xlsx` — school participation rates by year |
| `03_calculate_partneredShare_initialPop_BUlogic.do` | `input/partnered_share_targets.xlsx` — partnership shares by year |
| `03_calculate_partnership_target.do` | Supplementary partnership targets |
| `02_person_risk_employment_stats.do` | `employment_risk_emp_stats.csv` — person-level at-risk diagnostics used for employment alignment group construction |

Population projection targets (`align_popProjections.xlsx`) and fertility/mortality projections (`projections_*.xlsx`) come from ONS published projections and are not generated by these scripts.

---

## When to re-run each part

| Situation | What to re-run |
|-----------|---------------|
| Adding a new data year to the simulation | Part 1 (re-slice the population for the new year) + Part 3 (update alignment targets) |
| Re-estimating a behavioural module | Part 2 (the affected `reg_*.do` script only) + Stage 1 validation |
| Updating employment alignment targets | Part 3 (`01_employment_shares_initpopdata.do`) |
| Adding a new country | All three parts with country-appropriate data sources |

After re-running any part, re-run setup (`singlerun -Setup` or `multirun -DBSetup`) to rebuild `input/input.mv.db` before running the simulation.
