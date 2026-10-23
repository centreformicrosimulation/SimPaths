# The Start Class

A JAS-mine experiment builder defines which managers participate in a simulation. It creates the model and its supporting collector and observer, then registers them with the simulation engine.

In SimPaths this role belongs to `simpaths.experiment.SimPathsStart`. It supports interactive and headless single runs and coordinates the input-setup choices before model construction.

## Engine setup

The current entry point uses an experiment-builder instance:

```java
SimPathsStart experimentBuilder = new SimPathsStart();
engine.setExperimentBuilder(experimentBuilder);
engine.setup();
```

This is an excerpt from [SimPathsStart](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/experiment/SimPathsStart.java). It assumes the engine and input configuration have already been prepared; it is not a complete replacement `main()` method.

## Interactive and headless use

Interactive use creates the JAS-mine shell and lets the user build and start the model. Headless use starts the engine directly and waits for completion. Supporting charts are not required for batch execution, but collector outputs can still be written.

Use [Single Runs](../../user-guide/single-runs.md) for commands and [Start Class Implementation](../internals/start-class-implementation.md) for the full startup sequence. For repeated experiments use the existing [MultiRun class](multirun-class.md), not a new demo entry point.
