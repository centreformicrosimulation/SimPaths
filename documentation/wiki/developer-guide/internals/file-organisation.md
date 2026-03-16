# File Organisation

This page describes the directory and package layout of the SimPaths repository. For the generic JAS-mine project structure, see [Project Structure](../jasmine/project-structure.md).

## 1. Top-level directories

| Directory | Contents |
| --- | --- |
| `config/` | YAML configuration files for batch runs (`default.yml`, `test_create_database.yml`, `test_run.yml`) |
| `input/` | Survey-derived input data, EUROMOD donor files, Stata scripts for data preparation |
| `output/` | Simulation output (created at runtime; each run produces a timestamped subfolder) |
| `src/` | All Java source code (main and test) plus resources |
| `target/` | Maven build output: compiled classes and runnable JARs (`singlerun.jar`, `multirun.jar`) |
| `validation/` | Stata scripts and reference graphs for two-stage model validation |
| `documentation/` | Markdown documentation and wiki source files |
| `.github/workflows/` | CI pipeline (`SimPathsBuild.yml`) and Javadoc publishing (`publish-javadoc.yml`) |

Root-level files include `pom.xml` (Maven project definition) and `README.md`.

## 2. Source code — `src/main/java/simpaths/`

### `experiment/`

Entry points and orchestration. Contains the four manager classes required by the JAS-mine architecture:

| Class | Role |
| --- | --- |
| `SimPathsStart` | Entry point for interactive single runs. Builds the GUI dialog, creates database tables from CSV, and launches the simulation. |
| `SimPathsMultiRun` | Entry point for batch runs. Reads a YAML config file, iterates over runs with optional parameter variation (innovation shocks), and manages run labelling. |
| `SimPathsCollector` | Collector manager. Computes aggregate statistics each simulated year and exports them to output CSV files (Statistics, Statistics2, Statistics3). |
| `SimPathsObserver` | Observer manager. Builds real-time GUI charts for monitoring the simulation while it runs. |

### `model/`

Core simulation logic. The central class is `SimPathsModel`, which owns all agent collections, builds the yearly event schedule (44 ordered processes), and coordinates the annual simulation cycle.

Agent classes:

| Class | Description |
| --- | --- |
| `Person` | Individual agent. Carries all demographics, health, education, labour, income, and social care state. Contains the per-person process methods invoked by the schedule. |
| `BenefitUnit` | Tax-and-benefit assessment unit: one or two adults plus their dependents. Tax-benefit evaluation is performed at this level. |
| `Household` | Grouping of benefit units sharing the same address. |

Other key classes in `model/`:

| Class | Purpose |
| --- | --- |
| `SimPathsModel` | Model manager. Initialises the population, registers all 44 yearly processes with the JAS-mine scheduler, manages alignment and aggregate state. |
| `TaxEvaluation` | Orchestrates EUROMOD donor matching to impute taxes and benefits onto simulated benefit units. |
| `UnionMatching` | Partnership formation algorithm. Matches unpartnered individuals into couples based on characteristics and preferences. |
| `LabourMarket` | Labour market clearing: matches labour supply decisions to employment outcomes. |
| `Innovations` | Applies parameter shocks (innovation perturbations) across sequential runs for sensitivity analysis. |
| `Validator` | Runtime consistency checks on the simulated population. |
| `*Alignment` classes | `FertilityAlignment`, `ActivityAlignmentV2`, `InSchoolAlignment`, `PartnershipAlignment`, `SocialCareAlignment` — each aligns a specific outcome to external calibration targets. |

### `model/enums/`

46 enumeration classes defining the categorical variables used throughout the simulation: `Gender`, `Education`, `Labour`, `HealthStatus`, `Country`, `Region`, `Ethnicity`, `Occupancy`, and others. These are referenced by the ORM for database persistence and by regression models for covariate encoding.

### `model/decisions/`

Intertemporal optimisation (IO) computational engine. When IO is enabled, computing optimal consumption–labour choices for every agent at every time step during the simulation would be prohibitively slow. This package solves the problem once before the simulation runs: it constructs a grid covering all meaningful combinations of state variables (wealth, age, health, family status, etc.), then works backwards from the end of life to find the optimal choice at each grid point (backward induction). During the simulation, agents simply look up their current state in the pre-computed grid rather than solving an optimisation problem.

Key classes:

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

### `model/taxes/`

EUROMOD donor-matching subsystem. Imputes taxes and benefits onto simulated benefit units by matching them to pre-computed EUROMOD donor records.

| Class | Purpose |
| --- | --- |
| `DonorTaxImputation` | Main entry point. Implements the three-step matching process: coarse-exact matching on characteristics, income proximity filtering, and candidate selection/averaging. |
| `KeyFunction` / `KeyFunction1`–`4` | Four progressively relaxed matching-key definitions. The system tries the tightest key first and falls back through wider keys if no donors are found. |
| `DonorKeys` | Builds composite matching keys from benefit-unit characteristics. |
| `DonorTaxUnit` / `DonorPerson` | Represent the pre-computed EUROMOD donor records loaded from the database. |
| `CandidateList` | Ranked list of donor matches for a given benefit unit, sorted by income proximity. |
| `Match` / `Matches` | Store the final selected donor(s) and their imputed tax-benefit values. |

The `taxes/database/` sub-package handles loading donor data from the H2 database into memory (`TaxDonorDataParser`, `DatabaseExtension`, `MatchIndices`).

### `model/lifetime_incomes/`

