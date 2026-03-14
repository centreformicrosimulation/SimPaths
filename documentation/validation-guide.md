# Validation Guide

SimPaths uses a two-stage validation workflow in `validation/`. Stage 1 checks that each estimated regression model is well-specified before simulation; stage 2 checks that full simulation output matches observed survey data.

---

## Stage 1 — Estimate validation (`validation/01_estimate_validation/`)

**When to run:** After updating or re-estimating any regression module (i.e. after re-running scripts in `input/InitialPopulations/compile/RegressionEstimates/`).

**What it does:** For each behavioural module, the script loads the estimation sample, computes predicted values from the estimated coefficients, adds individual heterogeneity via 20 stochastic draws (as in multiple imputation), and overlays the predicted and observed distributions as histograms.

| Script | Module validated |
|--------|----------------|
| `int_val_wages.do` | Hourly wages — Heckman selection model, separately for males/females with and without previous wage history |
| `int_val_education.do` | Education transitions (3 processes) |
| `int_val_fertility.do` | Fertility (2 processes) |
| `int_val_health.do` | Physical health transitions |
| `int_val_home_ownership.do` | Homeownership transitions |
| `int_val_income.do` | Income processes — hurdle models (selection and amount) |
| `int_val_leave_parental_home.do` | Leaving parental home |
| `int_val_partnership.do` | Partnership formation and dissolution |
| `int_val_retirement.do` | Retirement transitions |

**Outputs:** PNG graphs saved under `validation/01_estimate_validation/graphs/<module>/`. Each graph shows predicted (red) vs observed (black outline) distributions. If the shapes diverge substantially, the regression may be mis-specified or the estimation sample may need updating.

---

## Stage 2 — Simulated output validation (`validation/02_simulated_output_validation/`)

**When to run:** After completing a baseline simulation run that you want to assess for plausibility.

**What it does:** Loads your simulation output CSVs, loads UKHLS initial population data as an observational benchmark, and produces side-by-side time-series plots comparing 18 simulated outcomes against the observed distributions with confidence intervals.

### Setup

Before running, open `00_master.do` and set the global paths:

```stata
global path     "/your/local/path/to/validation/02_simulated_output_validation"
global dir_sim  "/your/output/<run-folder>/csv"   * folder with simulation CSVs
global dir_obs  "/path/to/ukhls/initial/populations"
```

Then run `00_master.do`. It calls all sub-scripts in order.

### Scripts and what they check

**Data preparation (run first, automatically called by master):**

| Script | Purpose |
|--------|---------|
| `01_prepare_simulated_data.do` | Loads `Household.csv`, `BenefitUnit.csv`, `Person.csv` from the simulation output |
| `02_create_simulated_variables.do` | Derives analysis variables (sex, age groups, labour supply, income); produces full sample and ages 18–65 subset |
| `03_prepare_UKHLS_data.do` | Loads UKHLS observed data; prepares disposable income and matching variables |
| `05_create_UKHLS_validation_targets.do` | Creates target variables from UKHLS initial population CSVs by year |

**Comparison plots (18 scripts, `06_01` through `06_18`):**

| Script | What is compared |
|--------|-----------------|
| `06_01_plot_activity_status.do` | Economic activity: employed, student, inactive, retired by age group |
| `06_02_plot_education_level.do` | Completed education distribution over time |
| `06_03_plot_gross_income.do` | Gross benefit-unit income |
| `06_04_plot_gross_labour_income.do` | Gross labour income |
| `06_05_plot_capital_income.do` | Capital income (interest, dividends) |
| `06_06_plot_pension_income.do` | Pension income |
| `06_07_plot_disposable_income.do` | Disposable income after taxes and benefits |
| `06_08_plot_equivalised_disposable_income.do` | Household-size-adjusted disposable income |
| `06_09_plot_hourly_wages.do` | Hourly wages for employees |
| `06_10_plot_hours_worked.do` | Weekly hours worked by employment status |
| `06_11_plot_income_shares.do` | Income distribution across quintiles |
| `06_12_plot_partnership_status.do` | Partnership status (single, married, cohabiting, previously partnered) |
| `06_13_plot_health.do` | Physical and mental health (SF-12 PCS and MCS) |
| `06_14_plot_at_risk_of_poverty.do` | At-risk-of-poverty rate |
| `06_15_plot_inequality.do` | Income inequality (p90/p50 ratio) |
| `06_16_plot_number_children.do` | Number of dependent children |
| `06_17_plot_disability.do` | Disability prevalence |
| `06_18_plot_social_care.do` | Social care receipt |

**Correlation analysis:**

| Script | Purpose |
|--------|---------|
| `07_01_correlations.do` | Checks that key relationships between variables (e.g. income and employment, health and age) are preserved in the simulated data relative to UKHLS |

**Outputs:** PNG graphs saved under `validation/02_simulated_output_validation/graphs/<run-id>/`, organised by topic (income, health, inequality, partnership, etc.). A reference set from a named run (`20250909_run`) is already committed and can serve as a baseline for comparison.

---

## Interpreting results

- **Stage 1:** Predicted and observed histograms should broadly overlap. Systematic divergence (e.g. predicted wages consistently too high) indicates a problem with the estimation or variable construction.
- **Stage 2:** Simulated time-series should track UKHLS trends within reasonable uncertainty bounds. Large divergence in levels suggests a miscalibration; divergence in trends suggests a missing time-series process or a misspecified time-trend parameter.

The validation suite does not produce a single pass/fail metric — it is a diagnostic tool to inform judgement about whether a given parameterisation is fit for the intended research purpose.
