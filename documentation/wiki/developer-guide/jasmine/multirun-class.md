# The MultiRun Class

JAS-mine's `MultiRun` contract supports a sequence of experiments. SimPaths implements it in `simpaths.experiment.SimPathsMultiRun`.

## Core responsibilities

| Method | Purpose |
| --- | --- |
| `buildExperiment()` | Construct and register the model and collector for a run. |
| `nextModel()` | Decide whether another experiment should run and prepare the next parameter values. |
| `setupRunLabel()` | Identify the run in experiment output. |

In the [checked SimPaths implementation](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/experiment/SimPathsMultiRun.java), the sequence advances a run counter, applies the enabled innovations and stops at `maxNumberOfRuns`. Labels combine the seed and run counter. This is sequential execution, not a parallel worker scheduler.

A generic library example that changes population size between runs is not the same experiment design as the SimPaths defaults. Verify which quantities change in `iterateParameters()` before interpreting a set of results.

## Choose the relevant guide

- [Multiple Runs](../../user-guide/multiple-runs.md): run commands, YAML settings and persistence options.
- [Perform MultiRun Simulations](../how-to/multirun-simulations.md): checklist for designing an experiment.
- [MultiRun Implementation](../internals/multirun-implementation.md): configuration and extension points in the code.
- [Uncertainty Analysis](../../user-guide/uncertainty-analysis.md): interpretation of seed and coefficient variation.

For the underlying library implementation, see the [JAS-mine core repository](https://github.com/jasmineRepo/JAS-mine-core).