Synthetic lifetime income trajectory generator. When IO is enabled, this package creates projected income paths for birth cohorts using an AR(2) process anchored to age-gender geometric means, and matches simulated persons to donor income profiles.

| Class | Purpose |
| --- | --- |
| `ManagerProjectLifetimeIncomes` | Generates the synthetic income trajectory database for all birth cohorts in the simulation horizon. |
| `LifetimeIncomeImputation` | Matches each simulated person to a donor income trajectory via binary search on the income CDF. |
| `AnnualIncome` | Implements the AR(2) income process with age-gender anchoring. |
| `BirthCohort` | Groups individuals by birth year for cohort-level income projection. |
| `Individual` | Entity carrying age dummies and log GDP per capita for income regression. |

### `data/`

Parameters, input parsing, regression management, and utility classes.

| Class | Purpose |
| --- | --- |
| `Parameters` | Central parameter store. Loads all regression coefficients, alignment targets, projections, and scenario tables from Excel files at simulation start. |
| `ManagerRegressions` | Manages the regression coefficient files (`reg_*.xlsx`) and provides methods for evaluating regression equations. |
| `RegressionName` | Enum-like catalogue of all named regression models used in the simulation. |
| `ScenarioTable` | Reads scenario-specific parameter overrides from Excel files. |
| `MahalanobisDistance` | Mahalanobis distance computation, used in donor matching. |
| `RootSearch` / `RootSearch2` | Numerical root-finding routines for alignment. |

Sub-packages:

- **`data/filters/`** — 42 cross-section filter classes (e.g. `FemaleAgeGroupCSfilter`, `RegionEducationWorkingCSfilter`). Each defines a predicate for selecting subsets of agents by demographic characteristics, used in alignment and statistics collection.
- **`data/startingpop/`** — `DataParser` reads the initial population CSV files and constructs the starting agent objects; `Processed` tracks which records have been loaded.
- **`data/statistics/`** — `Statistics`, `Statistics2`, `Statistics3` define the output entities whose fields are exported to CSVs by the Collector. `EmploymentStatistics` and `HealthStatistics` compute domain-specific aggregate indicators.

## 3. Test code — `src/test/java/simpaths/`

Test packages mirror the main source structure:

| Package | Contents |
| --- | --- |
| `simpaths/model/` | Unit tests for agent classes and simulation logic |
| `simpaths/data/` | Tests for parameter loading and data utilities |
| `simpaths/data/filters/` | Tests for cross-section filters |
| `simpaths/data/statistics/` | Tests for statistics computation |
| `simpaths/experiment/` | Tests for entry points and configuration parsing |
| `simpaths/integrationtest/` | `RunSimPathsIntegrationTest` — end-to-end test that builds the database and runs a short simulation. The `expected/` subfolder contains reference output for comparison. |
| `simpaths/testinput/` | Test fixture data files |

## 4. Resources — `src/main/resources/`

| File | Purpose |
| --- | --- |
| `hibernate.cfg.xml` | Hibernate ORM configuration for the embedded H2 database |
| `log4j.properties` | Logging configuration |
| `META-INF/` | Persistence unit definitions |
| `images/` | Icons and images used by the GUI |

## 5. Input data — `input/`

| Subdirectory | Contents |
| --- | --- |
| `InitialPopulations/training/` | De-identified synthetic population CSV that can be distributed with the repo. Generated by `99_training_data.do`: household IDs anonymised, all continuous variables perturbed with 15% random noise, survey weights randomised. Structurally identical to the real data but contains no traceable individual records, so it is not subject to data licence restrictions. Used for CI testing and getting started without access to the full survey data. |
| `InitialPopulations/compile/` | 13 Stata do-files that build the full initial population from UKHLS/BHPS/WAS survey data |
| `InitialPopulations/compile/do_emphist/` | 8 Stata scripts that reconstruct monthly employment histories back to 2007 |
| `InitialPopulations/compile/RegressionEstimates/` | 14 Stata scripts that estimate regression coefficients and produce the `reg_*.xlsx` files |
| `DoFilesTarget/` | 5 Stata scripts that generate alignment target files (employment shares, education targets, partnership rates) |
| `EUROMODoutput/` | Pre-computed EUROMOD tax-benefit donor files, one per policy year. These are loaded into the H2 database during the setup phase. |

The full input data (survey microdata and EUROMOD output) is not committed to the repository due to data licence restrictions. The `training/` subfolder contains a small synthetic subset for CI and testing.

## 6. Validation — `validation/`

| Subdirectory | Contents |
| --- | --- |
| `01_estimate_validation/do_files/` | 9 Stata scripts that compare predicted versus observed values for each regression module |
| `01_estimate_validation/graphs/` | Output graphs from estimate validation |
| `02_simulated_output_validation/do_files/` | 28 Stata scripts that compare simulation output against UKHLS observed data across 18 outcomes |
| `02_simulated_output_validation/graphs/` | Reference comparison plots from a baseline validation run |

## 7. Configuration — `config/`

| File | Purpose |
| --- | --- |
| `default.yml` | Default configuration for batch runs. Fully annotated with inline comments. |
| `test_create_database.yml` | Rebuilds the H2 database from input CSVs (used during setup). |
| `test_run.yml` | Minimal configuration for CI testing. |

Each YAML file is standalone — there is no inheritance between config files. Keys map directly to fields in `SimPathsMultiRun` and `SimPathsModel`. See the [Configuration](../../../../documentation/configuration.md) reference for a complete listing of all keys.
