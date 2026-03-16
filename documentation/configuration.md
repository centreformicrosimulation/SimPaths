# Configuration

SimPaths batch runs are controlled by YAML files in `config/`. The main config is `default.yml`, which is fully annotated with inline comments.

---

## Quick run

After building, three commands are all you need:

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

## How config is applied

`SimPathsMultiRun` loads `config/<file>` and applies values in two stages:

1. YAML values initialise runtime fields and argument maps.
2. CLI flags override those values if provided.

If a key is not specified in the YAML, the Java class field default is used. Each config file is standalone — there is no inheritance between config files.

---

## Writing your own config

Place a new `.yml` file in `config/` and pass it via `-config`. You only need to specify the values you want to change — everything else falls back to the Java class field defaults.

### Core run arguments

| Key | Default | Description |
|-----|---------|-------------|
| `maxNumberOfRuns` | `1` | Number of sequential simulation runs |
| `executeWithGui` | `false` | `true` launches the JAS-mine GUI; `false` = headless (required on servers/CI) |
| `randomSeed` | `606` | RNG seed for the first run |
| `startYear` | `2019` | First simulation year (must have matching input/donor data) |
| `endYear` | `2022` | Last simulation year (inclusive) |
| `popSize` | `50000` | Simulated population size; larger = more accurate but slower |

### Collector arguments

The `collector_args` section controls what output files are produced:

| Flag | Default | Description |
|------|---------|-------------|
| `persistStatistics` | `true` | Write `Statistics1.csv` — income distribution, Gini, S-Index |
| `persistStatistics2` | `true` | Write `Statistics2.csv` — demographic validation by age and gender |
| `persistStatistics3` | `true` | Write `Statistics3.csv` — alignment diagnostics |
| `exportToCSV` | `true` | Write outputs to CSV files under `output/<run>/csv/` |

For a description of the variables in these files, see `documentation/SimPaths_Variable_Codebook.xlsx`.

### Minimal example

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

---

## Additional arguments

The YAML file supports several other argument sections (`model_args`, `innovation_args`, `parameter_args`) that control alignment flags, intertemporal optimisation settings, sensitivity analysis parameters, and file paths. Many of these are for specific analyses and some are under active review. The annotated `default.yml` file documents all available keys with inline comments.

Note that some settings — particularly alignment — are primarily controlled in `SimPathsModel.java` rather than through the YAML file.

---

## Practical notes

- Use quotes around config filenames that contain spaces: `-config "my config.yml"`.
- Add `-f` to write run logs to `output/logs/`.
- Override individual values at runtime without editing the YAML, for example `-n 10` overrides `maxNumberOfRuns`.
