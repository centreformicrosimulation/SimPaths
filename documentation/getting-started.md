# Getting Started

## Prerequisites

- Java 19
- Maven 3.8+
- Optional IDE: IntelliJ IDEA (import as a Maven project)

## Build

From repository root:

```bash
mvn clean package
```

Artifacts produced at the root:

- `singlerun.jar`
- `multirun.jar`

## Understand run modes

SimPaths supports two entry points:

- `singlerun.jar` (`SimPathsStart`): setup and single simulation execution
- `multirun.jar` (`SimPathsMultiRun`): repeated runs across seeds/scenarios

## First run (headless)

### Step 1: setup input artifacts

```bash
java -jar singlerun.jar -c UK -s 2019 -g false -Setup --rewrite-policy-schedule
```

This prepares required setup files such as:

- `input/input.mv.db`
- `input/EUROMODpolicySchedule.xlsx`
- `input/DatabaseCountryYear.xlsx`

### Step 2: execute a multi-run configuration

```bash
java -jar multirun.jar -config default.yml -g false
```

Results are written under `output/<run-folder>/`.

## Training vs full data mode

- The repository includes training data under:
  - `input/InitialPopulations/training/`
  - `input/EUROMODoutput/training/`
- If no initial-population CSV files are found in the main input location, SimPaths automatically switches to training mode.
- Training mode supports development and CI, but is not intended for research interpretation.

## GUI usage

Use `-g true` (default behavior in several flows) to run with GUI components.

In headless/remote environments, set `-g false`.

For GUI usage, see the GUI section of the user guide on the project website.
