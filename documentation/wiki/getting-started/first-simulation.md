---
reading_navigation:
  previous:
    title: Input Data
    url: getting-started/data/
  next:
    title: User Guide
    url: user-guide/
---

<div class="setup-guide" markdown="1">

<header class="setup-guide__intro" markdown="1">

# Running Your First Simulation

This page covers the quickest reliable path to a first successful SimPaths run using `singlerun.jar`.

</header>

<section class="setup-guide__section" markdown="1">

## Before you run

Make sure you have already completed:

- [Environment Setup](environment-setup.md)
- [Input Data](data/index.md)

In practice, that means:

- the project builds successfully with Maven
- `singlerun.jar` exists in the repository root
- the required input files are present under `input/`

</section>

<section class="setup-guide__section" markdown="1">

## Recommended first run: headless and explicit

For a first run, the clearest route is to separate setup from execution.

### 1. Build the project

```bash
mvn clean package
```

### 2. Run setup only

<div class="setup-guide__step" markdown="1">
<div markdown="1">

```bash
java -jar singlerun.jar -Setup -c UK -s 2019 -g false --rewrite-policy-schedule
```

If `input/EUROMODpolicySchedule.xlsx` already exists and matches your donor files, you can omit `--rewrite-policy-schedule`.

</div>
<aside class="setup-guide__note" aria-label="What setup prepares" markdown="1">

This setup phase does not run the simulation itself. It prepares the model by:

- writing or refreshing the policy schedule
- saving the selected country and start year
- loading uprating and alignment inputs
- rebuilding `input/input.mv.db`

</aside>
</div>

### 3. Run the simulation only

<div class="setup-guide__step" markdown="1">
<div markdown="1">

```bash
java -jar singlerun.jar -Run -c UK -s 2019 -g false
```

In headless mode, the process runs to completion and then exits.

</div>
<aside class="setup-guide__note" aria-label="Simulation components" markdown="1">

This starts the JAS-mine engine with:

- `SimPathsModel`
- `SimPathsCollector`
- `SimPathsObserver` only when the GUI is enabled

</aside>
</div>
</section>

<section class="setup-guide__section" markdown="1">

## What the main flags do

The most useful `singlerun.jar` options are:

| Option | Purpose |
| --- | --- |
| `-c <CC>` | country code such as `UK` or `IT` |
| `-s <year>` | simulation start year |
| `-Setup` | perform setup only, then exit |
| `-Run` | skip setup and run the simulation directly |
| `-g true\|false` | enable or disable the GUI |
| `--rewrite-policy-schedule` | rebuild `EUROMODpolicySchedule.xlsx` from detected donor policy files |

`-Setup` and `-Run` are mutually exclusive. If neither is provided, `SimPathsStart` does both.

</section>

<section class="setup-guide__section" markdown="1">

## If you want to use the GUI

After the initial setup has succeeded, you can launch the single-run interface with:

```bash
java -jar singlerun.jar
```

In GUI mode, SimPaths opens the start-up dialog and then launches the JAS-mine shell. This is useful for interactive exploration, but it is less explicit than the headless route for a first installation check.

For a fresh clone using the bundled training data, follow [First GUI run with bundled training data](../user-guide/gui.md#2-first-gui-run-with-bundled-training-data). It shows the exact start-up options, country, and 2019 start year required to build the local database before starting the simulation.

</section>

<section class="setup-guide__section" markdown="1">

## What success looks like

A successful first run should leave you with:

- a rebuilt input database at `input/input.mv.db`
- no setup error about missing donor files or missing policy schedule
- a completed simulation run, either in the GUI or in headless mode

If the run fails before the simulation starts, the problem is usually in setup rather than in the model itself.

</section>

<section class="setup-guide__section" markdown="1">

## Common first-run problems

| Problem | What to check |
| --- | --- |
| `Policy Schedule file ... doesn't exist` | create `input/EUROMODpolicySchedule.xlsx` first, or rerun setup with `--rewrite-policy-schedule` |
| donor or initial-population files are missing | check the contents of `input/InitialPopulations/` and `input/EUROMODoutput/` |
| wrong Java version | SimPaths targets Java 25 |
| setup succeeds but the run uses unexpected inputs | rebuild the database after changing donor files or the policy schedule |

</section>

<section class="setup-guide__next" markdown="1">

## Where to go next

Once the first run works, the next useful pages are:

- [Single Runs](../user-guide/single-runs.md)
- [Multiple Runs](../user-guide/multiple-runs.md)
- [Modifying Tax-Benefit Settings](../user-guide/tax-benefit-parameters.md)

</section>
</div>
