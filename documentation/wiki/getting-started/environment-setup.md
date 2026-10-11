---
reading_navigation:
  previous:
    title: Getting Started
    url: getting-started/
  next:
    title: Input Data
    url: getting-started/data/
---

<div class="setup-guide" markdown="1">

<header class="setup-guide__intro" markdown="1">

# Environment Setup

This page covers the local software requirements, repository setup, and the first-time build and database steps needed before SimPaths is ready to run.

</header>

<section class="setup-guide__section" markdown="1">

## Local requirements { #1-local-requirements }

| Component | Version | Sources |
| --- | --- | --- |
| Java Development Kit (JDK) | 25 | [Adoptium](https://adoptium.net/temurin/releases/?version=25) |
| Apache Maven | 3.8 or later | [Apache Maven](https://maven.apache.org/download.cgi) |
| Git | Not pinned | [Git](https://git-scm.com/install/) |

It is worth checking the toolchain before going further:

```bash
java -version
mvn -version
git --version
```

<aside class="setup-guide__note" aria-label="Java version requirement" markdown="1">

The current SimPaths development release is compiled for Java 25 in `pom.xml`, so earlier Java versions will not build the code correctly.

</aside>
</section>

<section class="setup-guide__section" markdown="1">

## Clone the repository { #2-clone-the-repository }

Clone SimPaths and move into the repository root:

```bash
git clone https://github.com/simpaths/SimPaths.git
cd SimPaths
```

</section>

<section class="setup-guide__section" markdown="1">

## Build the executables { #3-build-the-executables }

<div class="setup-guide__step" markdown="1">
<div markdown="1">

Build them from the repository root:

```bash
mvn clean package
```

After a successful package step, both jars are written to the repository root.

SimPaths uses the Maven Shade plugin to build two runnable jars:

- `singlerun.jar` for single simulations via `simpaths.experiment.SimPathsStart`
- `multirun.jar` for repeated runs via `simpaths.experiment.SimPathsMultiRun`

</div>
<aside class="setup-guide__note" aria-label="Build troubleshooting" markdown="1">

If this step fails, fix the Java or Maven setup first. There is no point debugging the model before the jars build cleanly.

</aside>
</div>
</section>

<section class="setup-guide__section" markdown="1">

## Input layout { #4-check-the-required-input-layout }

Before a real run, SimPaths expects the `input/` directory to contain:

- `InitialPopulations/` for starting population CSV files
- `EUROMODoutput/` for donor tax-benefit input files
- Excel workbooks such as `EUROMODpolicySchedule.xlsx`, `DatabaseCountryYear.xlsx`, and the `align_*.xlsx`, `reg_*.xlsx`, `scenario_*.xlsx`, and `projections_*.xlsx` parameter files

The key point here is that SimPaths does **not** run directly from the raw CSV and EUROMOD files. During setup, the code rebuilds the local H2 database `input/input.mv.db` from those sources by calling `Parameters.databaseSetup()`.

<aside class="setup-guide__note" aria-labelledby="6-training-data-fallback" markdown="1">

### Training data fallback { #6-training-data-fallback }

If no top-level CSV files are found in `input/InitialPopulations/`, SimPaths automatically switches to the bundled training data under `input/InitialPopulations/training/` and the associated training donor files. This is useful for documentation, testing, and development, but not for substantive research analysis.

The fallback is helpful for getting the code running locally, but it is not a substitute for the full research inputs.

</aside>
</section>

<section class="setup-guide__section" markdown="1">

## First-time setup { #5-understand-first-time-setup }

The first-time setup step is what turns the repository from "compiled" into "runnable".

<div class="setup-guide__step" markdown="1">
<div markdown="1">

For a clean first headless setup, the most explicit route is:

```bash
java -jar singlerun.jar -Setup -c UK -s 2019 -g false --rewrite-policy-schedule
```

Once that succeeds, a run-only check is straightforward:

```bash
java -jar singlerun.jar -Run -c UK -s 2019 -g false
```

If you prefer the repeated-run path, the equivalent setup command is:

```bash
java -jar multirun.jar -DBSetup -config config/default.yml
```

</div>
<aside class="setup-guide__note" aria-label="Setup details" markdown="1">

Setup does four important things:

1. writes or refreshes `DatabaseCountryYear.xlsx`
2. validates or rebuilds `EUROMODpolicySchedule.xlsx`
3. rebuilds the starting-population database under `input/input.mv.db`
4. rebuilds the donor tax-benefit database from `input/EUROMODoutput/`

</aside>
</div>

<aside class="setup-guide__note" aria-labelledby="7-policy-schedule-file" markdown="1">

### Policy schedule file { #7-policy-schedule-file }

Single-run setup expects `input/EUROMODpolicySchedule.xlsx` to exist. If you need to rebuild that file from the EUROMOD output filenames, use the `--rewrite-policy-schedule` flag when running `singlerun.jar`.

</aside>
</section>

<section class="setup-guide__section" markdown="1">

## Common setup problems { #8-common-setup-problems }

The usual setup failures are:

| Problem | What happens |
| --- | --- |
| wrong Java version | the build or runtime fails before the model even starts |
| missing initial population files | setup falls back to training data, or database creation fails |
| missing donor files | the tax-benefit database cannot be rebuilt |
| stale policy schedule | the EUROMOD output files and `EUROMODpolicySchedule.xlsx` do not line up |
| database needs rebuilding | donor files or schedule changed, but the old `input/input.mv.db` is still being reused |

If donor inputs, donor years, or the policy schedule change, rebuild the database before trying to interpret any results.

</section>

<section class="setup-guide__next" markdown="1">

## Next step

Once the project is built and the input data are in place, continue to [Running Your First Simulation](first-simulation.md).

</section>
</div>
