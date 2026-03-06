# Configuration

SimPaths multi-run behavior is controlled by YAML files in `config/`.

Examples in this repository include:

- `default.yml`
- `test_create_database.yml`
- `test_run.yml`
- `create database.yml`
- `sc analysis*.yml`
- `intertemporal elasticity.yml`
- `labour supply elasticity.yml`

For command-by-command guidance for each provided config, see [Scenario Cookbook](scenario-cookbook.md).

## How config is applied

`SimPathsMultiRun` loads `config/<file>` and applies values in two stages:

1. YAML values initialize runtime fields and argument maps.
2. CLI flags override those values if provided.

## Top-level keys

### Core run arguments

Common fields:

- `countryString`
- `maxNumberOfRuns`
- `executeWithGui`
- `randomSeed`
- `startYear`
- `endYear`
- `popSize`
- `integrationTest`

### `model_args`

Passed into `SimPathsModel` via reflection.

Typical toggles include:

- alignment flags (`alignPopulation`, `alignFertility`, `alignEmployment`, ...)
- behavioral switches (`enableIntertemporalOptimisations`, `responsesToHealth`, ...)
- persistence of behavioral grids (`saveBehaviour`, `useSavedBehaviour`, `readGrid`)

### `collector_args`

Controls output collection and export behavior (via `SimPathsCollector`), including:

- `persistStatistics`, `persistStatistics2`, `persistStatistics3`
- `persistPersons`, `persistBenefitUnits`, `persistHouseholds`
- `exportToCSV`, `exportToDatabase`

### `innovation_args`

Controls iteration logic across runs, such as:

- `randomSeedInnov`
- `intertemporalElasticityInnov`
- `labourSupplyElasticityInnov`
- `flagDatabaseSetup`

### `parameter_args`

Overrides values from `Parameters` (paths and model-global flags).

Common examples:

- `trainingFlag`
- `working_directory`
- `input_directory`
- `input_directory_initial_populations`
- `euromod_output_directory`

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
  persistPersons: false
  persistBenefitUnits: false
  persistHouseholds: false
```

Run it:

```bash
java -jar multirun.jar -config test_run.yml
```
