# MultiRun Implementation

`simpaths.experiment.SimPathsMultiRun` extends JAS-mine's `MultiRun` to execute SimPaths experiments sequentially. [Multiple Runs](../../user-guide/multiple-runs.md) is the canonical user guide; this page explains where to modify the implementation.

## Configuration and startup

The entry point reads the YAML configuration and then command-line arguments. Named files supplied through `-config` are resolved under `config/`.

Top-level settings configure the run wrapper. `model_args`, `collector_args`, `parameter_args` and `innovation_args` are handled separately. The implementation maps many keys to Java fields by reflection, so spelling and types matter; unknown or inaccessible fields can produce errors rather than valid settings.

Avoid defining the same setting at both the wrapper level and in `model_args`: model arguments are applied after the wrapper copies its settings into the model. Do not assume that command-line precedence resolves every duplicated field across configuration sections.

## Building each experiment

`buildExperiment()` creates a `SimPathsModel`, applies run settings and model arguments, and registers a `SimPathsCollector` with its collector arguments. It does not register the interactive `SimPathsObserver`.

The multi-run progress window is therefore not the single-run chart interface.

## Advancing the sequence

After each run, `nextModel()` increments the counter and checks it against `maxNumberOfRuns`. If another run is needed, `iterateParameters()` applies the enabled innovations.

The existing innovations include incrementing the random seed and predefined changes used for elasticity experiments. They are not a general-purpose parameter-grid parser. `setupRunLabel()` combines the seed and run counter to identify the run.

When adding an experiment design, define what changes at each iteration, its stopping condition and how the configuration is recorded. Check that state from one run cannot inadvertently carry into the next.

## Persistence and uncertainty

The population-persistence switches and collector export switches are separate. The former concern reuse/storage of the processed population; the latter control reported outputs. Follow both code paths when changing storage behaviour.

Parameter loading occurs as the model is built. In the checked revision, coefficient resampling is enabled in `Parameters` and is not a supported runtime toggle. Seed changes can therefore affect more than simulation draws. See [Uncertainty Analysis](../../user-guide/uncertainty-analysis.md).

## Source and tests

This description follows [SimPathsMultiRun](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/experiment/SimPathsMultiRun.java). Verify changes with unit and integration tests (`mvn verify`), then a short sequence using training data, including the relevant persistence modes. Check that the expected number of distinct run outputs is produced.
