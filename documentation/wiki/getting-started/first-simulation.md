# Running Your First Simulation

This page covers the quickest reliable path to a first successful SimPaths run using `singlerun.jar`.

## Before you run

Make sure you have already completed:

- [Environment Setup](environment-setup.md)
- [Input Data](data/index.md)

In practice, that means:

- the project builds successfully with Maven
- `singlerun.jar` exists in the repository root
- the required input files are present under `input/`

## Recommended first run: headless and explicit

For a first run, the clearest route is to separate setup from execution.

### 1. Build the project

```bash
mvn clean package
```

### 2. Run setup only

```bash
java -jar singlerun.jar -Setup -c UK -s 2019 -g false --rewrite-policy-schedule
```

This setup phase does not run the simulation itself. It prepares the model by:

- writing or refreshing the policy schedule
- saving the selected country and start year
- loading uprating and alignment inputs
- rebuilding `input/input.mv.db`

If `input/EUROMODpolicySchedule.xlsx` already exists and matches your donor files, you can omit `--rewrite-policy-schedule`.

### 3. Run the simulation only

```bash
java -jar singlerun.jar -Run -c UK -s 2019 -g false
```

This starts the JAS-mine engine with:

- `SimPathsModel`
- `SimPathsCollector`
- `SimPathsObserver` only when the GUI is enabled

In headless mode, the process runs to completion and then exits.

## What the main flags do

The most useful `singlerun.jar` options are:

- `-c <CC>`: country code such as `UK` or `IT`
- `-s <year>`: simulation start year
- `-Setup`: perform setup only, then exit
- `-Run`: skip setup and run the simulation directly
- `-g true|false`: enable or disable the GUI
- `--rewrite-policy-schedule`: rebuild `EUROMODpolicySchedule.xlsx` from detected donor policy files

`-Setup` and `-Run` are mutually exclusive. If neither is provided, `SimPathsStart` does both.

## If you want to use the GUI

After the initial setup has succeeded, you can launch the single-run interface with:

```bash
java -jar singlerun.jar
```

In GUI mode, SimPaths opens the start-up dialog and then launches the JAS-mine shell. This is useful for interactive exploration, but it is less explicit than the headless route for a first installation check.

## What success looks like

A successful first run should leave you with:

- a rebuilt input database at `input/input.mv.db`
- no setup error about missing donor files or missing policy schedule
- a completed simulation run, either in the GUI or in headless mode

If the run fails before the simulation starts, the problem is usually in setup rather than in the model itself.

## Common first-run problems

- `Policy Schedule file ... doesn't exist`
  - create `input/EUROMODpolicySchedule.xlsx` first, or rerun setup with `--rewrite-policy-schedule`
- donor or initial-population files are missing
  - check the contents of `input/InitialPopulations/` and `input/EUROMODoutput/`
- wrong Java version
  - SimPaths targets Java 19
- setup succeeds but the run uses unexpected inputs
  - rebuild the database after changing donor files or the policy schedule

## Where to go next

Once the first run works, the next useful pages are:

- [Single Runs](../user-guide/single-runs.md)
- [Multiple Runs](../user-guide/multiple-runs.md)
- [Modifying Tax-Benefit Parameters](../user-guide/tax-benefit-parameters.md)
