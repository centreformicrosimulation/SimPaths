# Modifying SimPaths Parameters

Different kinds of parameter changes use different interfaces:

- For run years, population size, random seeds and feature flags, use [Multiple Runs](multiple-runs.md), which also supports a single YAML-configured repetition.
- For interactive controls, see [Graphical User Interface](gui.md).
- For policy schedules and tax-benefit donor inputs, see [Modifying Tax-Benefit Settings](tax-benefit-parameters.md).
- For estimated coefficients and model input files, see [Model Parameterisation](../overview/parameterisation.md).
- For sensitivity experiments, distinguish the sources of variation described in [Uncertainty Analysis](uncertainty-analysis.md).

Record the source revision, configuration and input versions for each experiment. Changing a workbook is not equivalent to changing a run-time control, and some source-level settings are not exposed through either YAML or the GUI.
