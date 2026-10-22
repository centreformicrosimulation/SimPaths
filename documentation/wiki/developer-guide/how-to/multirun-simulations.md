# How to Perform MultiRun Simulations

Use the existing SimPaths multi-run entry point rather than creating a separate JAS-mine demo wrapper. It runs experiments sequentially.

## Define the experiment

Decide whether you need repeated draws, different policy scenarios or different model assumptions. These are different designs; [Uncertainty Analysis](../../user-guide/uncertainty-analysis.md) explains how to interpret them.

## Configure and run

1. Follow [Multiple Runs](../../user-guide/multiple-runs.md) for setup and the complete configuration reference.
2. Copy `config/default.yml` to a named experiment file under `config/`.
3. Set the run count, initial seed, start/end years, population size and output settings.
4. Enable only the intended innovations. `randomSeedInnov` increments the seed between runs; it does not isolate simulation randomness when coefficient resampling is also enabled.
5. Run the configuration from the repository root. For a file named `experiment.yml`:

    ```bash
    java -jar multirun.jar -config experiment.yml
    ```

6. Check the logs, run count, labels and output files before launching a larger experiment.

Keep an unchanged copy of the configuration, policy schedule and input versions with the results. For a set of policy scenarios, use separately named configurations and an explicit record of their differences.

## Extend the run design

If the existing controls cannot express the design, inspect `buildExperiment()`, `nextModel()`, `iterateParameters()` and `setupRunLabel()` in [MultiRun Implementation](../internals/multirun-implementation.md). Changing a YAML key alone does not add a new iteration rule.

The [JAS-mine MultiRun architecture](../jasmine/multirun-class.md) explains the library contract; it is not another set of SimPaths run commands.
