# Configuration

SimPaths multi-run behavior is controlled by YAML files in `config/`.

This repository ships with three configs:

- `default.yml` — standard baseline run (well-commented reference for all fields)
- `test_create_database.yml` — database setup using training data
- `test_run.yml` — short integration-test run

For command-by-command guidance and a template for building your own config, see [Run Configuration](run-configuration.md).

## How config is applied

`SimPathsMultiRun` loads `config/<file>` and applies values in two stages:

1. YAML values initialize runtime fields and argument maps.
2. CLI flags override those values if provided.

## Top-level keys

### Core run arguments

| Key | Default | Description |
|-----|---------|-------------|
| `maxNumberOfRuns` | `1` | Number of sequential simulation runs |
| `executeWithGui` | `false` | `true` launches the JAS-mine GUI; `false` = headless (required on servers/CI) |
| `randomSeed` | `606` | RNG seed for the first run; auto-incremented when `randomSeedInnov` is true |
| `startYear` | `2019` | First simulation year (must have matching input/donor data) |
| `endYear` | `2022` | Last simulation year (inclusive) |
| `popSize` | `50000` | Simulated population size; larger = more accurate but slower |
| `countryString` | auto | `"United Kingdom"` or `"Italy"`; auto-detected from donor DB if omitted |
| `integrationTest` | `false` | Writes output to a fixed folder for CI comparison |

---

### `model_args`

Keys map directly to `@GUIparameter`-annotated fields on `SimPathsModel`. Anything settable in the GUI can also be set here.

#### Alignment flags

Alignment prevents aggregate rates from drifting from known targets. Each dimension is independently controlled:

| Flag | Default | What it aligns |
|------|---------|----------------|
| `alignPopulation` | `true` | Age-sex-region totals to demographic projections |
| `alignCohabitation` | `true` | Share of individuals in partnerships |
| `alignFertility` | `false` | Birth rates to projected fertility rates |
| `alignInSchool` | `false` | School participation rate (age 16–29) |
| `alignEducation` | `false` | Completed education level distribution |
| `alignEmployment` | `false` | Employment share |

See [Model Concepts — Alignment](model-concepts.md#alignment) for a fuller explanation.

#### Income security (S-Index)

The S-Index is an economic (in)security measure computed each year per person and reported in `Statistics1.csv` as `SIndex_p50`. It takes a rolling window of equivalised consumption observations, applies exponential discounting, and weights losses more heavily than gains according to a risk-aversion parameter.

| Parameter | Default | Meaning |
|-----------|---------|---------|
| `sIndexTimeWindow` | `5` | Length of rolling window in years |
| `sIndexAlpha` | `2` | Coefficient of relative risk aversion — higher values make the index more sensitive to consumption drops |
| `sIndexDelta` | `0.98` | Annual discount factor applied to past consumption observations |

#### Intertemporal optimisation (IO)

Enables a backward-induction life-cycle solution for consumption and labour supply. Decision grids are pre-computed in year 0; agents look up their optimal choice each year. Computationally intensive — disabled by default.

The IO state-space flags control which personal characteristics enter the grid (each adds a dimension and increases solve time):

| Flag | Default |
|------|---------|
| `responsesToHealth` | `true` |
| `responsesToDisability` | `false` |
| `responsesToEducation` | `true` |
| `responsesToPension` | `false` |
| `responsesToRetirement` | `false` |
| `responsesToLowWageOffer` | `true` |
| `responsesToRegion` | `false` |

Grid persistence flags allow a baseline grid to be solved once and reused in counterfactual runs (`saveBehaviour: true` / `useSavedBehaviour: true` with `readGrid: "<run-name>"`). See [Run Configuration](run-configuration.md) for an example.

---

### `innovation_args`

Controls how parameters change across sequential runs (run 0, run 1, run 2, …). Useful for sensitivity analysis and uncertainty quantification.

| Flag | Default | Behavior |
|------|---------|----------|
| `randomSeedInnov` | `true` | Increments `randomSeed` by 1 for each successive run so each gets a distinct seed |
| `flagDatabaseSetup` | `false` | If `true`, runs database setup instead of simulation (equivalent to `-DBSetup` on the CLI) |
| `intertemporalElasticityInnov` | `false` | If `true`, applies interest rate shocks: run 1 = +0.0075 (higher return to saving), run 2 = −0.0075 (lower return to saving). Requires `maxNumberOfRuns >= 3` to see all variants. |
| `labourSupplyElasticityInnov` | `false` | If `true`, applies disposable income shocks: run 1 = +0.01 (higher net labour income), run 2 = −0.01 (lower net labour income). Requires `maxNumberOfRuns >= 3`. |

---

### `collector_args`

Controls what `SimPathsCollector` writes to CSV or database each simulation year.

#### Output files

| File | Content | Enabled by |
|------|---------|-----------|
| `Statistics1.csv` | Income distribution: Gini coefficients, income percentiles, median equivalised disposable income (EDI), S-Index | `persistStatistics: true` |
| `Statistics2.csv` | Demographic validation: partnership rates, employment rates, health and disability measures by age and gender | `persistStatistics2: true` |
| `Statistics3.csv` | Alignment diagnostics: simulated vs target rates and the adjustment factors applied | `persistStatistics3: true` |
| `EmploymentStatistics.csv` | Labour market transitions and participation rates | `persistEmploymentStatistics: true` |
| `HealthStatistics.csv` | Health measures (SF-12, GHQ-12, EQ-5D) by age and gender | *(written automatically when health statistics are computed)* |

For a description of the variables in these files, see `documentation/SimPaths_Variable_Codebook.xlsx`.

#### Other collector flags

| Flag | Default | Description |
|------|---------|-------------|
| `calculateGiniCoefficients` | `false` | Compute Gini coefficients (also populates GUI charts); off by default for speed |
| `exportToCSV` | `true` | Write outputs to CSV files under `output/<run>/csv/` |
| `exportToDatabase` | `false` | Write outputs to H2 database in addition to or instead of CSV |
| `persistPersons` | `false` | Write one row per person per year (produces large files) |
| `persistBenefitUnits` | `false` | Write one row per benefit unit per year (produces large files) |
| `persistHouseholds` | `false` | Write one row per household per year |
| `dataDumpStartTime` | `0` | First year to write output (`0` = `startYear`) |
| `dataDumpTimePeriod` | `1.0` | Output frequency in years (`1.0` = every year) |

---

### `parameter_args`

Overrides file paths and model-global flags in `Parameters`.

| Key | Default | Description |
|-----|---------|-------------|
| `input_directory` | `input` | Path to input data folder |
| `input_directory_initial_populations` | `input/InitialPopulations` | Path to initial population CSVs |
| `euromod_output_directory` | `input/EUROMODoutput` | Path to EUROMOD/UKMOD output files |
| `trainingFlag` | `false` | If `true`, loads training data from `input/.../training/` subfolders (set automatically by test configs) |
| `includeYears` | *(all)* | List of policy years for which EUROMOD donor data is available; only these years enter the donor database |

---

## Minimal example

```yaml
maxNumberOfRuns: 2
executeWithGui: false
randomSeed: 100
startYear: 2019
endYear: 2022
popSize: 20000

collector_args:
  persistStatistics: true
  persistStatistics2: true
  persistStatistics3: true
```

Run it:

```bash
java -jar multirun.jar -config my_run.yml
```
