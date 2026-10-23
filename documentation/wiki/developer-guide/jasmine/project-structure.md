# Project Structure

JAS-mine separates model behaviour, output collection and graphical observation into managers. SimPaths applies that pattern within its Maven project.

| Role | SimPaths implementation |
| --- | --- |
| Model | `SimPathsModel` creates the simulation state and schedules processes on `Person`, `BenefitUnit` and `Household`. |
| Collector | `SimPathsCollector` computes and exports statistics and selected entity records. |
| Observer | `SimPathsObserver` supplies charts and monitoring in interactive runs. |
| Experiment builder | `SimPathsStart` or `SimPathsMultiRun` constructs and registers the managers. |

## Where to work

Source is under `src/main/java/simpaths/`. The `model/` package contains agents and model processes, `data/` contains loaders and supporting calculations, and `experiment/` contains execution, collection and observation.

`input/` holds parameters and simulation inputs; `config/` holds run configurations; `output/` holds run results. Maven generates compiled files under `target/`. The root `pom.xml` specifies dependencies and builds the single-run and multi-run executables.

The [Repository Guide](../repository-guide.md) is the canonical directory and data-pipeline reference. An Eclipse-generated JAS-mine demo structure is not required to work on SimPaths.

## Follow the lifecycle

Manager objects are registered with the engine before execution. Their `buildObjects()` methods prepare state; their `buildSchedule()` methods register events. Collector and observer schedules are separate from the model schedule, so their timing determines which state is recorded or shown.

See [The Model and the Schedule](model-and-schedule.md) for event ordering, [Start Class Implementation](../internals/start-class-implementation.md) for startup and [Statistical display](../../overview/modules/statistical-display.md) for output controls.

For library development outside SimPaths, consult the [JAS-mine core repository](https://github.com/jasmineRepo/JAS-mine-core).
