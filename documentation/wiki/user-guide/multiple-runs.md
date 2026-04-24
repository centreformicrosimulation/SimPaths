# Multiple Runs

`multirun.jar` runs a sequence of simulations using `simpaths.experiment.SimPathsMultiRun`. It is the main route for repeated runs, sensitivity work, and batch execution on a server or in CI.

## 1. Before you start

Make sure the project builds and the input database has been created at least once:

```bash
mvn clean package
java -jar multirun.jar -DBSetup 
```

The setup step rebuilds `input/input.mv.db` from the initial-population files and the EUROMOD / UKMOD donor outputs. If you have already completed setup and none of those inputs have changed, you do not need to repeat it before every run.

## 2. Basic command

For a standard headless batch run:

```bash
java -Xmx8g -jar multirun.jar
```

The configuration file is loaded first, and any command-line flags then override it.

## 3. What the configuration file controls

The main configuration file is `config/default.yml`. The most useful top-level settings for multi-run work are:

- `maxNumberOfRuns`: number of sequential runs to perform
- `randomSeed`: seed used for the first run
- `startYear` and `endYear`: simulation horizon
- `popSize`: simulated population size
- `executeWithGui`: whether to launch the JAS-mine multi-run frame

The same file also exposes:

- `model_args`: settings passed into `SimPathsModel`
- `innovation_args`: rules for varying parameters across runs
- `collector_args`: output and persistence settings
- `parameter_args`: input-directory and donor-data settings

## 4. Useful command-line options

The most important command-line flags are:

- `-config <file>`: use a different YAML file under `config/`
- `-n <int>`: override the number of runs
- `-r <seed>`: override the first random seed
- `-s <year>` and `-e <year>`: override the run horizon
- `-p <int>`: override population size
- `-g true|false`: run with or without the GUI
- `-DBSetup`: rebuild the input database and exit without running simulations
- `-f`: write console output and log output to `output/logs/`
- `--persist=root|run|none`: control where processed population data are reused

## 5. Persistence options

`SimPathsMultiRun` supports three persistence modes:

- `root`: reuse processed population data from the root `input/` folder
- `run`: keep processed population data in the run-specific output folder
- `none`: do not persist processed population data between runs

`root` is the default and is usually the right choice for repeated runs over the same starting database. Use `run` when you want each batch to remain self-contained. Use `none` when you want to avoid writing processed population data at all.

## 6. Innovations across runs

Repeated runs do not have to be identical. In `innovation_args` you can enable changes between run 1, run 2, run 3, and so on.

The main options are:

- `randomSeedInnov`: increment the seed for each successive run
- `intertemporalElasticityInnov`: apply interest-rate innovations across runs
- `labourSupplyElasticityInnov`: apply disposable-income innovations across runs

These are intended for uncertainty and sensitivity work. If you only want repeated stochastic runs, leaving `randomSeedInnov: true` is normally enough.

## 7. Output collection

Output writing is controlled through `collector_args` in `default.yml`.

The main switches are:

- `exportToCSV`: write CSV outputs
- `exportToDatabase`: write outputs to the H2 database
- `persistStatistics`, `persistStatistics2`, `persistStatistics3`: write the main summary outputs
- `persistPersons`, `persistBenefitUnits`, `persistHouseholds`: write record-level panels

Record-level outputs can become very large, so they should only be enabled when needed.

## 8. A practical example

This example runs three headless simulations from 2019 to 2022, starting from seed `606`, and keeps processed population data in the root folder:

```bash
java -jar multirun.jar \
  -n 3 \
  -r 606 \
  -s 2019 \
  -e 2022 \
  -g false \
  --persist=root
```

## 9. When to rebuild the database

Run `-DBSetup` again before your batch if you have changed any of the following:

- files in `input/InitialPopulations/`
- files in `input/EUROMODoutput/`
- `input/EUROMODpolicySchedule.xlsx`
- input paths in `parameter_args`

If those inputs have not changed, a normal multi-run can start directly from the existing database.

## 10. Related pages

- [Environment Setup](../getting-started/environment-setup.md)
- [Single Runs](single-runs.md)
- [Uncertainty Analysis](uncertainty-analysis.md)
