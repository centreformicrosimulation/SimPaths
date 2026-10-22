# Single Runs

Use a single run to inspect one configured simulation, explore the graphical interface, or test a model change. For repeated runs and YAML configuration, use [Multiple Runs](multiple-runs.md).

## Before you start

Complete [Environment Setup](../getting-started/environment-setup.md) and [Running Your First Simulation](../getting-started/first-simulation.md). Run the commands below from the repository root, where `input/` and the executable JAR files are located.

The bundled training population uses 2019. It is suitable for learning and testing, not for substantive research. Research runs require authorised [input data](../getting-started/data/index.md) and a compatible start year.

## Run without the graphical interface

Build the executables if needed:

```bash
mvn clean package
```

For a first run with the bundled UK training data, prepare the inputs:

```bash
java -jar singlerun.jar -Setup -c UK -s 2019 -g false --rewrite-policy-schedule
```

The `--rewrite-policy-schedule` option rebuilds the policy schedule from the available donor files. Do not use it to overwrite a research scenario's custom schedule unintentionally.

The run-only path reads the saved country/year metadata. Its `-s` argument does not replace that saved year, so complete setup for the intended start year first.

Then run the simulation:

```bash
java -jar singlerun.jar -Run -c UK -s 2019 -g false
```

`-Setup` prepares inputs and exits; `-Run` runs without setup. They cannot be used together. With neither option, the entry point performs setup and then runs.

## Run interactively

```bash
java -jar singlerun.jar
```

Follow the [first GUI run](gui.md#2-first-gui-run-with-bundled-training-data) instructions to select the year and complete setup. Adjust the exposed model and collector settings before building the simulation, then start it. Population size and other initialisation settings require a rebuild to take effect.

## Choose the right configuration route

The single-run command accepts country, start year, setup/run mode, GUI mode and policy-schedule rewriting. It does not accept the multi-run `-config`, `-p` or `-e` options. For a reproducible run with explicit population size, end year and output settings, use [Multiple Runs](multiple-runs.md) with `maxNumberOfRuns: 1`.

Inspect the options supported by your executable:

```bash
java -jar singlerun.jar --help
```

## Check the result

Read the console for completion or exceptions, then inspect the newly created run directory under `output/`. The files present depend on the collector settings; see [Statistical display](../overview/modules/statistical-display.md). Keep the code revision, input and parameter versions, seed, settings and policy schedule with the results.

If the run reports a missing population table, check that setup and simulation use the same country and year. If donor files or a policy year are missing, check [Tax-Benefit Donors (UK)](../getting-started/data/tax-benefit-donors-uk.md) before rebuilding.

These commands follow [SimPathsStart](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/experiment/SimPathsStart.java). For an older release, use that release's `--help` output.
