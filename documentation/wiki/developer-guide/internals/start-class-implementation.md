# Start Class Implementation

`simpaths.experiment.SimPathsStart` is the single-run entry point. For commands and first-use setup, see [Single Runs](../../user-guide/single-runs.md); this page describes the implementation.

## Startup sequence

1. `main()` parses the command-line arguments.
2. In GUI mode, `runGUIdialog()` presents the setup choices. In headless mode, `runGUIlessSetup()` performs setup when requested.
3. A setup-only invocation exits. Otherwise, startup reads the saved database country/year metadata before creating the simulation engine.
4. The experiment builder registers the model, collector and, where enabled, observer.
5. The engine builds the experiment. The GUI controls interactive execution; headless execution starts the simulation and waits for completion.

In the [checked revision](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/experiment/SimPathsStart.java), run startup explicitly selects the UK and reads its saved start year from `DatabaseCountryYear.xlsx`. Supplying `-s` to a run-only invocation does not rebuild or replace that saved metadata. Use setup to prepare the intended start year.

## Responsibilities

| Component | Responsibility |
| --- | --- |
| `parseCommandLineArgs()` | Country/year arguments, mutually exclusive setup-only and run-only modes, GUI and policy-schedule options |
| `runGUIdialog()` / `runGUIlessSetup()` | Coordinate preparation of the inputs |
| `buildExperiment()` | Create and register SimPaths managers |
| `Parameters.databaseSetup()` and input parsers | Build or load the required population and donor inputs |
| `SimPathsModel.buildObjects()` | Initialise the simulation population and model state |
| `SimPathsModel.buildSchedule()` | Define the simulation event order |

Database preparation is not all implemented inside the start class. Follow the called parser and parameter methods before changing input handling.

## Extending startup

Keep setup and simulation responsibilities separate. A new option should have a defined default, validation and help text, and should behave consistently in GUI and headless use where both are supported.

Test setup-only and run-only paths, missing inputs, saved country/year metadata and the normal training-data workflow. Preserve custom policy schedules unless rewriting them was explicitly requested. Do not reset entity ID counters during setup or between runs.

For repeated execution and YAML configuration, see [MultiRun Implementation](multirun-implementation.md).
