# Add Parameters to the GUI

SimPaths uses JAS-mine's `@GUIparameter` annotation to expose fields on its simulation managers. This controls the interactive parameter panel, not the model logic itself.

## Start from an existing field

For example, `SimPathsModel` declares:

```java
@GUIparameter(description = "Simulated population size (base year)")
private Integer popSize = 50000;
```

The class imports:

```java
import microsim.annotation.GUIparameter;
```

See [SimPathsModel](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/src/main/java/simpaths/model/SimPathsModel.java) for the complete context. To expose another setting:

1. Define the field on the manager that owns the setting, with an appropriate type and default.
2. Add the annotation and a description explaining its units, valid values and effect.
3. Ensure the model actually reads the field at the intended point in its lifecycle.
4. Rebuild, open the GUI and check both the displayed control and its effect on a small training run.

## Initialisation versus live changes

A parameter read only in `buildObjects()` will not rebuild the population when edited during a run. Change such settings before building the model. Live changes affect only code that reads the updated value subsequently; do not assume that clicking the update button recalculates cached state.

Check dependent values and validate inputs explicitly. The annotation does not add scientific validity checks or make an otherwise incompatible setting safe.

## Batch configuration

The annotation does not automatically create a command-line option. Multi-run YAML settings are handled separately by `SimPathsMultiRun`, including `model_args` and `collector_args`. If the setting should work in batch mode, verify the relevant configuration path as well as the GUI.

See [Multiple Runs](../../user-guide/multiple-runs.md) for configuration and [The Graphical User Interface](../../user-guide/gui.md) for interactive controls.
