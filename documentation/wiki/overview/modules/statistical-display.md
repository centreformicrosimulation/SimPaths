---
title: Statistical display
---

<div class="module-detail" markdown>

# Statistical display

<p class="module-detail__lead">At the end of each simulated year, SimPaths produces year-specific summary statistics for post-simulation analysis. A selected subset is also displayed graphically while a simulation is running.</p>

## Output handling

[`SimPathsCollector`](https://github.com/simpaths/SimPaths/blob/develop/src/main/java/simpaths/experiment/SimPathsCollector.java) collects and persists simulation outputs. When the graphical interface is enabled, [`SimPathsObserver`](https://github.com/simpaths/SimPaths/blob/develop/src/main/java/simpaths/experiment/SimPathsObserver.java) updates the live charts and monitoring views.

The collector has separate switches for person, benefit-unit and household records, and for summary statistics covering income and wealth, demographics, alignment, labour, health and wellbeing. A displayed chart is not a substitute for saving the data needed for analysis.

## Choosing outputs

For a YAML-configured run, use the `collector_args` section. For example, this fragment enables CSV export while omitting the three large entity-level tables:

```yaml
collector_args:
  exportToCSV: true
  exportToDatabase: false
  persistPersons: false
  persistBenefitUnits: false
  persistHouseholds: false
```

Merge these settings into the existing `collector_args` block rather than creating a second block. Summary-statistic switches remain at their configured values. The documented options are in [config/default.yml](https://github.com/simpaths/SimPaths/blob/b223738b9cdf1d814cc3c6f09b04bc4930d3c667/config/default.yml); use [Multiple Runs](../../user-guide/multiple-runs.md) to run a configuration, including a single repetition.

Before a large experiment, run a small population and check that:

1. The expected files appear in the run's `output/` directory.
2. The saved years and output frequency match the experiment.
3. The required variables and population groups are present. Some statistics, such as Gini coefficients, have a separate calculation switch.
4. Enough detail has been retained for validation and subgroup analysis.

Entity-level exports can be large and may contain sensitive microdata. Retain them only in an appropriate research environment. Population persistence for restarting a run is a separate facility from collector export.

See [Running your first simulation](../../getting-started/first-simulation.md) for the main run components and [Saving outputs](../../jasmine-reference/saving-outputs.md) for the underlying JAS-mine output facilities.

</div>
