# SimPaths Quick Reference

A command-line quick reference for building, running, configuring, and validating SimPaths. For the full model documentation — simulated modules, parameterisation, GUI usage, research — see the [website](../documentation/wiki/index.md).

---

## 1. Building and running

### Prerequisites

- Java 19
- Maven 3.8+
- Optional IDE: IntelliJ IDEA (import as a Maven project)

### Quick run

Three commands are all you need:

```bash
mvn clean package
java -jar multirun.jar -DBSetup
java -jar multirun.jar
```

The first builds the JARs. The second creates the H2 donor database from the input data. The third runs the simulation using `default.yml`.

To use a different config file:

```bash
java -jar multirun.jar -config my_run.yml
```

---

## 2. Configuration

SimPaths batch runs are controlled by YAML files in `config/`. The main config is `default.yml`, which is fully annotated with inline comments.

### How config is applied

`SimPathsMultiRun` loads `config/<file>` and applies values in two stages:

1. YAML values initialise runtime fields and argument maps.
2. CLI flags override those values if provided.

If a key is not specified in the YAML, the Java class field default is used. Each config file is standalone — there is no inheritance between config files.

### Writing your own config

Place a new `.yml` file in `config/` and pass it via `-config`. You only need to specify the values you want to change — everything else falls back to the Java class field defaults.

#### Core run arguments

| Key | Default | Description |
|-----|---------|-------------|
| `maxNumberOfRuns` | `1` | Number of sequential simulation runs |
| `executeWithGui` | `false` | `true` launches the JAS-mine GUI; `false` = headless (required on servers/CI) |
| `randomSeed` | `606` | RNG seed for the first run |
| `startYear` | `2019` | First simulation year (must have matching input/donor data) |
| `endYear` | `2022` | Last simulation year (inclusive) |
| `popSize` | `50000` | Simulated population size; larger = more accurate but slower |

#### Collector arguments

The `collector_args` section controls what output files are produced:

| Flag | Default | Description |
|------|---------|-------------|
| `persistStatistics` | `true` | Write `Statistics1.csv` — income distribution, Gini, S-Index |
| `persistStatistics2` | `true` | Write `Statistics2.csv` — demographic validation by age and gender |
| `persistStatistics3` | `true` | Write `Statistics3.csv` — alignment diagnostics |
| `exportToCSV` | `true` | Write outputs to CSV files under `output/<run>/csv/` |

For a description of the variables in these files, see `documentation/SimPaths_Variable_Codebook.xlsx`.

#### Minimal example

```yaml
maxNumberOfRuns: 5
executeWithGui: false
randomSeed: 42
startYear: 2019
endYear: 2030
popSize: 20000

collector_args:
  persistStatistics: true
  persistStatistics2: true
  persistStatistics3: true
```

### Additional arguments

The YAML file supports several other argument sections (`model_args`, `innovation_args`, `parameter_args`) that control alignment flags, intertemporal optimisation settings, sensitivity analysis parameters, and file paths. Many of these are for specific analyses and some are under active review. The annotated `default.yml` file documents all available keys with inline comments.

Note that some settings — particularly alignment — are primarily controlled in `SimPathsModel.java` rather than through the YAML file.

### Practical notes

- Use quotes around config filenames that contain spaces: `-config "my config.yml"`.
- Add `-f` to write run logs to `output/logs/`.
- Override individual values at runtime without editing the YAML, for example `-n 10` overrides `maxNumberOfRuns`.
- If you see `Config file <name> not found`, the `-config` flag points to a file not present in `config/` — check the filename and extension.
- If `EUROMODpolicySchedule.xlsx` is missing, re-run setup: `java -jar multirun.jar -DBSetup`.
- On headless servers or CI, always use `executeWithGui: false` in your YAML (or `-g false` on the command line) to avoid GUI errors.

---

## 3. Data pipeline

This section explains how the simulation-ready input files in `input/` are generated from raw survey data, and what to do if you need to update or extend them.

The pipeline has three independent parts: (1) initial populations, (2) regression coefficients, (3) alignment targets. Each can be re-run separately.

### Data sources

| Source | Description | Access |
|--------|-------------|--------|
| **UKHLS** (Understanding Society) | Main household panel survey; waves 1 to O (UKDA-6614-stata) | Requires EUL licence from UK Data Service |
| **BHPS** (British Household Panel Survey) | Historical predecessor to UKHLS; used for pre-2009 employment history | Bundled with UKHLS EUL |
| **WAS** (Wealth and Assets Survey) | Biennial survey of household wealth; waves 1 to 7 (UKDA-7215-stata) | Requires EUL licence from UK Data Service |
| **EUROMOD / UKMOD** | Tax-benefit microsimulation system | See [Tax-Benefit Donors (UK)](../documentation/wiki/getting-started/data/tax-benefit-donors-uk.md) on the website |

