# Scenario Cookbook

This guide maps every YAML config currently in `config/` to its intended use, and explains how to build your own.

All commands assume you are running from repository root after building jars.

---

## Provided configs

### `default.yml`

The standard baseline run with conservative defaults. Use this as your starting point for any new analysis.

```bash
java -jar multirun.jar -config default.yml -g false
```

### `test_create_database.yml`

Test-oriented database setup using training data (`trainingFlag: true`). Creates the H2 donor database needed before running simulations.

```bash
java -jar multirun.jar -DBSetup -config test_create_database.yml
```

### `test_run.yml`

Short integration-style run (2 runs, test settings, training data). Used by CI and useful for reproducing CI behavior locally.

```bash
java -jar multirun.jar -config test_run.yml -P root
```

---

## Building your own config

Place a new `.yml` file in `config/` and pass it via `-config`. You only need to specify the values you want to override — everything else inherits defaults from `default.yml` or class field defaults.

The keys under `model_args` map directly to the `@GUIparameter`-annotated fields on `SimPathsModel` — so anything you can set in the GUI can also be set here.

### Minimal template

```yaml
maxNumberOfRuns: 5
executeWithGui: false
randomSeed: 42
startYear: 2019
endYear: 2030
countryString: UK
popSize: 20000

collector_args:
  persistStatistics: true
  persistStatistics2: true
  persistStatistics3: true
  persistPersons: false
  persistBenefitUnits: false
  persistHouseholds: false
```

### Enabling alignment

To align simulated aggregates to external targets, add `model_args` with the relevant flags:

```yaml
model_args:
  alignPopulation: true
  alignCohabitation: true
  alignFertility: true
  alignInSchool: true
  alignEducation: true
```

See [Configuration](configuration.md) for a full list of `model_args` toggles, and [Model Concepts](model-concepts.md) for what each alignment dimension does.

### Running sensitivity analyses

To vary a parameter across runs, use `innovation_args`. For example, to sweep the intertemporal interest-rate innovation:

```yaml
maxNumberOfRuns: 3
model_args:
  enableIntertemporalOptimisations: true

innovation_args:
  intertemporalElasticityInnov: true
```

### Saving and reusing a behavioural grid

If you have computed a decision grid for a baseline scenario and want to reuse it in a counterfactual:

```yaml
# Baseline run — saves the grid
model_args:
  enableIntertemporalOptimisations: true
  saveBehaviour: true
  # readGrid is set to the run name automatically

# Counterfactual run — loads the saved grid
model_args:
  enableIntertemporalOptimisations: true
  useSavedBehaviour: true
  readGrid: "my_baseline_run"
```

---

## Practical notes

- Use quotes around config filenames that contain spaces: `-config "my config.yml"`.
- Add `-f` to write run logs to `output/logs/`.
- Override individual values at runtime without editing the YAML, for example `-n 10` overrides `maxNumberOfRuns`.
- Add `-P none` when you do not need the processed dataset to persist between runs (faster).
