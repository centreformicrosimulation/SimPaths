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

```bash
mvn clean package
java -jar multirun.jar -DBSetup
java -jar multirun.jar
```

The first command builds the JARs. The second creates the H2 donor database from the input data. The third runs the simulation using `default.yml`.

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
