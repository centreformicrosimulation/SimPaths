---
hide:
  - toc
---

# SimPaths Repository Guide

A guide to navigating the SimPaths repository structure and codebase. Technical paths and examples in this guide were checked against [development revision b223738b9](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667) dated 28 August 2026. For an older release, inspect the corresponding source and input files.

---

## Table of Contents

1. [Repository Structure](#repository-structure)
2. [Core Components](#core-components)
3. [Key Directories Explained](#key-directories-explained)
4. [Sub-package Detail](#sub-package-detail)
5. [Data Pipeline Reference](#data-pipeline-reference)
6. [Development Workflow](#development-workflow)
7. [Code Navigation Tips](#code-navigation-tips)
8. [Additional Resources](#additional-resources)

---

## Repository Structure

```{.text .no-copy}
SimPaths/
├── config/                         # Configuration files for simulations
│   ├── default.yml                 # Default simulation parameters
│   ├── test_create_database.yml    # Database creation test config
│   └── test_run.yml                # Test run configuration
│
├── documentation/                  # Comprehensive documentation
│   ├── figures/                    # Diagrams and illustrations
│   ├── wiki/                       # Full documentation website
│   │   ├── getting-started/        # Setup and first simulation guides
│   │   ├── overview/               # Model description and modules
│   │   ├── user-guide/             # Running simulations
│   │   ├── developer-guide/        # Extending the model
│   │   │   └── repository-guide.md # Repository guide (copy for website)
│   │   ├── jasmine-reference/      # JAS-mine library reference
│   │   ├── research/               # Published papers
│   │   └── validation/             # Model validation results
│   ├── repository-guide.md         # Repository structure and navigation guide
│   ├── SimPaths_Variable_Codebook.xlsx    # Codebook of all variables in SimPaths
│   ├── SimPaths_Stata_Parameters.xlsx     # Comparison of parameters: Stata do-files vs Java code
│   └── SimPathsUK_Schedule.xlsx           # Detailed schedule of events and corresponding classes
│
├── input/                          # Input data and parameters
│   ├── InitialPopulations/         # Starting population data
│   │   ├── training/               # De-identified training population (included in repo)
│   │   └── compile/                # Stata pipeline: builds populations, estimates regressions
│   │       ├── do_emphist/         # Employment history reconstruction sub-pipeline
│   │       └── RegressionEstimates/  # Regression coefficient estimation scripts
│   ├── EUROMODoutput/              # Tax-benefit model outputs
│   │   └── training/               # Training UKMOD outputs (included in repo)
│   ├── DoFilesTarget/              # Stata scripts that generate alignment targets
│   ├── align_*.xlsx                # Alignment files (population, employment, etc.)
│   ├── reg_*.xlsx                  # Regression parameter files
│   ├── scenario_*.xlsx             # Scenario configuration files
│   ├── projections_*.xlsx          # Mortality/fertility projections
│   ├── DatabaseCountryYear.xlsx    # Database metadata
│   ├── EUROMODpolicySchedule.xlsx  # Policy schedule
│   ├── social_care_parameters.xlsx # Social care parameters
│   ├── validation_statistics.xlsx  # Validation targets
│   └── input.mv.db                 # Initial-population and donor database
│
├── output/                         # Simulation outputs
│   ├── [timestamp]_[seed]_[run]/   # Timestamped output folders
│   │   ├── csv/
│   │   │   ├── WealthIncomeStatistics*.csv  # Income and wealth; Gini, S-Index
│   │   │   ├── DemographicStatistics*.csv   # Demographics by age band
│   │   │   ├── AlignmentStatistics*.csv     # Alignment diagnostics
│   │   │   ├── LabourStatistics*.csv        # Labour market transitions and participation
│   │   │   ├── HealthStatistics*.csv        # Population health by age band
│   │   │   ├── WellbeingByGender*.csv       # Wellbeing by gender, ages 25-64
│   │   │   ├── Person*.csv               # Person-level output
│   │   │   ├── BenefitUnit*.csv          # Benefit-unit-level output
│   │   │   └── Household*.csv            # Household-level output
│   │   ├── database/                    # Run-specific persistence output
│   │   └── input/                       # Copied run input artifacts
│   └── logs/                       # Log files (with -f flag on multirun)
│
├── src/                            # Source code
│   ├── main/
│   │   ├── java/simpaths/
│   │   │   ├── data/               # Data handling and parameters
│   │   │   ├── experiment/         # Simulation execution classes
│   │   │   └── model/              # Core model implementation
│   │   │       ├── decisions/      # Intertemporal optimisation grids
│   │   │       ├── enums/          # Categorical variable definitions
│   │   │       ├── taxes/          # EUROMOD donor matching
│   │   │       └── lifetime_incomes/  # Synthetic income trajectory generation
│   │   └── resources/              # Configuration resources
│   └── test/                       # Test classes
│
├── validation/                     # Validation scripts and results
│   ├── 01_estimate_validation/     # Estimation validation
│   └── 02_simulated_output_validation/  # Output validation
│
├── pom.xml                         # Maven build configuration
├── singlerun.jar                   # Executable for single runs
├── multirun.jar                    # Executable for multiple runs
└── README.md                       # Project overview
```

---

## Core Components

### 1. Entry Points

#### SimPathsStart (`src/main/java/simpaths/experiment/SimPathsStart.java`)
- Main class for single simulation execution
- Handles GUI and command-line interfaces
- Manages database setup phases
- **Key methods**:
  - `main()`: Entry point
  - `runGUIdialog()`: Launch GUI
  - `runGUIlessSetup()`: Command-line setup

#### SimPathsMultiRun (`src/main/java/simpaths/experiment/SimPathsMultiRun.java`)
- Coordinates multiple simulation runs
- Executes runs sequentially
- Labels each run and applies the configured innovations
- Configurable via YAML files

### 2. Core Model

#### SimPathsModel (`src/main/java/simpaths/model/SimPathsModel.java`)
- Central simulation manager
- Implements `AbstractSimulationManager` from JAS-mine
- Defines the simulation schedule via `buildSchedule()`
- Manages all simulation modules and processes
- **Key responsibilities**:
  - Population initialization
  - Event scheduling
  - Module coordination
  - Time progression

### 3. Data & Parameters

#### Parameters (`src/main/java/simpaths/data/Parameters.java`)
- Global parameter storage
- Loads regression coefficients from Excel
- Manages country-specific configurations
- Stores alignment targets
- **Key data structures**:
  - Regression coefficient maps
  - Policy parameters
  - Alignment targets
  - EUROMOD variable definitions

---

## Key Directories Explained

### `/src/main/java/simpaths/`

#### `data/`
**Purpose**: Data handling, parameter management, and utility classes

- **Parameters.java**: Global parameter storage and Excel data loading
- **ManagerRegressions.java**: Regression coefficient management
- **CallEUROMOD.java** / **CallEMLight.java**: Interface with tax-benefit models
- **filters/**: Collection filters for querying simulated populations
- **startingpop/**: Initial population data parsing
- **statistics/**: Statistical utilities

#### `experiment/`
**Purpose**: Simulation execution and coordination

- **SimPathsStart.java**: Single-run entry point
- **SimPathsMultiRun.java**: Multi-run orchestration
- **SimPathsCollector.java**: Output collection and aggregation
- **SimPathsObserver.java**: GUI updates and monitoring

#### `model/`
**Purpose**: Core simulation logic

- **SimPathsModel.java**: Main simulation manager
- **Person.java**: Individual-level processes and attributes
- **BenefitUnit.java**: Fiscal unit processes
- **Household.java**: Residential unit processes
- **decisions/**: Labour supply and consumption optimization
- **enums/**: Type-safe enumerations (Gender, Country, Dhe, etc.)
- **taxes/**: Tax-benefit donor matching system
- **lifetime_incomes/**: Lifetime income projection utilities

### `/input/`

**Critical input files**:

| File Pattern | Purpose |
|--------------|---------|
| `align_*.xlsx` and `alignment_targets_*.xlsx` | Population, education, employment and partnership targets |
| `alignment_adjustment_series.xlsx` | Time-varying alignment adjustments |
| `economic_time_series.xlsx` | Price and wage indices, returns, debt costs and care wages |
| `reg_*.xlsx` | Regression parameters for behavioral processes |
| `scenario_*.xlsx` | Policy scenarios and projections |
| `projections_*.xlsx` | Demographic projections (mortality, fertility) |
| `DatabaseCountryYear.xlsx` | Tracks current database country/year |
| `EUROMODpolicySchedule.xlsx` | Tax-benefit policy schedule |
| `social_care_parameters.xlsx` | Detailed social care parameters |

**Subdirectories**:

- `InitialPopulations/`: Starting population databases
- `EUROMODoutput/`: Tax-benefit donor population data
- `DoFilesTarget/`: Stata-generated alignment targets

### `/config/`

YAML configuration files override default parameters. The main file is **default.yml**, which contains several configuration sections:

- **model_args**: SimPathsModel parameters (alignment switches, behavioral responses)
- **collector_args**: Output options (CSV, database, statistics)
- **parameter_args**: Data directories and input years
- **innovation_args**: Experimental parameters for sensitivity analysis

Additional configuration files for testing: **test_create_database.yml**, **test_run.yml**

---

## Sub-package Detail

The following sub-packages are self-contained subsystems whose internals are not obvious from the class names alone.

### `model/decisions/` — IO engine

When IO is enabled, computing optimal consumption–labour choices for every agent at every time step would be prohibitively slow. This package solves the problem once before the simulation runs: it constructs a grid covering all meaningful combinations of state variables (wealth, age, health, family status, etc.), then works backwards from the end of life to find the optimal choice at each grid point (backward induction). During the simulation, agents simply look up their current state in the pre-computed grid.

| Class | Purpose |
| --- | --- |
| `DecisionParams` | Defines the state-space dimensions and grid parameters for the optimisation problem. |
| `ManagerPopulateGrids` | Populates the state-space grid points and evaluates value functions by backward induction. |
| `ManagerSolveGrids` | Solves for optimal policy at each grid point. |
| `ManagerFileGrids` | Reads and writes pre-computed grids to disk, so they can be reused across runs. |
| `Grids` | Container for the set of solved decision grids. |
| `States` | Enumerates the state variables that define each grid point. |
| `Expectations` / `LocalExpectations` | Computes expected future values over stochastic transitions. |
| `CESUtility` | CES utility function used in the optimisation. |

### `model/taxes/` — EUROMOD donor matching

Imputes taxes and benefits onto simulated benefit units by matching them to pre-computed EUROMOD donor records.

| Class | Purpose |
| --- | --- |
| `DonorTaxImputation` | Main entry point. Implements the three-step matching process: coarse-exact matching on characteristics, income proximity filtering, and candidate selection/averaging. |
| `KeyFunction` / `KeyFunction1`–`4` | Four progressively relaxed matching-key definitions. The system tries the tightest key first and falls back through wider keys if no donors are found. |
| `DonorKeys` | Builds composite matching keys from benefit-unit characteristics. |
| `DonorTaxUnit` / `DonorPerson` | Represent the pre-computed EUROMOD donor records loaded from the database. |
| `CandidateList` | Ranked list of donor matches for a given benefit unit, sorted by income proximity. |
| `Match` / `Matches` | Store the final selected donor(s) and their imputed tax-benefit values. |

The `taxes/database/` sub-package handles loading donor data from the H2 database into memory (`TaxDonorDataParser`, `DatabaseExtension`, `MatchIndices`).

### `model/lifetime_incomes/` — synthetic income trajectories

When IO is enabled, this package creates projected income paths for birth cohorts using an AR(2) process anchored to age-gender geometric means, and matches simulated persons to donor income profiles.

| Class | Purpose |
| --- | --- |
| `ManagerProjectLifetimeIncomes` | Generates the synthetic income trajectory database for all birth cohorts in the simulation horizon. |
| `LifetimeIncomeImputation` | Matches each simulated person to a donor income trajectory via binary search on the income CDF. |
| `AnnualIncome` | Implements the AR(2) income process with age-gender anchoring. |
| `BirthCohort` | Groups individuals by birth year for cohort-level income projection. |
| `Individual` | Entity carrying age dummies and log GDP per capita for income regression. |

Collector CSV outputs are written under the run's output directory. Inspect that directory and its generated output README for the actual filenames and columns; do not infer the run identifier from a filename suffix. See [Statistical display](../overview/modules/statistical-display.md) for output selection.

For a description of the variables in output CSV files, see the [SimPaths Variable Codebook workbook on GitHub](https://github.com/simpaths/SimPaths/blob/develop/documentation/SimPaths_Variable_Codebook.xlsx). For the input-file families and links to the canonical inventory and loader, see the [Model Parameterisation page](../overview/parameterisation.md).

---

## Data Pipeline Reference

This section explains how the simulation-ready input files in `input/` are generated from raw survey data, and what to do if you need to update or extend them.

The pipeline has three main parts: initial populations, regression coefficients and alignment targets. They can be rerun separately only when their required inputs and preprocessing outputs are available and compatible.

### Data sources

| Source | Description | Access |
|--------|-------------|--------|
| **UKHLS** (Understanding Society) | Main household panel survey; waves 1 to O (UKDA-6614-stata) | Requires EUL licence from UK Data Service |
| **BHPS** (British Household Panel Survey) | Historical predecessor to UKHLS; used for pre-2009 employment history | Bundled with UKHLS EUL |
| **WAS** (Wealth and Assets Survey) | Biennial survey of household wealth; waves 1 to 7 (UKDA-7215-stata) | Requires EUL licence from UK Data Service |
| **EUROMOD / UKMOD** | Tax-benefit microsimulation system | See [Tax-Benefit Donors (UK)](../getting-started/data/tax-benefit-donors-uk.md) on the website |

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

### Part 2: Regression coefficients (`input/InitialPopulations/compile/RegressionEstimates/`)

The master script is `00_master_regression_estimates.do`. It defines the estimation samples and output paths, loads process conditions, and calls the numbered scripts. Review and adapt its local paths before use.

The master notes that wage predictions are needed by later estimation work, but its listed calls are not a complete dependency guarantee. In particular, `09_reg_income.do` reads `estimation_sample2`, which includes predicted wages. Confirm that this file has been produced by the compatible wage-estimation step before running income estimation.

| Script | Outcomes and methods |
| --- | --- |
| `01_reg_education.do` | Probit models for remaining in and returning to education; generalised ordered logit for attainment |
| `02_reg_leave_parental_home.do` | Leaving the parental home, probit |
| `03_reg_partnership.do` | Partnership formation and dissolution, probit |
| `04_reg_fertility.do` | Fertility, probit |
| `05_reg_health.do` | Five-category self-rated health, generalised ordered logit; long-term sickness/disability, probit |
| `06_reg_home_ownership.do` | Home ownership, probit |
| `07_reg_retirement.do` | Retirement, probit |
| `08_reg_wages.do` | Wage equations with employment selection |
| `09_reg_income.do` | Non-labour income receipt and amounts |
| `10_reg_socialcare.do` | Care need, receipt, source, hours and provision, with process-specific regression types |
| `11_reg_financial_distress.do` | Financial distress, logit |
| `12_reg_health_mental.do` | GHQ-based wellbeing and distress: linear level model, ordered-logit baseline caseness model, and subsequent adjustments |
| `13_reg_health_wellbeing.do` | SF-12 MCS, SF-12 PCS and life satisfaction, with linear baseline models and subsequent adjustments |

`00_master_conditions.do` defines estimation conditions. `variable_update.do` prepares regression variables, and `programs.do` supplies export helpers. The master lists the required Stata packages. Its final three estimation scripts are marked as not yet refactored, so do not infer identical naming conventions throughout the pipeline.

Results are written to the configured `dir_results` and `dir_raw_results` locations. They do not automatically replace every model input workbook. Review the outputs, worksheet names, regressor mappings and validation results before promoting compatible files into a simulation's input directory.

The [Model Parameterisation page](../overview/parameterisation.md) links the canonical workbook inventory and loader. Estimation scripts and input workbooks are different inventories; a single script may update multiple files.

### Part 3: Alignment targets (`input/DoFilesTarget/`)

| Script | Main output |
| --- | --- |
| `01_employment_shares_initpopdata.do` | `alignment_targets_employment.xlsx` |
| `02_inSchool_targets_initpopdata.do` | `alignment_targets_inSchool.xlsx` |
| `03a_calculate_partneredShare_initialPop_BUlogic.do` | Initial-population partnership diagnostics and `alignment_targets_partnered_share.xlsx` |
| `03b_calculate_partnership_target.do` | Partnership targets in `alignment_targets_partnered_share.xlsx` |
| `90_person_risk_employment_stats.do` | Person-level employment-risk diagnostics |

Each script has its own configured input/output paths. Check the resulting workbook names and worksheets against the Java loader before use. Demographic projection workbooks are separate inputs; these scripts do not regenerate the entire set of population, fertility and mortality projections.

### When to rebuild inputs

Recompile populations when the source data, derived variables or required start year change. Re-estimate affected equations when their specification or estimation data change, including any prerequisite predicted variables. Regenerate targets when their data or subgroup definitions change.

Rebuild the relevant database after changing the imported population or donor records. Coefficient and target workbooks are loaded separately: changing only these files generally calls for a fresh simulation and validation, not an automatic rebuild of every population database.

### Setup artifacts

Setup prepares the population and donor tables used by the selected run. `input.mv.db` contains initial-population and donor tables; it is not solely a donor database. `DatabaseCountryYear.xlsx` records the selected country/year, not macroeconomic parameters. The policy schedule maps years to tax-benefit systems.

The setup route matters: single-run startup can rebuild a policy schedule, whereas multi-run database setup should not be assumed to regenerate a custom one. Check [Single Runs](../user-guide/single-runs.md) and [Modifying Tax-Benefit Settings](../user-guide/tax-benefit-parameters.md) before replacing existing inputs.

### Training mode

The repository includes de-identified training data under `input/InitialPopulations/training/` and `input/EUROMODoutput/training/`. If no initial-population CSV files are found in the main input location, SimPaths automatically switches to training mode. Training mode supports development and CI but is not intended for research interpretation.

### Logging

With `-f` on `multirun.jar`, logs are written to `output/logs/run_<seed>.txt` (stdout) and `output/logs/run_<seed>.log` (log4j).

---

## Development Workflow

### Understand the affected process

1. Start with `SimPathsStart` or `SimPathsMultiRun` for configuration and initialisation.
2. Find the process in `SimPathsModel.buildSchedule()`.
3. Follow its event dispatch to `Person`, `BenefitUnit` or the relevant manager.
4. Check the variable codebook, schedule workbook and Stata parameter mapping before changing fields, timing or shared constants.

### Trace regressions

Follow the process's `RegressionName` through `ManagerRegressions` to the regression object in `Parameters`. Then locate the workbook and worksheet loaded for that object. Do not infer the workbook name directly from the enum name.

For example, the health implementation obtains the category probabilities with:

```java
Map<Dhe, Double> probs =
    ManagerRegressions.getProbabilities(this, RegressionName.HealthH1);
```

This is an excerpt from `Person.health()`, not a standalone program. Outcome sampling and state assignment follow it in the source.

### Change fields, settings and outputs

Use [Introduce a New Variable](how-to/new-variable.md) for persistent fields, input mapping, lags and regressors. Use [Add Parameters to the GUI](how-to/add-gui-parameters.md) for interactive controls, and [Multiple Runs](../user-guide/multiple-runs.md) for batch configuration.

`PERSON_VARIABLES_INITIAL` lists imported person columns, not all runtime fields. A collector summary and an exported entity field are also different kinds of output changes. Check the actual export path before assuming that adding a field is sufficient.

### Test and review

```bash
mvn test
mvn verify
```

The first command runs unit tests; the second also runs integration tests. Follow with a relevant training-data run and the corresponding [validation](../validation/index.md). Record the seed, configuration, input and parameter versions used.

[Working in GitHub](working-in-github.md) covers branches, reviewed commits and pull requests.

## Code Navigation Tips

- Event order: `SimPathsModel.buildSchedule()`, then the relevant `onEvent()`.
- Regression inputs: `RegressionName`, `ManagerRegressions`, the loader in `Parameters`, then the agent's regressor mapping.
- Alignment: the relevant model alignment method and target loader; not every SimPaths alignment is a direct call to the same JAS-mine algorithm.
- Outputs: `SimPathsCollector` settings, statistic classes and exported entity mappings.

## Additional Resources

- **Full Documentation**: See `documentation/wiki/` for comprehensive guides
- **Issues**: [GitHub Issues](https://github.com/simpaths/SimPaths/issues)