### Part 1 — Initial populations (`input/InitialPopulations/compile/`)

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
| `99_training_data.do` | Produces the de-identified training population committed to `input/InitialPopulations/training/` |

#### Employment history sub-pipeline (`compile/do_emphist/`)

Reconstructs each respondent's monthly employment history from January 2007 onwards by combining UKHLS and BHPS interview records. The output variable `liwwh` (months employed since Jan 2007) feeds into the labour supply models.

| Script | Purpose |
|--------|---------|
| `00_Master_emphist.do` | Master; sets parameters and calls sub-scripts |
| `01_Intdate.do` – `07_Empcal1a.do` | Sequential stages: interview dating, BHPS linkage, employment spell reconstruction, new-entrant identification |

### Part 2 — Regression coefficients (`input/InitialPopulations/compile/RegressionEstimates/`)

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
| `programs.do` | Shared utility programs called by the estimation scripts | — |
| `variable_update.do` | Prepares and recodes variables before estimation | — |

After running, output Excel files are placed in `input/` (overwriting the existing `reg_*.xlsx` files).

### Part 3 — Alignment targets (`input/DoFilesTarget/`)

**What it produces:** The `align_*.xlsx` and `*_targets.xlsx` files that the alignment modules use to rescale simulated rates.

| Script | Output file |
|--------|------------|
| `01_employment_shares_initpopdata.do` | `input/employment_targets.xlsx` — employment shares by benefit-unit subgroup and year |
| `01_inSchool_targets_initpopdata.do` | `input/inSchool_targets.xlsx` — school participation rates by year |
| `03_calculate_partneredShare_initialPop_BUlogic.do` | `input/partnered_share_targets.xlsx` — partnership shares by year |
| `03_calculate_partnership_target.do` | Supplementary partnership targets |
| `02_person_risk_employment_stats.do` | `employment_risk_emp_stats.csv` — person-level at-risk diagnostics used for employment alignment group construction |

Population projection targets (`align_popProjections.xlsx`) and fertility/mortality projections (`projections_*.xlsx`) come from ONS published projections and are not generated by these scripts.

### When to re-run each part

| Situation | What to re-run |
|-----------|---------------|
| Adding a new data year to the simulation | Part 1 (re-slice the population for the new year) + Part 3 (update alignment targets) |
| Re-estimating a behavioural module | Part 2 (the affected `reg_*.do` script only) + Stage 1 validation |
| Updating employment alignment targets | Part 3 (`01_employment_shares_initpopdata.do`) |

After re-running any part, re-run setup (`singlerun -Setup` or `multirun -DBSetup`) to rebuild `input/input.mv.db` before running the simulation.

---

## 4. Validation

SimPaths uses a two-stage validation workflow in `validation/`. Stage 1 checks that each estimated regression model is well-specified before simulation; stage 2 checks that full simulation output matches observed survey data. For the conceptual overview and detailed setup instructions, see [Model Validation](../documentation/wiki/validation/index.md) on the website.

### Stage 1 — Estimate validation (`validation/01_estimate_validation/`)

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

**Outputs:** PNG graphs saved under `validation/01_estimate_validation/graphs/<module>/`. Each graph shows predicted (red) vs observed (black outline) distributions.

### Stage 2 — Simulated output validation (`validation/02_simulated_output_validation/`)

**When to run:** After completing a baseline simulation run that you want to assess for plausibility.

**What it does:** Loads your simulation output CSVs, loads UKHLS initial population data as an observational benchmark, and produces side-by-side time-series plots comparing 18 simulated outcomes against the observed distributions with confidence intervals.

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

**Outputs:** PNG graphs saved under `validation/02_simulated_output_validation/graphs/<run-id>/`. A reference set from a baseline run (`20250909_run`) is already committed for comparison.

### Interpreting results

- **Stage 1:** Predicted and observed histograms should broadly overlap. Systematic divergence indicates a problem with the estimation or variable construction.
- **Stage 2:** Simulated time-series should track UKHLS trends within reasonable uncertainty bounds. Large divergence in levels suggests a miscalibration; divergence in trends suggests a missing time-series process or a misspecified time-trend parameter.

The validation suite does not produce a single pass/fail metric — it is a diagnostic tool to inform judgement about whether a given parameterisation is fit for the intended research purpose.
